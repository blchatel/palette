package ch.epfl.cs413.palettev01.processing;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by joachim on 3/25/17.
 */

public class Kmeans {
    /**
     * Saving the location of a pixel
     */
    private class IndexPair {
        IndexPair(int x, int y) {
            this.x = x;
            this.y = y;
        }
        IndexPair (int[] coord) {
            if (coord.length != 2) {
                throw new IllegalArgumentException("Should be only 2 values in given coordinates");
            }
            x = coord[0];
            y = coord[1];
        }
        int x;
        int y;
    }

    /**
     * Saving the location in the bins
     */
    private class BinsTriplet {
        BinsTriplet (int l, int m, int n) {
            this.l = l;
            this.m = m;
            this.n = n;
        }
        BinsTriplet (int[] coord) {
            if (coord.length != 3) {
                throw new IllegalArgumentException("Should be only 3 values in given coordinates");
            }
            l = coord[0];
            m = coord[1];
            n = coord[2];
        }
        int l;
        int m;
        int n;
    }

    /**
     * The structure to store the bins values
     */
    private class Bins {
        Map<BinsTriplet, Pair<LabColor, Integer>> bins;

        Bins(Map<BinsTriplet, List<RGBColor>> binPixelMap) {
            bins = new HashMap<>();
            createBins(binPixelMap);
        }

        private void createBins(Map<BinsTriplet, List<RGBColor>> binPixelMap) {
            for (Map.Entry<BinsTriplet, List<RGBColor>> entry: binPixelMap.entrySet()) {
                // Compute mean for each triplet
                int count = entry.getValue().size();
                LabColor c = new LabColor(0,0,0);
                for (int i=0; i < count; i++) {
                    RGBColor rgb = entry.getValue().get(i);
                    // Convert to Lab
                    double[] Lab = new double[3];
                    ColorUtils.RGBToLAB(rgb.r, rgb.g, rgb.b, Lab);
                    c = c.addColor(new LabColor(Lab));
                }
                c = c.divide(count);

//                // Convert to Lab
//                double[] Lab = new double[3];
//                ColorUtils.RGBToLAB(c.r, c.g, c.b, Lab);
                // Add the bin entry
                bins.put(entry.getKey(), Pair.create(c, count));
            }
        }

        LabColor getBinValue(BinsTriplet coord) {
            return bins.get(coord).first;
        }

        Collection<Pair<LabColor, Integer>> getAll () {
            return bins.values();
        }

        /**
         * TODO : Is this one really efficient ?
         *
         * @return the values separated to permit parallelization
         */
        public List<Collection<Pair<LabColor, Integer>>> getAllParallel() {
            List<Pair<LabColor, Integer>> binsValues = new ArrayList<>(bins.values());
            List<Collection<Pair<LabColor, Integer>>> out = new ArrayList<>();
            int maxInPool = 500;
            // TODO ? size or values.size ?
            int j = 0;
            while (j < bins.size()) {
                Collection<Pair<LabColor, Integer>> partOut = new HashSet<>();
                for (int i = 0; i < maxInPool && j < bins.size(); i++, j++) {
                    partOut.add(binsValues.get(j));
                }
                out.add(partOut);
            }
            return out;
        }
    }


    /**
     * Max iterations number
     */
    private final int maxIt = 5;

    /**
     * Simplification of colors to bins
     */
    private Bins bins;

    /**
     * Mapping a bin cell with all pixels in it
     */
    private Map<BinsTriplet, List<RGBColor>> mBinPixelsMap;

    /**
     * The list of the clusters -- Automatic Palette
     */
    private List<LabColor> mPaletteClusters;

    /**
     * Number of clusters, Palette size
     */
    private final int K;

    private int moves;

    private static final int b = 16;
    private static final int invB = (int)(Math.ceil(256.0/b));

    /**
     * The wanted palette size, number of clusters
     * @param k
     */
    public Kmeans (int k, Bitmap img) {
        this.K = k;
        mBinPixelsMap = createBinPixelMap(img);
        Log.d("<Kmeans", "Created bin-pixel map of size : " + mBinPixelsMap.size());
        this.bins = new Bins(mBinPixelsMap);
        Log.d("<Kmeans", "Created bins structure containing " + bins.bins.size() + " colors !");


        /// We first add the black cluster in order to avoid a dark palette
        mPaletteClusters = new ArrayList<>();
        mPaletteClusters.add(new LabColor(0,0,0));
        moves = 1;
    }

    public List<LabColor> run () {
        initClusters();
        Log.d("<<Kmeans>>", "Centers initialized");

        int it = 0;
        while (it < maxIt || moves == 0) {
            moves = 0;
            assignToCenters(true);
            it++;
            Log.d("<<Kmeans>>", "Kmeans iteration " + it);
        }

        return mPaletteClusters;
    }

    /**
     * This method will create K random clusters
     */
    private void initClusters () {
        for (int i=0; i < K; i++) {
            mPaletteClusters.add(LabColor.generateRandom());
        }
    }

    /**
     * This method assign each point to a cluster and recompute the mean
     * It's a step of the k-means algorithm.
     * Use parallel implementation if @param isParallel = true
     *
     * @return the assignements
     */
    private List<Collection<Pair<LabColor, Integer>>> assignToCenters (boolean isParallel) {
        long time1 = System.nanoTime();
        if (!isParallel) {
            /// Basic K-means
            List<Collection<Pair<LabColor, Integer>>> assignments = new ArrayList<>();
            for (int i = 0; i < K + 1; i++) {
                assignments.add(new HashSet<Pair<LabColor, Integer>>());
            }

            Collection<Pair<LabColor, Integer>> binsList = this.bins.getAll();
            for (Pair<LabColor, Integer> lab : binsList) {
                double minDist = lab.first.squareDistanceTo(mPaletteClusters.get(0));
                int chosenIndex = 0;
                for (int i = 1; i < K + 1; i++) {
                    double next = lab.first.squareDistanceTo(mPaletteClusters.get(i));
                    if (next < minDist) {
                        minDist = next;
                        chosenIndex = i;
                    }
                }

                // We assign to the closer center
                assignments.get(chosenIndex).add(lab);
            }

            long time2 = System.nanoTime();
            long timeTaken = time2 - time1;
            Log.d("<<TIMER>>", "Time taken " + timeTaken + " ns");

            // We are done assigning, now we can update the new centers
            for (int i = 1; i < K + 1; i++) {
                // We first set it to zero such that it won't affect the division
                LabColor meanColor = new LabColor(0, 0, 0);
                for (Pair<LabColor, Integer> c : assignments.get(i)) {
                    meanColor = meanColor.addColor(c);
                }
                meanColor = meanColor.divide(assignments.get(i).size());

                LabColor lastC = mPaletteClusters.get(i);
                if (lastC != meanColor) {
                    moves++;
                }
                mPaletteClusters.set(i, meanColor);
            }

            long time3 = System.nanoTime();
            timeTaken = time3 - time1;
            Log.d("<<TIMER>>", "Time taken " + timeTaken + " ns");
            return assignments;
        } else {
            /// Parallel k-means
            List<Collection<Pair<LabColor, Integer>>> binsList = this.bins.getAllParallel();
            List<Collection<Pair<LabColor, Integer>>> assignments = null;
            try {
                assignments = parralelizedAssignement(binsList);

                long time2 = System.nanoTime();
                long timeTaken = time2 - time1;
                Log.d("<<TIMER>>", "Time taken " + timeTaken + " ns");

                moves = paralellizeMean(assignments);

                long time3 = System.nanoTime();
                timeTaken = time3 - time1;
                Log.d("<<TIMER>>", "Time taken " + timeTaken + " ns");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            return assignments;
        }
    }

    /**
     *
     * @param img
     * @return
     */
    public Map<BinsTriplet, List<RGBColor>> createBinPixelMap(Bitmap img) {
        Map<BinsTriplet, List<RGBColor>> map = new HashMap<>();
        /// Traverse in this order for memory access acceleration
        for(int y = 0; y < img.getHeight(); y++){
            for(int x = 0; x < img.getWidth(); x++) {
                int p = img.getPixel(x, y);
                RGBColor rgb = RGBColor.intToRGB(p);

                BinsTriplet triplet = new BinsTriplet(rgb.r/invB, rgb.g/invB, rgb.b/invB);
                List<RGBColor> list;
                if (map.containsKey(triplet)) {
                    list = map.get(triplet);
                } else {
                    list = new ArrayList<>();
                }
                list.add(rgb);
                map.put(triplet, list);
            }
        }

        return map;
    }



    /* * * * * * * * * * * * * * * * * * * * * * * *
     *  METHODS FOR PARALLEL COMPUTATION OF KMEANS *
     * * * * * * * * * * * * * * * * * * * * * * * */
    // TODO: Not working yet parallel change the methodology because Future are not working

    /**
     * Parallel implementation for mean computation
     *
     * @param inputs
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public int paralellizeMean(List<Collection<Pair<LabColor, Integer>>> inputs) throws InterruptedException, ExecutionException {

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(threads);

        List<Pair<Future<LabColor>, Integer>> futures = new ArrayList<>();
        int i = 0;
        for (final Collection<Pair<LabColor, Integer>> input : inputs) {
            Callable<LabColor> callable = new Callable<LabColor>() {
                public LabColor call() throws Exception {
                    LabColor meanColor = new LabColor(0,0,0);

                    for (Pair<LabColor, Integer> c: input) {
                        meanColor = meanColor.addColor(c);
                    }
                    meanColor = meanColor.divide(input.size());

                    return meanColor;
                }
            };
            futures.add(Pair.create(service.submit(callable), i));
            i++;
        }

        service.shutdown();

        int nM = 0;
        List<LabColor> outputs = new ArrayList<>();
        for (Pair<Future<LabColor>, Integer> future : futures) {
            int index = future.second;
            LabColor mean = future.first.get();
            outputs.add(index, mean);
            if (mPaletteClusters.get(index) != mean) {
                nM++;
            }
        }

        return nM;
    }

    /**
     * Parallel implementation for kmeans clusters assignement
     *
     * @param inputs
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private List<Collection<Pair<LabColor, Integer>>> parralelizedAssignement(List<Collection<Pair<LabColor, Integer>>> inputs) throws InterruptedException, ExecutionException {
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(threads);

        List<Future<List<Collection<Pair<LabColor, Integer>>>>> futures = new ArrayList<>();
        for (final Collection<Pair<LabColor, Integer>> input: inputs) {
            Callable<List<Collection<Pair<LabColor, Integer>>>> callable = new Callable<List<Collection<Pair<LabColor, Integer>>>>() {
                public List<Collection<Pair<LabColor, Integer>>> call() throws Exception {

                    List<Collection<Pair<LabColor, Integer>>> partialAssign = new ArrayList<>();
                    for (int i = 0; i < K+1; i++) {
                        partialAssign.add(new HashSet<Pair<LabColor, Integer>>());
                    }

                    for (Pair<LabColor, Integer> lab: input) {
                        double minDist = lab.first.squareDistanceTo(mPaletteClusters.get(0));
                        int chosenIndex = 0;
                        for (int i = 1; i < K+1; i++) {
                            double next = lab.first.squareDistanceTo(mPaletteClusters.get(i));
                            if (next < minDist) {
                                minDist = next;
                                chosenIndex = i;
                            }
                        }

                        // We assign to the closer center
                        partialAssign.get(chosenIndex).add(lab);
                    }

                    return partialAssign;
                }
            };
            futures.add(service.submit(callable));
        }
        service.shutdown();

        // Merge all the futures into one output
        List<Collection<Pair<LabColor, Integer>>> output = futures.get(0).get();
        for (int i = 1; i < futures.size(); i++) {
            int j = 0;
            for (Collection<Pair<LabColor, Integer>> c : futures.get(i).get()) {
                output.get(j).addAll(c);
                j++;
            }
        }

        return output;
    }
}
