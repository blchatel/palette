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
        long t1, t2;

        t1 = System.nanoTime();
        rsTest(k, img, rsProcessing);
        t2 = System.nanoTime();
        Log.d("TIME_ACCESS", "RS bins takes " + ( (t2 - t1) / 1000000 ) + " ms");
    }

    private void rsTest(int k, Bitmap img, RSProcessing rsProcessing) {
        mPaletteClusters = new ArrayList<>();
        mPaletteClusters.add(new LabColor(0,0,0));
        Map<Kmeans.BinsTriplet, Pair<LabColor, Integer>> res = rsProcessing.generateBins(k, img);

        bins = new Bins();
        bins.bins = res;
        List<Pair<Integer, LabColor>> indexes = new ArrayList<>();
        List<LabColor> stored = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            indexes.add(Pair.create(-1, new LabColor(0, 0, 0)));
        }

        for (Map.Entry<BinsTriplet, Pair<LabColor, Integer>> entry : res.entrySet()) {
            // Compute mean for each triplet
            int count = entry.getValue().second;

            LabColor c = entry.getValue().first;

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

        }

        for (int i = 0; i < indexes.size(); i++) {
            if (indexes.get(i).first == -1) {
                mPaletteClusters.add(stored.remove(stored.size() - 1));
            } else {
                mPaletteClusters.add(indexes.get(i).second);
            }
        }
    }

    public List<LabColor> run (RSProcessing rsProcessing) {
        long t1, t2;

        for (int i=0; i<K+1; i++) {
            LabColor lab = mPaletteClusters.get(i);
            Log.d("old " + Integer.toString(i), lab.toString());
        }
        t1 = System.nanoTime();
        rsProcessing.KMean_cluster(mPaletteClusters, bins.bins, K);
        t2 = System.nanoTime();
        Log.d("TIME_ACCESS", "RS Kmeans Process takes " + ( (t2 - t1) / 1000000 ) + " ms");
        for (int i=0; i<K+1; i++) {
            LabColor lab = mPaletteClusters.get(i);
            Log.d("new " + Integer.toString(i), lab.toString());
        }

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

        Bins () {}

    }
}
