package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;


/**
 * Color box are uniform color discs used as palette element colors
 */
public class ColorBox extends AppCompatImageView {

    /**
     * Size of a normal colorBox
     */
    private final int NORMAL_SIZE = 45;


    /**
     * Size of a selected colorbox
     */
    private final static int SELECTED_SIZE = 50;


    /**
     * The uniform color of the box
     */
    private int mColor;


    /**
     * Diameter of the disc
     */
    private int diameter = NORMAL_SIZE;


    /**
     *
     */
    private Paint paint = new Paint();


    /**
     * Constructor
     * @param context for the colorbox
     */
    public ColorBox(Context context) {
        super(context);
    }


    /**
     * Constructor
     * @param context for the colorbox
     * @param attrs for the colorbox
     */
    public ColorBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * Constructor
     * @param context for the colorbox
     * @param attrs for the colorbox
     * @param defStyle for the colorbox
     */
    public ColorBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    /**
     * Set this color box as selected for display (Bigger diameter)
     */
    public void setSelected() {
        diameter = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, SELECTED_SIZE, getResources().getDisplayMetrics());
    }


    /**
     * Set this color box as non selected dor display (small diameter)
     */
    public void setNotSelected() {
        diameter = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, NORMAL_SIZE, getResources().getDisplayMetrics());
    }


    /**
     * Set the color of the Color box
     * @param color the new color of the box
     */
    public void setColor(int color) {
        mColor = color;
    }


    /**
     * Override onDraw to round bitmap before render it
     * @param canvas to draw on it
     */
    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(mColor);
        canvas.drawCircle(getWidth()/2, getHeight()/2, diameter/2, paint);
    }

}