package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import ch.epfl.cs413.palettev01.R;

public class PaletteAdapter extends BaseAdapter{

    private Context mContext;
    private int[] colors = new int[7];
    private int size = 0;

    public PaletteAdapter(Context c, int size ) {
        mContext = c;

        if(size > 7){
            throw new IllegalArgumentException("Size argument must be integer smaller than 8");
        }
        this.size = size;

        for (int i = 0; i<size; i++) {
            colors[i] = Color.argb( 255, 255/(i+1), 255/(i+1), 255/(i+1));
        }
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return size;
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View grid;
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {

            grid = new View(mContext);
            grid = inflater.inflate(R.layout.color_box, null);
            TextView textView = (TextView) grid.findViewById(R.id.grid_text);
            ColorBox button = (ColorBox) grid.findViewById(R.id.grid_color_box);
            textView.setText("color#"+position);
            button.setBackgroundColor(colors[position]);
        } else {
            grid = (View) convertView;
        }
        return grid;
    }
}