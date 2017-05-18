package ch.epfl.cs413.palettev01.processing;

import android.graphics.Bitmap;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import ch.epfl.cs413.palettev01.views.PaletteAdapter;

/**
 * Created by joachim on 3/25/17.
 */

public class Kmeans {
    private static final int b = 16;
    private static final int invB = (int)(Math.ceil(256.0/b));
    /**
     * Max iterations number
     */
    private final int maxIt = 50;
    /**
     * Number of clusters, Palette size
     */
    private int K;
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
    private int moves;

    /**
     * The wanted palette size, number of clusters
     * @param k
     */
    public Kmeans (int k, Bitmap img) {
        this.K = k;
        /// We first add the black cluster in order to avoid a dark palette
        mPaletteClusters = new ArrayList<>();
        mPaletteClusters.add(new LabColor(0,0,0));

        long t1 = System.nanoTime();
        mBinPixelsMap = createBinPixelMap(img);
        long t2 = System.nanoTime();
        Log.d("TIME_ACCESS", "Pixel Map creation takes " + ( (t2 - t1) / 1000000 ) + " ms");



        t1 = System.nanoTime();
        // This will also init the clusters
        this.bins = new Bins(mBinPixelsMap);
        t2 = System.nanoTime();
        Log.d("TIME_ACCESS", "Bins creation takes " + ( (t2 - t1) / 1000000 ) + " ms");

        moves = 1;
    }

    public List<LabColor> run () {
        long t1 = System.nanoTime();
        int it = 0;

        while (it < maxIt || moves == 0) {
            moves = 0;
            assignToCenters();
            it++;
        }

        long t2 = System.nanoTime();
        Log.d("TIME_ACCESS", "Kmeans Process takes " + ( (t2 - t1) / 1000000 ) + " ms");

        return mPaletteClusters;
    }

    /**
     * This method assign each point to a cluster and recompute the mean
     * It's a step of the k-means algorithm.
     * Use parallel implementation if @param isParallel = true
     *
     * @return the assignements
     */
    private List<Collection<Pair<LabColor, Integer>>> assignToCenters () {
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

            // We are done assigning, now we can update the new centers
            for (int i = 1; i < K + 1; i++) {
                // We first set it to zero such that it won't affect the division
                LabColor meanColor = new LabColor(0, 0, 0);
                int divTot = 0;
                for (Pair<LabColor, Integer> c : assignments.get(i)) {
                    meanColor = meanColor.addColor(c);
                    divTot += c.second;
                }
                meanColor = meanColor.divide(divTot);

                LabColor lastC = mPaletteClusters.get(i);
                if (lastC != meanColor) {
                    moves++;
                }
                mPaletteClusters.set(i, meanColor);
            }

            return assignments;
    }

    /**
     *
     * @param img
     * @return
     */
    public Map<BinsTriplet, List<RGBColor>> createBinPixelMap(Bitmap img) {
        Map<BinsTriplet, List<RGBColor>> map = new HashMap<>();

        int[] pixels = new int[img.getHeight()*img.getWidth()];
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        // 2778ms with this method against 4402ms with double for loop
        for (int x = 0; x < pixels.length; x++) {
            int p = pixels[x];
            RGBColor rgb = RGBColor.intToRGB(p);

            BinsTriplet triplet = new BinsTriplet(rgb.r / invB, rgb.g / invB, rgb.b / invB);
            List<RGBColor> list;
            if (map.containsKey(triplet)) {
                list = map.get(triplet);
            } else {
                list = new ArrayList<>();
            }
            list.add(rgb);
            map.put(triplet, list);
        }

        return map;
    }

    /**
     * Saving the location in the bins
     */
    private class BinsTriplet {
        int l;
        int m;
        int n;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            BinsTriplet triplet = (BinsTriplet) o;

            if (l != triplet.l) return false;
            if (m != triplet.m) return false;
            return n == triplet.n;

        }

        @Override
        public int hashCode() {
            int result = l;
            result = 31 * result + m;
            result = 31 * result + n;
            return result;
        }
    }

    /**
     * The structure to store the bins values
     */
    private class Bins {
        /**
         * Contains the bins indexes and then a pair for the corresponding mean lab color and the number of items in the bin
         */
        Map<BinsTriplet, Pair<LabColor, Integer>> bins;
        Map<BinsTriplet, List<LabColor>> binsContent;

        Bins(Map<BinsTriplet, List<RGBColor>> binPixelMap) {
            bins = new HashMap<>();
            binsContent = new HashMap<>();
            createBins(binPixelMap);
        }

        private void createBins(Map<BinsTriplet, List<RGBColor>> binPixelMap) {
            // In the same time we take the initial clusters
            List<Pair<Integer, LabColor>> indexes = new ArrayList<>();
            List<LabColor> stored = new ArrayList<>();
            for (int i = 0; i < K; i++) {
                indexes.add(Pair.create(-1, new LabColor(0, 0, 0)));
            }

            for (Map.Entry<BinsTriplet, List<RGBColor>> entry : binPixelMap.entrySet()) {
                // Compute mean for each triplet
                int count = entry.getValue().size();

                LabColor c = new LabColor(0, 0, 0);
                for (int i = 0; i < count; i++) {
                    RGBColor rgb = entry.getValue().get(i);
                    // Convert to Lab
                    double[] Lab = new double[3];
                    ColorUtils.RGBToLAB(rgb.r, rgb.g, rgb.b, Lab);
                    c = c.addColor(new LabColor(Lab));
                }
                c = c.divide(count);

                double distanceMin = 1000;
                boolean okay = true;
                if (count > indexes.get(0).first) {
                    for (int i = 0; i < indexes.size(); i++) {
                        if (indexes.get(i).first != -1) {
                            double squareDist = c.squareDistanceTo(indexes.get(i).second);
                            if (squareDist < distanceMin) {
                                okay = false;
                                break;
                            }
                        }
                    }
                    if (c.squareDistanceTo(mPaletteClusters.get(0)) > distanceMin && okay) {
                        indexes.set(0, Pair.create(count, c));
                        Collections.sort(indexes, new Comparator<Pair<Integer, LabColor>>() {
                            @Override
                            public int compare(Pair<Integer, LabColor> o1, Pair<Integer, LabColor> o2) {
                                return (o1.first < o2.first) ? -1 : 1;
                            }
                        });
                    } else {
                        stored.add(c);
                    }
                }

                // Add the bin entry
                bins.put(entry.getKey(), Pair.create(c, count));
            }

            for (int i = 0; i < indexes.size(); i++) {
                if (indexes.get(i).first == -1) {
                    mPaletteClusters.add(stored.remove(stored.size() - 1));
                } else {
                    mPaletteClusters.add(indexes.get(i).second);
                }
            }
        }

        Collection<Pair<LabColor, Integer>> getAll() {
            return bins.values();
        }
    }
}
