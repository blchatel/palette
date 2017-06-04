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


    public OurPalette(Context context) {
        super(context);
    }

    public OurPalette(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OurPalette(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
