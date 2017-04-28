package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import ch.epfl.cs413.palettev01.R;
import ch.epfl.cs413.palettev01.processing.LabColor;

import static java.lang.Math.exp;
import static java.lang.Math.log;

public class PaletteAdapter extends BaseAdapter{

    public static final int PALETTE_SIZE = 5;
    private Context mContext;
    private int[] colors;
    private int selectedBox = -1;
    private int size = 0;


    public PaletteAdapter(Context c, int size) {

        mContext = c;

        if(size > 7){
            throw new IllegalArgumentException("Size argument must be integer smaller than 8");
        }
        this.size = size;
        colors = new int[size];

        // initialization of the palette color . For now simply grayscale value
        for (int i = 0; i<size; i++) {
            colors[i] = Color.argb( 255, 255/(i+1), 255/(i+1), 255/(i+1));
        }
    }

    public int getSize() {
        return size;
    }

    /**
     * Set the palette element on position with newColor
     * And notify the modification for display
     * @param position
     * @param newColor
     */
    public void setColor(int position, int newColor){
        if (position >= size || position < 0){
            throw new IllegalArgumentException("Position out of bounds");
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
            setColor(selectedBox, newColor);
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

        grid = inflater.inflate(R.layout.color_box, null);
        ColorBox button = (ColorBox) grid.findViewById(R.id.grid_color_box);
        ColorBox activate = (ColorBox) grid.findViewById(R.id.grid_box_activation);
        button.setBackgroundColor(colors[position]);

        if(position == selectedBox){
            activate.setBackgroundColor(Color.BLUE);
        } else {
            activate.setBackgroundColor(Color.TRANSPARENT);
        }

        return grid;
    }

    private double smoothL (double x, double d) {
        double lambda = 0.2 * log(2);
        return log(exp(lambda*x) + exp(lambda*d) - 1) / lambda - x;
    }

    /**
     * Luminance update on a color change
     *
     * @param position
     * @param color
     */
    public void updateAll(int position, int color) {
        double[] new_lab = new double[3];
        ColorUtils.colorToLAB(color, new_lab);
        double[] old_lab_pos = new double[3];
        ColorUtils.colorToLAB(getColor(position), old_lab_pos);
        double delta = old_lab_pos[0] - new_lab[0];
        for (int i = 0; i < size; i++) {
            int newColor = color;
            if (i < position) { // Palette colors with higher luminance
                double[] old_lab = new double[3];
                ColorUtils.colorToLAB(getColor(i), old_lab);
                new_lab[0] = new_lab[0] - smoothL(-delta, old_lab[0]-old_lab_pos[0]);
                newColor = ColorUtils.LABToColor(new_lab[0], old_lab[1], old_lab[2]);
            } else if (i > position) {
                double[] old_lab = new double[3];
                ColorUtils.colorToLAB(getColor(i), old_lab);
                new_lab[0] = smoothL(delta, old_lab_pos[0]-old_lab[0]);
                newColor = ColorUtils.LABToColor(new_lab[0], old_lab[1], old_lab[2]);
//                newColor = getColor(i);
            }

            // If i==position we just want to return the color

            setColor(i, newColor);
        }
    }

    // TODO: Maybie we need a rs here too in order to transform directly the palette colors when
    // TODO: changing one without having to recompute the kmeans

}