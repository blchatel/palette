package ch.epfl.cs413.palettev01.processing;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.graphics.ColorUtils;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import android.util.Log;
import android.util.Pair;

import ch.epfl.cs413.palettev01.ScriptC_color;
import ch.epfl.cs413.palettev01.views.OurPalette;
import ch.epfl.cs413.palettev01.views.PaletteAdapter;

/**
 * Created by darke on 5/17/2017.
 */

public class RSProcessing {
    public RSProcessing() {

    }

    private RenderScript rs;
    private ScriptC_color colorScript;
    private Bitmap input_pic;
    private Bitmap output_bitmap;
    private float[] grid;
    private float[] temp_grid;
    private int grid_g;
    private float[] old_palette;
    private float[] palette_weights;


    public void rsInit(Context context) {
        //Create renderscript
        rs = RenderScript.create(context);

        //Create script from rs file.
        colorScript = new ScriptC_color(rs);
    }

    public void initGrid(Bitmap input_bitmap) {
        grid_g = 12;
        int g1 = grid_g + 1;
        grid = new float[3 * g1 * g1 * g1];
        temp_grid = new float[3 * g1 * g1 * g1];
        Allocation allocationGrid = Allocation.createSized(rs, Element.F32_3(rs), g1 * g1 * g1, Allocation.USAGE_SCRIPT);
        allocationGrid.setAutoPadding(true);
        allocationGrid.copyFrom(grid);
        colorScript.set_grid(allocationGrid);
        colorScript.set_grid_g(grid_g);
        colorScript.invoke_initGrid();
        allocationGrid.copyTo(grid);
        allocationGrid.destroy();
        System.arraycopy(grid, 0, temp_grid, 0, 3 * g1 * g1 * g1);
        input_pic = Bitmap.createBitmap(input_bitmap);
        output_bitmap = Bitmap.createBitmap(input_pic);
    }

    public void transImage(Bitmap scaled) {
        int g1 = grid_g + 1;
        Allocation allocationGrid = Allocation.createSized(rs, Element.F32_3(rs), g1 * g1 * g1, Allocation.USAGE_SCRIPT);
        allocationGrid.setAutoPadding(true);
        allocationGrid.copyFrom(temp_grid);
        Allocation allocationA = Allocation.createFromBitmap(rs, input_pic);
        Allocation allocationB = Allocation.createTyped(rs, allocationA.getType());

        colorScript.set_grid(allocationGrid);
        colorScript.set_grid_g(grid_g);
        colorScript.forEach_image_transfer(allocationA, allocationB);

        allocationB.copyTo(scaled);
        allocationA.destroy();
        allocationB.destroy();
        allocationGrid.destroy();
    }


    /**
     * Create old_palette with the given palette
     *
     * @param ourPalette
     */
    public void initTransPalette(OurPalette ourPalette) {
        int paletteSize = ((PaletteAdapter) ourPalette.getAdapter()).getSize();

        old_palette = new float[3 * paletteSize];
        for (int i=0; i<paletteSize; i++) {
            int color = ((PaletteAdapter) ourPalette.getAdapter()).getColor(i);
            double [] lab_color = new double[3];
            ColorUtils.colorToLAB(color, lab_color);
            for (int j=0; j<3; j++)
                old_palette[i * 3 + j] = (float) lab_color[j];
        }

        palette_weights = new float[paletteSize * paletteSize + 1];
        float []palette_distance = new float [paletteSize * paletteSize + 1];
        double [][]palette_distance_2D = new double [paletteSize][paletteSize];
        Allocation allocationOld = Allocation.createSized(rs, Element.F32_3(rs), paletteSize, Allocation.USAGE_SCRIPT);
        allocationOld.setAutoPadding(true);
        allocationOld.copyFrom(old_palette);
        Allocation allocationDistance = Allocation.createSized(rs, Element.F32(rs), paletteSize * paletteSize + 1, Allocation.USAGE_SCRIPT);
        colorScript.set_old_palette(allocationOld);
        colorScript.set_palette_distance(allocationDistance);
        colorScript.invoke_calculate_distance(paletteSize);
        allocationDistance.copyTo(palette_distance);
        float palette_mean_distance = palette_distance[paletteSize * paletteSize];

        allocationDistance.destroy();
        allocationOld.destroy();

        for (int i=0; i<paletteSize; i++)
            for (int j=0; j<paletteSize; j++)
                palette_distance_2D[i][j] = palette_distance[i * paletteSize + j];
        Jama.Matrix A = new Jama.Matrix(palette_distance_2D);
        Jama.Matrix B = Jama.Matrix.identity(paletteSize, paletteSize);
        Jama.Matrix C = A.solve(B);
        palette_distance_2D = C.getArray();
        for (int i=0; i<paletteSize; i++)
            for (int j=0; j<paletteSize; j++)
                palette_weights[i * paletteSize + j] = (float)(palette_distance_2D[i][j]);
        palette_weights[paletteSize * paletteSize] = palette_mean_distance;
    }

    public void transGrid(OurPalette ourPalette) {
        PaletteAdapter paletteAdapter = ((PaletteAdapter) ourPalette.getAdapter());
        int paletteSize = paletteAdapter.getSize();

        /// Sorting the palette by luminosity to be sure it is sorted as desired
//        List<LabColor> colorsList = new ArrayList<>();

        // We create the new palette
        float[] new_palette = new float[3 * paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            int color = paletteAdapter.getColor(i);
            double[] lab_color = new double[3];
            ColorUtils.colorToLAB(color, lab_color);
//            colorsList.add(new LabColor(lab_color));
            for (int j = 0; j < 3; j++)
                new_palette[3 * i + j] = (float) lab_color[j];
        }

        // We sort the palette by luminance
//        Collections.sort(colorsList, new Comparator<LabColor>() {
//            @Override
//            public int compare(LabColor o1, LabColor o2) {
//                return (o1.getL() > o2.getL()) ? 1 : (o1.getL() < o2.getL()) ? -1 : 0;
//            }
//        });

//        for (int i = 0; i < colorsList.size(); i++) {
//            double[] lab_color = colorsList.get(i).getLab();
//            for (int j = 0; j < 3; j++)
//                new_palette[3 * i + j] = (float) lab_color[j];
//        }

        Allocation allocationOld = Allocation.createSized(rs, Element.F32_3(rs), paletteSize, Allocation.USAGE_SCRIPT);
        allocationOld.setAutoPadding(true);
        allocationOld.copyFrom(old_palette);
        Allocation allocationNew = Allocation.createSized(rs, Element.F32_3(rs), paletteSize, Allocation.USAGE_SCRIPT);
        allocationNew.setAutoPadding(true);
        allocationNew.copyFrom(new_palette);
        Allocation allocationDiff = Allocation.createSized(rs, Element.F32_3(rs), paletteSize, Allocation.USAGE_SCRIPT);
        allocationDiff.setAutoPadding(true);
        Allocation allocation_ccbl = Allocation.createSized(rs, Element.F32(rs), paletteSize, Allocation.USAGE_SCRIPT);
        Allocation allocation_ccl = Allocation.createSized(rs, Element.F32(rs), paletteSize, Allocation.USAGE_SCRIPT);
        Allocation allocation_weights = Allocation.createSized(rs, Element.F32(rs), paletteSize * paletteSize + 1, Allocation.USAGE_SCRIPT);
        allocation_weights.copyFrom(palette_weights);
        colorScript.set_old_palette(allocationOld);
        colorScript.set_new_palette(allocationNew);
        colorScript.set_paletteSize(paletteSize);
        colorScript.set_diff(allocationDiff);
        colorScript.set_ccb_l(allocation_ccbl);
        colorScript.set_cc_l(allocation_ccl);
        colorScript.set_palette_weights(allocation_weights);


        int g1 = grid_g + 1;
        Allocation allocationGrid = Allocation.createSized(rs, Element.F32_3(rs), g1 * g1 * g1, Allocation.USAGE_SCRIPT);
        allocationGrid.setAutoPadding(true);
        allocationGrid.copyFrom(grid);
        Allocation allocationTempGrid = Allocation.createTyped(rs, allocationGrid.getType());
        allocationTempGrid.setAutoPadding(true);

        colorScript.invoke_cal_palette_rate();
        colorScript.forEach_grid_transfer(allocationGrid, allocationTempGrid);
        allocationTempGrid.copyTo(temp_grid);

        allocationOld.destroy();
        allocationNew.destroy();
        allocationDiff.destroy();
        allocation_ccbl.destroy();
        allocation_ccl.destroy();
        allocationGrid.destroy();
        allocationTempGrid.destroy();
        allocation_weights.destroy();
    }

    private static final int b = 16;
    public Map<Kmeans.BinsTriplet, Pair<LabColor, Integer>> generateBins(int k, Bitmap img) {
        Map<Kmeans.BinsTriplet, Pair<LabColor, Integer>> res = new HashMap<Kmeans.BinsTriplet, Pair<LabColor, Integer>>();
        int img_size = img.getWidth() * img.getHeight();
        int[] pixels = new int[img_size];
        img.getPixels(pixels, 0, img.getWidth(), 0, 0, img.getWidth(), img.getHeight());

        Allocation allocationInput = Allocation.createSized(rs, Element.I32(rs), img_size, Allocation.USAGE_SCRIPT);
        allocationInput.copyFrom(pixels);
        Allocation allocationBinIndex = Allocation.createSized(rs, Element.I32_3(rs), img_size, Allocation.USAGE_SCRIPT);
        Allocation allocationLab = Allocation.createSized(rs, Element.F32_3(rs), img_size, Allocation.USAGE_SCRIPT);
        Allocation allocationBinLab = Allocation.createSized(rs, Element.F32_3(rs), b * b * b, Allocation.USAGE_SCRIPT);
        Allocation allocationBinNum = Allocation.createSized(rs, Element.I32(rs), b * b * b, Allocation.USAGE_SCRIPT);

        allocationBinIndex.setAutoPadding(true);
        allocationLab.setAutoPadding(true);
        allocationBinLab.setAutoPadding(true);

        colorScript.forEach_image_to_lab(allocationInput, allocationLab);
        colorScript.set_bin_b(b);
        colorScript.forEach_image_to_binIndex(allocationInput, allocationBinIndex);
        colorScript.set_image_size(img_size);
        colorScript.invoke_image_to_bins(allocationLab, allocationBinIndex,
                allocationBinLab, allocationBinNum);
        float[] bin_lab = new float[b * b * b * 3];
        int[] bin_num = new int[b * b * b];
        allocationBinNum.copyTo(bin_num);
        allocationBinLab.copyTo(bin_lab);
        for (int ir=0; ir<b; ir++)
            for (int ig=0; ig<b; ig++)
                for (int ib = 0; ib < b; ib++) {
                    int index = ir * b * b + ig * b + ib;
                    if (bin_num[index] > 0) {
                        Kmeans.BinsTriplet triplet = new Kmeans.BinsTriplet(ir, ig, ib);
                        int num = bin_num[index];
                        LabColor lab = new LabColor(bin_lab[index * 3 + 0],
                                bin_lab[index * 3 + 1],
                                bin_lab[index * 3 + 2]);
                        res.put(triplet, Pair.create(lab, num));
                    }
                }

        allocationInput.destroy();
        allocationBinIndex.destroy();
        allocationLab.destroy();
        allocationBinLab.destroy();
        allocationBinNum.destroy();
        return res;
    }

    public List<LabColor> KMean_cluster(List<LabColor> paletteClusters,
                                Map<Kmeans.BinsTriplet, Pair<LabColor, Integer>> bins,
                                int K) {
        List<Pair<LabColor, Integer>> binsList = new ArrayList<Pair<LabColor, Integer>>(bins.values());

        int binNum = binsList.size();
        Allocation allocationBinLab = Allocation.createSized(rs, Element.F32_3(rs), binNum, Allocation.USAGE_SCRIPT);
        Allocation allocationBinNum = Allocation.createSized(rs, Element.I32(rs), binNum, Allocation.USAGE_SCRIPT);
        Allocation allocationPalette = Allocation.createSized(rs, Element.F32_3(rs), K + 1, Allocation.USAGE_SCRIPT);
        Allocation allocationColorSum = Allocation.createSized(rs, Element.F32_3(rs), K + 1, Allocation.USAGE_SCRIPT);
        Allocation allocationColorNum = Allocation.createSized(rs, Element.I32(rs), K + 1, Allocation.USAGE_SCRIPT);

        allocationBinLab.setAutoPadding(true);
        allocationPalette.setAutoPadding(true);
        allocationColorSum.setAutoPadding(true);
        float[] palette = new float[(K + 1) * 3];
        for (int i=0; i<K+1; i++) {
            LabColor color = paletteClusters.get(i);
            palette[i * 3 + 0] = (float)(color.getL());
            palette[i * 3 + 1] = (float)(color.getA());
            palette[i * 3 + 2] = (float)(color.getB());
        }
        allocationPalette.copyFrom(palette);
        float[] binsColor = new float[binNum * 3];
        int[] binsNum = new int[binNum];
        for (int i=0; i<binNum; i++) {
            Pair<LabColor, Integer> pair = binsList.get(i);
            binsNum[i] = pair.second;
            binsColor[i * 3 + 0] = (float)(pair.first.getL());
            binsColor[i * 3 + 1] = (float)(pair.first.getA());
            binsColor[i * 3 + 2] = (float)(pair.first.getB());
        }
        allocationBinLab.copyFrom(binsColor);
        allocationBinNum.copyFrom(binsNum);

        colorScript.invoke_kmean_cluster(allocationBinLab, allocationBinNum, allocationPalette,
                allocationColorSum, allocationColorNum, K, binNum);
        allocationPalette.copyTo(palette);

        for (int i=0; i<K+1; i++) {
            LabColor color = new LabColor(palette[i * 3 + 0],
                    palette[i * 3 + 1],
                    palette[i * 3 + 2]);
            paletteClusters.set(i, color);
        }
        allocationBinLab.destroy();
        allocationBinNum.destroy();
        allocationPalette.destroy();
        allocationColorSum.destroy();
        allocationColorNum.destroy();
        return null;
    }

    public void rsClose() {
        colorScript.destroy();
        rs.destroy();
    }

}
