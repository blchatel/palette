package ch.epfl.cs413.palettev01.processing;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.graphics.ColorUtils;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;

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
    // TODO: Refactoring for more readability
//    private LabColor[][][] tempGrid;
//    private LabColor[][][] newGrid;
//    private LabColor[] oldPalette;


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
        //TODO CLEAN THIS COMMENTED CODE
//        oldPalette = new LabColor[paletteSize];
//        for (int i=0; i < paletteSize; i++) {
//            int color = ((PaletteAdapter)palette.getAdapter()).getColor(i);
//            double [] labColor = new double[3];
//            ColorUtils.colorToLAB(color, labColor);
//            oldPalette[i] = new LabColor(labColor);
//        }

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
        //TODO CLEAN THIS COMMENTED CODE
        /*
        Log.d("dis mean", Float.toString(palette_mean_distance));
        for (int i=0; i<paletteSize; i++)
            Log.d("dis " + Integer.toString(i), Float.toString(palette_distance[i * paletteSize + 0]) + " "
                    + Float.toString(palette_distance[i * paletteSize + 1]) + " "
                    + Float.toString(palette_distance[i * paletteSize + 2]) + " "
                    + Float.toString(palette_distance[i * paletteSize + 3]) + " "
                    + Float.toString(palette_distance[i * paletteSize + 4]));
        for (int i=0; i<paletteSize; i++)
            Log.d(Integer.toString(i), Float.toString(palette_weights[i * paletteSize + 0]) + " "
                                       + Float.toString(palette_weights[i * paletteSize + 1]) + " "
                                       + Float.toString(palette_weights[i * paletteSize + 2]) + " "
                                       + Float.toString(palette_weights[i * paletteSize + 3]) + " "
                                       + Float.toString(palette_weights[i * paletteSize + 4]));
        */
    }

    public void transGrid(OurPalette ourPalette, int changedIndex) {
        PaletteAdapter paletteAdapter = ((PaletteAdapter) ourPalette.getAdapter());
        int paletteSize = paletteAdapter.getSize();

        // We create the new palette
        float[] new_palette = new float[3 * paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            int color = paletteAdapter.getColor(i);
            double[] lab_color = new double[3];
            ColorUtils.colorToLAB(color, lab_color);
            // TODO CLEAN THIS COMMENTED CODE
//            LabColor oldC = new LabColor(old_palette[i*3], old_palette[i*3+1], old_palette[i*3+2]);
//            LabColor newC = new LabColor(lab_color);
//            if (!oldC.equals(newC)) {
//                Log.d("PALETTE_COLOR", "Color changed from " + oldC + " to " + newC + " at position " + i);
//                changedIndex =  i;
//            }
            for (int j=0; j<3; j++)
                new_palette[3*i + j] = (float)lab_color[j];
        }

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

        // TODO CLEAN THIS COMMENTED CODE
        /*
        float[] diff_value = new float[paletteSize * 3];
        float[] rate_value = new float[paletteSize];
        float[] max_value = new float[paletteSize];
        allocationDiff.copyTo(diff_value);
        allocation_rate.copyTo(rate_value);
        allocation_max.copyTo(max_value);
        for (int i=0; i<paletteSize; i++) {
            Log.d("info0 " + Integer.toString(i), Float.toString(old_palette[i * 3 + 0]) + " " +
                    Float.toString(old_palette[i * 3 + 1]) + " " +
                    Float.toString(old_palette[i * 3 + 2]) + " " +
                    Float.toString(new_palette[i * 3 + 0]) + " " +
                    Float.toString(new_palette[i * 3 + 1]) + " " +
                    Float.toString(new_palette[i * 3 + 2]));
            Log.d("info1 " + Integer.toString(i), Float.toString(diff_value[i * 3 + 0]) + " " +
                    Float.toString(diff_value[i * 3 + 1]) + " " +
                    Float.toString(diff_value[i * 3 + 2]) + " " +
                    Float.toString(rate_value[i]) + " " +
                    Float.toString(max_value[i]));
        }
        int test_i;
        test_i = 0;
        Log.d("old " + Integer.toString(test_i), Float.toString(grid[test_i * 3 + 0]) + " " +
                Float.toString(grid[test_i * 3 + 1]) + " " +
                Float.toString(grid[test_i * 3 + 2]));
        Log.d(Integer.toString(test_i), Float.toString(temp_grid[test_i * 3 + 0]) + " " +
                   Float.toString(temp_grid[test_i * 3 + 1]) + " " +
                   Float.toString(temp_grid[test_i * 3 + 2]));
        test_i = 8;
        Log.d("old " + Integer.toString(test_i), Float.toString(grid[test_i * 3 + 0]) + " " +
                Float.toString(grid[test_i * 3 + 1]) + " " +
                Float.toString(grid[test_i * 3 + 2]));
        Log.d(Integer.toString(test_i), Float.toString(temp_grid[test_i * 3 + 0]) + " " +
                Float.toString(temp_grid[test_i * 3 + 1]) + " " +
                Float.toString(temp_grid[test_i * 3 + 2]));
        test_i = 13;
        Log.d("old " + Integer.toString(test_i), Float.toString(grid[test_i * 3 + 0]) + " " +
                Float.toString(grid[test_i * 3 + 1]) + " " +
                Float.toString(grid[test_i * 3 + 2]));
        Log.d(Integer.toString(test_i), Float.toString(temp_grid[test_i * 3 + 0]) + " " +
                Float.toString(temp_grid[test_i * 3 + 1]) + " " +
                Float.toString(temp_grid[test_i * 3 + 2]));
        test_i = 26;
        Log.d("old " + Integer.toString(test_i), Float.toString(grid[test_i * 3 + 0]) + " " +
                Float.toString(grid[test_i * 3 + 1]) + " " +
                Float.toString(grid[test_i * 3 + 2]));
        Log.d(Integer.toString(test_i), Float.toString(temp_grid[test_i * 3 + 0]) + " " +
                Float.toString(temp_grid[test_i * 3 + 1]) + " " +
                Float.toString(temp_grid[test_i * 3 + 2]));
        */

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
                        LabColor lab = new LabColor(bin_lab[index * 3 + 0] / num,
                                bin_lab[index * 3 + 1] / num,
                                bin_lab[index * 3 + 2] / num);
                        res.put(triplet, Pair.create(lab, num));
                    }
                }


        /*
        float[] pixel_lab = new float[img_size * 3];
        int[] pixel_bin = new int[img_size * 3];
        allocationBinIndex.copyTo(pixel_bin);
        allocationLab.copyTo(pixel_lab);
        int invB = (int)(Math.ceil(256.0/b));
        for (int i=0; i<5; i++) {
            int index = i * 50;
            RGBColor rgb = RGBColor.intToRGB(pixels[index]);
            double[] Lab = new double[3];
            ColorUtils.RGBToLAB(rgb.r, rgb.g, rgb.b, Lab);
            Log.d(Integer.toString(index), Integer.toString(rgb.r) + " " +
                    Integer.toString(rgb.g) + " " +
                    Integer.toString(rgb.b));
            Log.d(Integer.toString(index), Integer.toString(rgb.r / invB) + " " +
                    Integer.toString(rgb.g / invB) + " " +
                    Integer.toString(rgb.b / invB));
            Log.d(Integer.toString(index), Double.toString(Lab[0]) + " " +
                    Double.toString(Lab[1]) + " " +
                    Double.toString(Lab[2]));
            Log.d(Integer.toString(index), Integer.toString(pixel_bin[index * 3 + 0]) + " " +
                    Integer.toString(pixel_bin[index * 3 + 1]) + " " +
                    Integer.toString(pixel_bin[index * 3 + 2]));
            Log.d(Integer.toString(index), Float.toString(pixel_lab[index * 3 + 0]) + " " +
                    Float.toString(pixel_lab[index * 3 + 1]) + " " +
                    Float.toString(pixel_lab[index * 3 + 2]));
        }
        */

        allocationInput.destroy();
        allocationBinIndex.destroy();
        allocationLab.destroy();
        allocationBinLab.destroy();
        allocationBinNum.destroy();
        return res;
    }

    public void rsClose() {
        colorScript.destroy();
        rs.destroy();
    }

}
