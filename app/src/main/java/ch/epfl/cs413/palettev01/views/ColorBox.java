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
import android.util.TypedValue;
import android.view.ViewGroup;


/**
 * Created by bastien on 14.03.17.
 */
public class ColorBox extends AppCompatImageView {
    private final int normalSize = 45;
    private final int selectedSize = 55;

    private Bitmap bitmap;
    private int radius = normalSize;

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
        int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, selectedSize, getResources().getDisplayMetrics());
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = size;
        params.width = size;
        // Don't forget to set the radius
        radius = size;
        setLayoutParams(params);
    }

    public void setNotSelected() {
        int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, normalSize, getResources().getDisplayMetrics());
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = size;
        params.width = size;
        // Don't forget to set the radius
        radius = size;
        setLayoutParams(params);
    }

    public ColorBox(Context context, int color) {
        super(context);
    }


    /**
     * Override onDraw to round bitmap before render it
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap roundBitmap = getCroppedBitmap(this.bitmap, this.radius);
        canvas.drawBitmap(roundBitmap, 0, 0, null);
    }


    /**
     * Methode from
     * http://stackoverflow.com/questions/16208365/how-to-create-a-circular-imageview-in-android
     * That make a round bitmap from a square one
     * @param bmp
     * @param radius
     * @return
     */
    private Bitmap getCroppedBitmap(Bitmap bmp, int radius) {
        Bitmap sbmp;

        if (bmp.getWidth() != radius || bmp.getHeight() != radius) {
            float smallest = Math.min(bmp.getWidth(), bmp.getHeight());
            float factor = smallest / radius;
            sbmp = Bitmap.createScaledBitmap(bmp,
                    (int) (bmp.getWidth() / factor),
                    (int) (bmp.getHeight() / factor), false);
        } else {
            sbmp = bmp;
        }

        Bitmap output = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final String color = "#BAB399";
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, radius, radius);

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor(color));
        canvas.drawCircle(radius / 2 + 0.7f, radius / 2 + 0.7f,
                radius / 2 + 0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(sbmp, rect, rect, paint);

        return output;
    }

    /**
     * Set the color of the Color box using a uniform bitmap
     * @param color
     */
    public void setColor(int color) {

        int R = (color >> 16) & 0xff;
        int G = (color >>  8) & 0xff;
        int B = (color      ) & 0xff;
        Bitmap bitmap = Bitmap.createBitmap(radius, radius, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                bitmap.setPixel(i, j, Color.argb(255, R, G, B));
            }
        }
        this.bitmap = bitmap;
    }
}
