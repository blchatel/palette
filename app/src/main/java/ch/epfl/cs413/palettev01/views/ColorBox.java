package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;


/**
 * Created by bastien on 14.03.17.
 */
public class ColorBox extends AppCompatImageView {
    private final int normalSize = 45;
    private final int selectedSize = 50;

    private int mColor;

    private int diameter = normalSize;

    public ColorBox(Context context) {
        super(context);
    }

    public ColorBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setSelected() {
        diameter = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, selectedSize, getResources().getDisplayMetrics());
    }

    public void setNotSelected() {
        diameter = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, normalSize, getResources().getDisplayMetrics());
    }

    public ColorBox(Context context, int color) {
        super(context);
    }


    Paint paint = new Paint();
    /**
     * Override onDraw to round bitmap before render it
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        paint.setColor(mColor);
        Log.d("BOX_SIZE", "Width : " + getWidth() + " and Height : " + getHeight());
        canvas.drawCircle(getWidth()/2, getHeight()/2, diameter / 2, paint);
    }

    /**
     * Set the color of the Color box using a uniform bitmap
     * @param color
     */
    public void setColor(int color) {
        mColor = color;
    }
}
