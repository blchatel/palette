package ch.epfl.cs413.palettev01.processing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.graphics.ColorUtils;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.epfl.cs413.palettev01.views.Miniature;
import ch.epfl.cs413.palettev01.views.Palette;
import ch.epfl.cs413.palettev01.views.PaletteAdapter;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.Type;
import ch.epfl.cs413.palettev01.ScriptC_color;


/**
 * Created by bastien on 21.03.17.
 */


// The idea of this class is not providing any bitmap getter to control modification


public class PaletteBitmap {


    /**
     * Private bitmap attribute we can set mofify and transform inside this class
     * The bitmap class is final and so cannot be extended. We need to have a bitmap because
     * we cannot be one
     */
    private Bitmap bitmap ;

    /**
     * Scalled bitmap to reduce complexity
     */
    private Bitmap scaled ;

    /**
     * The file connected with the bitmap. Useful for loading
     */
    private File file;

    /**
     * Height of the destination Miniature View for the bitmap
     */
    private int height;

    /**
     * Width of the destination Miniature View for the bitmap
     */
    private int width;





    /**
     * Constructor empty for now
     */
    public PaletteBitmap(){

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///  SETTERS
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Set the file to null
     */
    public void setFileToNull(){
        file = null;
    }

    /**
     * Setter for the file. This function is more meaningful with restore name
     * @param file
     */
    public void restoreFile(String file){
        Log.d("PaletteBitmap <<<<", file + " and null ? " + isFileNull());
        this.file = new File(file);
    }

    /**
     * Setter for the height
     * @param height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Setter for the width
     * @param width
     */
    public void setWidth(int width) {
        this.width = width;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///  GETTER
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Check if the bitmap has been initialized yet
     * @return
     */
    public boolean isEmpty () {
        return bitmap==null;
    }

    /**
     * Check if the file attribute is null
     * @return boolean
     */
    public boolean isFileNull(){
        return file == null;
    }

    /**
     * Get the absolute path of the file
     * @return the absolute path
     */
    public String fileAbsolutePath(){
        return file.getAbsolutePath();
    }

    /**
     * Get the Uri of the file
     * @return the Uri of the file
     */
    public Uri getUri(){
        return Uri.fromFile(file);
    }

    /**
     * Get the height
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Get the width
     * @return the width
     */
    public int getWidth() {
        return width;
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///  Functionality
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public void recycle(){

        if(bitmap != null){
            bitmap.recycle();
        }
        if(scaled != null){
            scaled.recycle();
        }

    }

    /**
     * Set the picture and scale it if needed
     * @param v
     */
    public void setPicture(Miniature v) {

        // Free the data of the last picture
        recycle();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        if(photoW > photoH) {
            bitmap = rotateImage(bitmap, 90);
            int temp = photoH;
            photoH = photoW;
            photoW = temp;
        }

        // Determine how much to scale down the image
        float scaleFactor = Math.max(photoW/(float)width, photoH/(float)height);
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, (int)(photoW/scaleFactor), (int)(photoH/scaleFactor), true);

        // Make bitmap and scaled mutable
        // Avoid IllegalStateException with Immutable bitmap
        this.bitmap = bitmap.copy(bitmap.getConfig(), true);
        this.scaled = scaled.copy(scaled.getConfig(), true);

        //Set the view Miniature with this bitmap
        v.setImageBitmap(scaled);
    }


    /**
     * Prepare a file (.jpg) where to save a new image file (i.e. a camera file)
     * @throws IOException
     */
    public Uri prepareImageFile() throws IOException {
        // Create an image file name
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "palette" + File.separator);
        root.mkdirs();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
//        final File sdImageMainDirectory = new File(root, imageFileName);

        try {
//            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + "Palette", imageFileName);
            file = new File(root, imageFileName);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("<<ERROR>>", "File creation is not working.. PaletteBitmap_L214");
        }

        return Uri.fromFile(file);
    }

    /**
     * Rotate a source bitmap to angle degree
     * @param source
     * @param angle
     * @return
     */
    private Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * Get the color of the (x, y) pixel
     * @param miniatureX
     * @param miniatureY
     * @return the color integer
     */
    public int getColor(int miniatureX, int miniatureY){

        try {
            int x = (int)(miniatureX - 0.5*(width -scaled.getWidth()));
            int y = (int)(miniatureY - 0.5*(height -scaled.getHeight()));

            if(x < 0 || y < 0 || x >= scaled.getWidth() || y >= scaled.getHeight())
                return Color.TRANSPARENT;
            else {
                return scaled.getPixel(x, y);
            }
        }
        catch(Exception e){
            return Color.TRANSPARENT;
        }
    }

    public void transformBlackAndWhite(Miniature v){

        Log.d("BW", ""+bitmap.getWidth());
        Log.d("BH", ""+bitmap.getHeight());
        Log.d("H", ""+scaled.getHeight());
        Log.d("W", ""+scaled.getWidth());

        for(int x = 0; x < scaled.getWidth(); x++){
            for(int y = 0; y < scaled.getHeight(); y++) {

                int pixel = scaled.getPixel(x, y);

                // Black = -16777216
                // white = -1
                if(pixel > -8388608){
                    scaled.setPixel(x, y, Color.WHITE);
                }else{
                    scaled.setPixel(x, y, Color.BLACK);
                }
            }
        }

        v.setImageBitmap(scaled);
    }

    public void extractPalette(Palette palette) {
        int paletteSize = PaletteAdapter.PALETTE_SIZE;
        Bitmap smallImage = Bitmap.createScaledBitmap(this.scaled, 200, 200, false);
        Kmeans kmeans = new Kmeans(paletteSize, smallImage);
        List<LabColor> paletteColors = kmeans.run();
        Collections.sort(paletteColors, new Comparator<LabColor>() {
            @Override
            public int compare(LabColor o1, LabColor o2) {
                return (o1.L < o2.L) ? 1 : (o1.L > o2.L) ? -1 : 0;
            }
        });
        Log.d("<PaletteBitmap>", "Palette has been computed " + paletteColors.size());
        for (int i = 0; i < paletteSize; i++) {
            LabColor Lab = paletteColors.get(i);
            Log.d("<<Sorted>>", "Luminosity is " + Lab.L);
            ((PaletteAdapter)palette.getAdapter()).setColor(i, ColorUtils.LABToColor(Lab.L, Lab.a, Lab.b));
        }
    }

    public void myFunction(Miniature v){

        long startTime = System.nanoTime();
        int test;
        int r,g,b,a;
        Bitmap res = histogramEqualization(scaled);
        scaled = res;
        for (int i= 0; i < 2; i++)
            for (int j=0; j<2; j++) {
                test = scaled.getPixel(i * 100, j * 100);
                a = (test >> 24) & 0xff;
                r = (test >> 16) & 0xff;
                g = (test >> 8) & 0xff;
                b = test & 0xff;
                Log.d(Integer.toString(i) + "," + Integer.toString(j), Integer.toHexString(test) + " " + Integer.toString(r) + " " + Integer.toString(g) + " " + Integer.toString(b) + " " + Integer.toString(a));
            }
        long consumingTime = System.nanoTime() - startTime;
        Log.d("time", Long.toString(consumingTime));

        v.setImageBitmap(scaled);
    }

    private RenderScript rs;
    private ScriptC_color colorScript;
    private Bitmap input_pic;
    private float[] grid;
    private float[] temp_grid;
    private int grid_g;
    private float[] old_palette;

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

    public void initGrid() {
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
        input_pic = Bitmap.createBitmap(scaled);
    }

    public void transImage(Miniature v) {
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
        v.setImageBitmap(scaled);
    }


    /**
     * Create old_palette with the given palette
     *
     * @param palette
     */
    public void initTransPalette(Palette palette) {
        int paletteSize = ((PaletteAdapter)palette.getAdapter()).getSize();
//        oldPalette = new LabColor[paletteSize];
//        for (int i=0; i < paletteSize; i++) {
//            int color = ((PaletteAdapter)palette.getAdapter()).getColor(i);
//            double [] labColor = new double[3];
//            ColorUtils.colorToLAB(color, labColor);
//            oldPalette[i] = new LabColor(labColor);
//        }

        old_palette = new float[3 * paletteSize];
        for (int i=0; i<paletteSize; i++) {
            int color = ((PaletteAdapter)palette.getAdapter()).getColor(i);
            double [] lab_color = new double[3];
            ColorUtils.colorToLAB(color, lab_color);
            for (int j=0; j<3; j++)
                old_palette[i * 3 + j] = (float) lab_color[j];
        }
    }

    public void transGrid(Palette palette, int changedIndex) {
        PaletteAdapter paletteAdapter = ((PaletteAdapter)palette.getAdapter());
        int paletteSize = paletteAdapter.getSize();

        // We create the new palette
        float[] new_palette = new float[3 * paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            int color = paletteAdapter.getColor(i);
            double[] lab_color = new double[3];
            ColorUtils.colorToLAB(color, lab_color);
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
        Allocation allocation_rate = Allocation.createSized(rs, Element.F32(rs), paletteSize, Allocation.USAGE_SCRIPT);
        colorScript.set_old_palette(allocationOld);
        colorScript.set_new_palette(allocationNew);
        colorScript.set_paletteSize(paletteSize);
        colorScript.set_diff(allocationDiff);
        colorScript.set_c_rate(allocation_rate);


        int g1 = grid_g + 1;
        Allocation allocationGrid = Allocation.createSized(rs, Element.F32_3(rs), g1 * g1 * g1, Allocation.USAGE_SCRIPT);
        allocationGrid.setAutoPadding(true);
        allocationGrid.copyFrom(grid);
        Allocation allocationTempGrid = Allocation.createTyped(rs, allocationGrid.getType());
        allocationTempGrid.setAutoPadding(true);

        colorScript.invoke_cal_palette_rate();
        colorScript.set_i(changedIndex);
        colorScript.forEach_grid_transfer_i(allocationGrid, allocationTempGrid);
        allocationTempGrid.copyTo(temp_grid);

        allocationOld.destroy();
        allocationNew.destroy();
        allocationDiff.destroy();
        allocation_rate.destroy();
        allocationGrid.destroy();
        allocationTempGrid.destroy();
    }

    public void rsClose() {
        colorScript.destroy();
        rs.destroy();
    }

    public Bitmap histogramEqualization(Bitmap image) {
        //Get image size
        int width = image.getWidth();
        int height = image.getHeight();

        //Create new bitmap
        Bitmap res = image.copy(image.getConfig(), true);

        //Create allocation from Bitmap
        Allocation allocationA = Allocation.createFromBitmap(rs, res);

        //Create allocation with same type
        Allocation allocationB = Allocation.createTyped(rs, allocationA.getType());

        float[] grid = new float[30];
        Allocation allocationGrid = Allocation.createSized(rs, Element.F32_3(rs), 10, Allocation.USAGE_SCRIPT);
        allocationGrid.setAutoPadding(true);
        allocationGrid.copyFrom(grid);
        colorScript.set_grid(allocationGrid);
        colorScript.invoke_initGrid2();

        //Call the first kernel.
        colorScript.forEach_test(allocationA, allocationA);

        //Copy script result into bitmap
        allocationA.copyTo(res);

        for (int i=0; i<8; i++)
            Log.d("test" + Integer.toString(i), Float.toString(grid[i]));

        allocationGrid.copyTo(grid);
        for (int i=0; i<8; i++)
            Log.d("test" + Integer.toString(i), Float.toString(grid[i]));

        //Destroy everything to free memory
        allocationA.destroy();
        allocationB.destroy();
        allocationGrid.destroy();

        return res;
    }

    public Bitmap getScaled() {
        return scaled;
    }


    /**
     * Test functions
     */

    public void testInitTransPalette(Palette palette) {
        old_palette = new float[3];
        double [] lab_color = new double[3];
        // int color = ((PaletteAdapter)palette.getAdapter()).getColor(2);
        int color = 0xff000000 + 158 * 0x10000 + 182 * 0x100 + 215;
        ColorUtils.colorToLAB(color, lab_color);
        for (int i=0; i<3; i++)
            old_palette[i] = (float) lab_color[i];
    }

    public void testTransGrid(Palette palette) {
        float[] new_palette = new float[3];
        int paletteSize = 1;
        double [] lab_color = new double[3];
        // int color = ((PaletteAdapter)palette.getAdapter()).getColor(2) + 0x100000;
        int color = 0xff000000 + 178 * 0x10000 + 128 * 0x100 + 128;
        ColorUtils.colorToLAB(color, lab_color);
        for (int i=0; i<3; i++)
            new_palette[i] = (float) lab_color[i];
        Allocation allocationOld = Allocation.createSized(rs, Element.F32_3(rs), paletteSize, Allocation.USAGE_SCRIPT);
        allocationOld.setAutoPadding(true);
        allocationOld.copyFrom(old_palette);
        Allocation allocationNew = Allocation.createSized(rs, Element.F32_3(rs), paletteSize, Allocation.USAGE_SCRIPT);
        allocationNew.setAutoPadding(true);
        allocationNew.copyFrom(new_palette);
        Allocation allocationDiff = Allocation.createSized(rs, Element.F32_3(rs), paletteSize, Allocation.USAGE_SCRIPT);
        Allocation allocation_rate = Allocation.createSized(rs, Element.F32(rs), paletteSize, Allocation.USAGE_SCRIPT);
        colorScript.set_old_palette(allocationOld);
        colorScript.set_new_palette(allocationNew);
        colorScript.set_paletteSize(paletteSize);
        colorScript.set_diff(allocationDiff);
        colorScript.set_c_rate(allocation_rate);

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
        allocation_rate.destroy();
        allocationGrid.destroy();
        allocationTempGrid.destroy();
    }
}

