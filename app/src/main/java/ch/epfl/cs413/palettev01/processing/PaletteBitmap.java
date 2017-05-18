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
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.epfl.cs413.palettev01.ScriptC_color;
import ch.epfl.cs413.palettev01.views.Miniature;
import ch.epfl.cs413.palettev01.views.OurPalette;
import ch.epfl.cs413.palettev01.views.PaletteAdapter;

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
     * Scaled bitmap to reduce complexity
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

        try {
            file = new File(root, imageFileName);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("<<ERROR>>", "File creation is not working.. PaletteBitmap_L214");
        }

        return Uri.fromFile(file);
    }


    /**
     * Export the bitmap in JPEG format into the gallery
     * at the same emplacement (with other timestamp) of the camera taken pictures
     */
    public void exportImage() {

        if (file != null) {

            final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "palette" + File.separator);
            root.mkdirs();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + ".jpg";

            File exportFile = new File(root, imageFileName);

            FileOutputStream out = null;
            try {

                // TODO APPLY THE TRANSFORM TO THE FULL SIZE BITMAP TO USE IT HERE
                //Bitmap bmp = bitmap.copy(bitmap.getConfig(), true);
                Bitmap bmp = scaled.copy(scaled.getConfig(), true);
                out = new FileOutputStream(exportFile);

                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
                //bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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

    // TODO PUT JAVA DOC AND COMMENT FROM HERE IN THIS CLASS

    private RenderScript rs;
    private ScriptC_color colorScript;
    private Bitmap input_pic;
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

        allocationOld.destroy();
        allocationNew.destroy();
        allocationDiff.destroy();
        allocation_ccbl.destroy();
        allocation_ccl.destroy();
        allocationGrid.destroy();
        allocationTempGrid.destroy();
        allocation_weights.destroy();
    }

    public void rsClose() {
        colorScript.destroy();
        rs.destroy();
    }

    public Bitmap getScaled() {
        return scaled;
    }


}

