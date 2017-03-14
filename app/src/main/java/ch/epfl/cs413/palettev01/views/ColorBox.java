package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;


/**
 * Created by bastien on 14.03.17.
 */
public class ColorBox extends AppCompatImageView{

    public ColorBox(Context context) {
        super(context);
    }

    public ColorBox(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColorBox(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    public ColorBox(Context context, int color) {
        super(context);
    }


}
