package ch.epfl.cs413.palettev01.processing;

import android.graphics.Bitmap;

import ch.epfl.cs413.palettev01.views.Miniature;

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
     * Constructor empty for now
     */
    public PaletteBitmap(){

    }


    /**
     * Setter for the bitmap parameter
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap){
        this.bitmap = bitmap;
    }


    /**
     * Recycle the bit map
     */
    public void recycle(){
        if (bitmap != null) {
            bitmap.recycle();
        }
    }

    /**
     * Transform the bitmap to another bitmap which is muttable
     */
    public void makeMutable(){
        Bitmap pic = bitmap.copy(bitmap.getConfig(), true);;
        recycle();
        setBitmap(pic);
    }


    /**
     * Set the view Miniature with this bitmap
     * @param view
     */
    public void showBitmap(Miniature view){
        view.setImageBitmap(bitmap);
    }








}

