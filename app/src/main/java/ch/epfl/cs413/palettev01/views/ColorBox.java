package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;


/**
 * Created by bastien on 14.03.17.
 */
public class ColorBox extends AppCompatImageView {
    private final int normalSize = 45;
    private final int selectedSize = 55;

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
        setLayoutParams(params);
    }

    public void setNotSelected() {
        int size = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, normalSize, getResources().getDisplayMetrics());
        ViewGroup.LayoutParams params = getLayoutParams();
        params.height = size;
        params.width = size;
        setLayoutParams(params);
    }

    public ColorBox(Context context, int color) {
        super(context);
    }


}
