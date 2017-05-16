package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import ch.epfl.cs413.palettev01.R;

import static java.lang.Math.exp;
import static java.lang.Math.log;

public class PaletteAdapter extends BaseAdapter{

    public static final int PALETTE_MAX_SIZE = 6;
    public static final int PALETTE_MIN_SIZE = 3;
    public static final int PALETTE_SIZE = 4;

    private Context mContext;
    private int[] colors;
    private int[] tempColors;
    private int selectedBox = -1;
    private int size = 0;
    private int tempsize = 0;


    private boolean isEditingMode = false;


    public PaletteAdapter(Context c, int size) {

        mContext = c;

        if(size > PALETTE_MAX_SIZE){
            throw new IllegalArgumentException("Size argument must be integer smaller than 7");
        }
        this.size = size;
        this.tempsize = size;
        colors = new int[PALETTE_MAX_SIZE];
        tempColors = new int[PALETTE_MAX_SIZE];

        // initialization of the palette color . For now simply grayscale value
        for (int i = 0; i<size; i++) {
            colors[i] = Color.argb( 255, 255/(size-i), 255/(size-i), 255/(size-i));
            tempColors[i] = Color.argb( 255, 255/(size-i), 255/(size-i), 255/(size-i));
        }
    }

    public int getSize() {
        return isEditingMode ? tempsize : size;
    }

    /**
     * Set the palette element on position with newColor
     * And notify the modification for display
     * The modified element is the temporary one if the edit mode is enable
     * @param position
     * @param newColor
     */
    public void setColor(int position, int newColor){
        if (position >= size || position < 0){
            throw new IllegalArgumentException("Position out of bounds");
        }

        if(isEditingMode){
            tempColors[position] = newColor;
        }else {
            colors[position] = newColor;
        }
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
     * The returned element is the temporary one if editing mode is enable
     * @param position
     * @return one integer representing the color of the position element of the palette
     */
    public int getColor(int position) {

        if (position >= size) {
            throw new IllegalArgumentException("Position too big");
        }
        if (isEditingMode){
            return tempColors[position];
        }else {
            return colors[position];
        }
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


    /**
     *  Enable the palette editing mode
     */
    public void enableEditing(){
        isEditingMode = true;
        setSelectedBox(-1);
        initTempColors();
        this.notifyDataSetChanged();
    }

    /**
     * Disable the palette editing mode
     * And update the current color with temporary one if the user validate the temporary colors
     * @param isValidated
     */
    public void disableEditing(boolean isValidated){
        isEditingMode = false;
        setSelectedBox(-1);
        if(isValidated){
            defineTempColors();
        }
        this.notifyDataSetChanged();
    }
    /**
     * Init the temporary palette color with the current palette colors
     */
    private void initTempColors(){
        for (int i = 0; i<size; i++) {
            tempColors[i] = colors[i];
            tempsize = size;
        }
    }
    /**
     * update the current color with temporary one
     */
    private void defineTempColors(){
        for (int i = 0; i<tempsize; i++) {
            colors[i] = tempColors[i];
            size = tempsize;
        }
    }

    /**
     * Check if the palette editing mode is enable
     */
    public boolean isEditing(){
        return isEditingMode;
    }

    public void addColor(int color){

        if(tempsize < PALETTE_MAX_SIZE) {
            tempColors[tempsize] = color;
            tempsize++;
            this.notifyDataSetChanged();
        }
    }

    public void removeColor(int position){

        if(tempsize > PALETTE_MIN_SIZE) {

            for (int i = position; i < tempsize-1; i++){
                tempColors[i] = tempColors[i+1];
            }
            tempsize--;
            this.notifyDataSetChanged();
        }
    }



    @Override
    public int getCount() {

        if(isEditingMode){
            return tempsize+2;
        }
        return size;
    }

    @Override
    public Object getItem(int position) {

        if(isEditingMode && position < tempsize){
            return tempColors[position];
        }
        else if(!isEditingMode && position < size )
            return colors[position];
        else
            return 0;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View grid = new View(mContext);
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int size = isEditingMode ? this.tempsize : this.size;

        if(position < size) {

            grid = inflater.inflate(R.layout.color_box, null);

            ColorBox button = (ColorBox) grid.findViewById(R.id.grid_color_box);

            if (position == selectedBox) {
                button.setSelected();
            } else {
                button.setNotSelected();
            }

            // If the palette is in editing mode, the temporary colors are shown
            if (isEditingMode) {
                button.setColor(tempColors[position]);

            } else {
                button.setColor(colors[position]);
            }
        }
        else if (position == size && isEditingMode && size < PALETTE_MAX_SIZE){
            grid = inflater.inflate(R.layout.plus_box, null);
        }
        else if (position == size+1 && isEditingMode){
            grid = inflater.inflate(R.layout.magic_box, null);
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
                old_lab[0] = new_lab[0] - smoothL(delta, old_lab_pos[0]-old_lab[0]);
                newColor = ColorUtils.LABToColor(old_lab[0], old_lab[1], old_lab[2]);
            } else if (i > position) {
                double[] old_lab = new double[3];
                ColorUtils.colorToLAB(getColor(i), old_lab);
                old_lab[0] = new_lab[0] + smoothL(-delta, old_lab[0]-old_lab_pos[0]);
                newColor = ColorUtils.LABToColor(old_lab[0], old_lab[1], old_lab[2]);
            }

            /// TODO : We don't check if palette color goes out of bounds !

            // If i==position we just want to return the color
            setColor(i, newColor);
        }
    }
}