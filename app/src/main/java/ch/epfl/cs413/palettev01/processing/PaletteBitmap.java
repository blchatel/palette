package ch.epfl.cs413.palettev01.processing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.epfl.cs413.palettev01.views.Miniature;

/**
 * This class contains the different bitmap used by the application.
 */
public class PaletteBitmap {


    /**
     * Private bitmap attribute we can set modify and transform inside this class
     * This is the original image bitmap
     */
    private Bitmap bitmap ;

    /**
     * Scaled bitmap to reduce complexity
     * Displayed bitmap
     */
    private Bitmap scaled ;

    /**
     * Bitmap scaled to a constant size of 1024px on the bigger side
     * Used for the kmeans computation - To have the same palette extraction on every phones
     */
    private Bitmap kmean;

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
     * Default constructor
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

    /**
     * Getter for the scaled bitmap
     */
    public Bitmap getScaled() {
        return scaled;
    }

    /**
     * Getter for the kmean bitmap used for palette extraction
     */
    public Bitmap getKmean() {
        return kmean;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///  Functionalities
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Recycle all the bitmaps
     */
    public void recycle(){

        if(bitmap != null){
            bitmap.recycle();
        }
        if(scaled != null){
            scaled.recycle();
        }
        if(kmean != null){
            kmean.recycle();
        }

    }

    /**
     * Set the picture and scale it if needed
     * @param view
     */
    public void setPicture(Miniature view) {

        // Free the data of the last picture
        recycle();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // We rotate the image for optimal occupancy on display space if the image is reversed
        if(photoW > photoH) {
            bitmap = rotateImage(bitmap, 90);
            int temp = photoH;
            photoH = photoW;
            photoW = temp;
        }

        // Determine how much to scale down the image for optimal display - Phone dependent
        float scaleFactor = Math.max(photoW/(float)width, photoH/(float)height);
        this.scaled = Bitmap.createScaledBitmap(bitmap, (int)(photoW/scaleFactor), (int)(photoH/scaleFactor), true);

        // Scale the kmean's used bitmap to a limited size
        scaleFactor = Math.max(photoW/(float)1024, photoH/(float)1024);
        this.kmean = Bitmap.createScaledBitmap(bitmap, (int)(photoW/scaleFactor), (int)(photoH/scaleFactor), true);

        // Make bitmap and scaled mutable
        // Avoid IllegalStateException with Immutable bitmap
        this.bitmap = bitmap.copy(bitmap.getConfig(), true);

        // Display the scaled image on the view
        view.setImageBitmap(scaled);
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

                // TODO : If we want to export the full the higher resolution image should transform it here
                //Bitmap bmp = bitmap.copy(bitmap.getConfig(), true);

                // For now we export the scaled image
                Bitmap bmp = scaled.copy(scaled.getConfig(), true);
                out = new FileOutputStream(exportFile);

                // Export to a jpeg image
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
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

    /**
     * Display the scaled image in the given miniature view
     * @param v
     */
    public void displayScaledImage(Miniature v) {
        v.setImageBitmap(scaled);
    }
}

