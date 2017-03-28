package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ch.epfl.cs413.palettev01.R;

public class PaletteAdapter extends BaseAdapter{

    public static final int PALETTE_SIZE = 5;
    private Context mContext;
    private int[] colors = new int[7];
    private int selectedBox = -1;
    private int size = 0;


    public PaletteAdapter(Context c, int size ) {

        mContext = c;

        if(size > 7){
            throw new IllegalArgumentException("Size argument must be integer smaller than 8");
        }
        this.size = size;

        // initialization of the palette color . For now simply grayscale value
        for (int i = 0; i<size; i++) {
            colors[i] = Color.argb( 255, 255/(i+1), 255/(i+1), 255/(i+1));
        }
    }


    /**
     * Set the palette element on position with newColor
     * And notify the modification for display
     * @param position
     * @param newColor
     */
    public void setColor(int position, int newColor){

        if (position >= size){
            throw new IllegalArgumentException("Position too big");
        }
        colors[position] = newColor;
        this.notifyDataSetChanged();
    }


    /**
     * Set color of the selected item of the palette with newColor
     * if one item is indeed selected. Do nothing otherwise
     * And notify the modification for display
     * @param newColor
     */
    public void setColor(int newColor){

        if (selectedBox != -1){
            colors[selectedBox] = newColor;
            this.notifyDataSetChanged();
        }
    }




    /**
     * Get the palette element Color on position
     * @param position
     * @return one integer representing the color of the position element of the palette
     */
    public int getColor(int position) {

        if (position >= size){
            throw new IllegalArgumentException("Position too big");
        }
        return colors[position];
    }






    /**
     * Set the selected box
     * @param position
     */
    public void setSelectedBox(int position){

        selectedBox = selectedBox == position ? -1 : position;
        this.notifyDataSetChanged();
    }


    /**
     * Test if a box is selected
     * @return boolean
     */
    public boolean isBoxSelected() {
        return selectedBox > -1;
    }

    @Override
    public int getCount() {
        return size;
    }

    @Override
    public Object getItem(int position) {
        return colors[position];
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        // TODO I don't know how to implement this function
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View grid;
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        grid = new View(mContext);
        grid = inflater.inflate(R.layout.color_box, null);
//        TextView textView = (TextView) grid.findViewById(R.id.grid_text);
        ColorBox button = (ColorBox) grid.findViewById(R.id.grid_color_box);
//        textView.setText("color#"+position);
        button.setBackgroundColor(colors[position]);

        //TODO : Find a nicer way without printing colors name
//        if(position == selectedBox){
//            textView.setTextColor(Color.RED);
//        } else {
//            textView.setTextColor(Color.BLACK);
//        }

        return grid;
    }
}