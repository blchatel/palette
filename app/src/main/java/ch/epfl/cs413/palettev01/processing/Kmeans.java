package ch.epfl.cs413.palettev01.processing;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Explicit name. This class computes a weighted K-means in order to extract the palette
 */
public class Kmeans {
    /**
     * Number of clusters, Palette size
     */
    private int K;

    /**
     * Simplification of colors to bins
     */
    private Bins bins;

    /**
     * The list of the clusters -- Automatic Palette
     */
    private List<LabColor> mPaletteClusters;

    /**
     * The wanted palette size, number of clusters
     * @param k
     */
    public Kmeans (int k, Bitmap img, RSProcessing rsProcessing) {
        this.K = k;

        // Uncomment for time visualization
//        long t1, t2;
//        t1 = System.nanoTime();
        init(k, img, rsProcessing);
//        t2 = System.nanoTime();
//        Log.d("TIME_ACCESS", "RS bins takes " + ( (t2 - t1) / 1000000 ) + " ms");
    }

    /**
     * This method will simply init the different clusters for kmeans processing
     *
     * @param k     the number of clusters
     * @param img   the image on which computation is done
     * @param rsProcessing  the renderscript with which computations are done
     */
    private void init(int k, Bitmap img, RSProcessing rsProcessing) {
        mPaletteClusters = new ArrayList<>();

        // We add a black cluster to have lighter clusters at the end
        mPaletteClusters.add(new LabColor(0,0,0));

        // Create the bins with their mean values
        Map<Kmeans.BinsTriplet, Pair<LabColor, Integer>> binsData = rsProcessing.generateBins(k, img);
        bins = new Bins();
        bins.bins = binsData;

        // Initialize every clusters
        List<Pair<Integer, LabColor>> indexes = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            indexes.add(Pair.create(-1, new LabColor(0, 0, 0)));
        }

        // Extra choices saved
        List<LabColor> stored = new ArrayList<>();

        // Use the bins to choose the good centers and create the clusters
        for (Map.Entry<BinsTriplet, Pair<LabColor, Integer>> entry : binsData.entrySet()) {
            // Compute mean for each triplet
            int count = entry.getValue().second;

            LabColor c = entry.getValue().first;

            // We set a wanted min distance between two palette colors in order to avoid having too close colors
            double distanceMin = 1000;
            boolean okay = true;    // okay=true if the color can be added to the list
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
                    // We store thoses to be sure we have enough clusters center at the end
                    // If it is not the case we will take these
                    stored.add(c);
                }
            }

        }

        // We add the chosen colors to define the palette clusters
        for (int i = 0; i < indexes.size(); i++) {
            if (indexes.get(i).first == -1) {
                mPaletteClusters.add(stored.remove(stored.size() - 1));
            } else {
                mPaletteClusters.add(indexes.get(i).second);
            }
        }
    }

    /**
     * This method will execute the weighted kmeans algorithm
     *
     * @param rsProcessing the renderscript that will do the computations
     * @return
     */
    public List<LabColor> run (RSProcessing rsProcessing) {
        // Uncomment the commented code to have time information
//        long t1, t2;
//        t1 = System.nanoTime();
        rsProcessing.KMean_cluster(mPaletteClusters, bins.bins, K);
//        t2 = System.nanoTime();
//        Log.d("TIME_ACCESS", "RS Kmeans Process takes " + ( (t2 - t1) / 1000000 ) + " ms");

        return mPaletteClusters;
    }

    /**
     * Saving the location in the bins
     */
    static public class BinsTriplet {
        int l;
        int m;
        int n;
        BinsTriplet (int l, int m, int n) {
            this.l = l;
            this.m = m;
            this.n = n;
        }

        /**
         * Redefined equals function for bins triplet
         * @param o
         * @return
         */
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

        /**
         * Empty constructor needed
         */
        Bins () {}

    }
}
