package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

/**
 * This class is the view representing our image, inheriting from AppCompatImageView class
 */
public class Miniature extends AppCompatImageView {

    /**
     * Constructor
     * @param context for the miniature
     */
    public Miniature(Context context) {
        super(context);
    }


    /**
     * Constructor
     * @param context for the miniature
     * @param attrs for the miniature
     */
    public Miniature(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * Constructor
     * @param context for the miniature
     * @param attrs for the miniature
     * @param defStyle for the miniature
     */
    public Miniature(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
