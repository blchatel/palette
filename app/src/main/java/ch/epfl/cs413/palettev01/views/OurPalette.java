package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;


/**
 * This class represent our Palette structure class inheriting from a gridView
 * to be able to display the different colors.
 * More details and implementation in the adapter
 */
public class OurPalette extends GridView {


    /**
     * Constructor
     * @param context for the Palette
     */
    public OurPalette(Context context) {
        super(context);
    }


    /**
     * Constructor
     * @param context for the Palette
     * @param attrs for the Palette
     */
    public OurPalette(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * Constructor
     * @param context for the Palette
     * @param attrs for the Palette
     * @param defStyle for the Palette
     */
    public OurPalette(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
