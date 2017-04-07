package ch.epfl.cs413.palettev01.processing;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ch.epfl.cs413.palettev01.views.Miniature;
import ch.epfl.cs413.palettev01.views.Palette;
import ch.epfl.cs413.palettev01.views.PaletteAdapter;
import android.content.Context;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.Type;
import ch.epfl.cs413.palettev01.ScriptC_hist;


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

    public void myFunction(Miniature v, Context context){
        Log.d("BW", ""+bitmap.getWidth());
        Log.d("BH", ""+bitmap.getHeight());
        Log.d("H", ""+scaled.getHeight());
        Log.d("W", ""+scaled.getWidth());

        long startTime = System.nanoTime();
        int test;
        int r,g,b,a;
        for (int i= 0; i < 2; i++)
            for (int j=0; j<2; j++) {
                test = scaled.getPixel(i * 100, j * 100);
                a = (test >> 24) & 0xff;
                r = (test >> 16) & 0xff;
                g = (test >> 8) & 0xff;
                b = test & 0xff;
                Log.d(Integer.toString(i) + "," + Integer.toString(j), Integer.toHexString(test) + " " + Integer.toString(r) + " " + Integer.toString(g) + " " + Integer.toString(b) + " " + Integer.toString(a));
            }
        Bitmap res = histogramEqualization(scaled, context);
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

    public Bitmap histogramEqualization(Bitmap image, Context context) {
        //Get image size
        int width = image.getWidth();
        int height = image.getHeight();

        //Create new bitmap
        Bitmap res = image.copy(image.getConfig(), true);

        //Create renderscript
        RenderScript rs = RenderScript.create(context);

        //Create allocation from Bitmap
        Allocation allocationA = Allocation.createFromBitmap(rs, res);

        //Create allocation with same type
        Allocation allocationB = Allocation.createTyped(rs, allocationA.getType());

        //Create script from rs file.
        ScriptC_hist histEqScript = new ScriptC_hist(rs);

        //Call the first kernel.
        histEqScript.forEach_test(allocationA, allocationB);

        //Copy script result into bitmap
        allocationB.copyTo(res);

        //Destroy everything to free memory
        allocationA.destroy();
        allocationB.destroy();
        histEqScript.destroy();
        rs.destroy();

        return res;
    }

    public Bitmap getScaled() {
        return scaled;
    }
}

