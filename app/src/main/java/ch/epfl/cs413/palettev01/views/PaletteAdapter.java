package ch.epfl.cs413.palettev01.views;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.epfl.cs413.palettev01.R;
import ch.epfl.cs413.palettev01.processing.LabColor;

import static java.lang.Math.exp;
import static java.lang.Math.log;

/**
 *
 *
 */
public class PaletteAdapter extends BaseAdapter{

    /**
     * Maximum amount of color in a palette
     * paper talk 6 or 7 is a maximum to deal with correctly
     */
    public static final int PALETTE_MAX_SIZE = 6;

    /**
     * Minimum amount of color in a palette
     * paper talk 3 is a minimum to get something interesting
     */
    public static final int PALETTE_MIN_SIZE = 3;

    /**
     * Default number of colors in a palette when init
     */
    public static final int PALETTE_SIZE = 4;

    /**
     * Context of the palette
     */
    private Context mContext;

    /**
     * Array of the palette colors as integer.
     * Color used in the main mode
     */
    private int[] colors;

    /**
     * Array of the palette temporary colors as integer
     * These colors are used in edit mode
     */
    private int[] tempColors;

    /**
     * Index of the selected color box for different drawing
     * and know where redirect palette modification
     * value -1 if no color is selected
     */
    private int selectedBox = -1;

    /**
     * Size of the main palette. Should by between PALETTE_MIN_SIZE and PALETTE_MAX_SIZE
     */
    private int size = 0;

    /**
     * Size of the temporary palette. Should by between PALETTE_MIN_SIZE and PALETTE_MAX_SIZE
     */
    private int tempsize = 0;


    /**
     * Boolean flag indicating if the editing palette mode is enable or not
     * true : edit mode enabled
     * false : main mode enabled
     */
    private boolean isEditingMode = false;


    /**
     * Palette adapter constructor
     * @param c
     * @param size
     */
    public PaletteAdapter(Context c, int size) {

        mContext = c;

        // Check whether the init palette size is correct. If not write an error message
        // and set the size value with the default size
        if(size > PALETTE_MAX_SIZE || size < PALETTE_MIN_SIZE){
            Log.d("ERROR", "Palette size must be between "+PALETTE_MIN_SIZE+ " and "+PALETTE_MAX_SIZE+" size will be set to "+PALETTE_SIZE);
            size = PALETTE_SIZE;
        }
        this.size = size;
        this.tempsize = size;

        // Init array with maximum color size. It does not take too much place...only a few integers.
        colors = new int[PALETTE_MAX_SIZE];
        tempColors = new int[PALETTE_MAX_SIZE];

        // initialization of the palette color . For now simply grayscale value : waiting for image
        for (int i = 0; i<size; i++) {
            colors[i] = Color.argb( 255, 255/(size-i), 255/(size-i), 255/(size-i));
            tempColors[i] = Color.argb( 255, 255/(size-i), 255/(size-i), 255/(size-i));
        }
    }

    /**
     * Get the palette size
     * @return the size of the palette of the current mode
     */
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
        if(isEditingMode){
            if (position >= tempsize || position < 0){
                throw new IllegalArgumentException("Position out of bounds");
            }
            tempColors[position] = newColor;
        } else {
            if (position >= size || position < 0){
                throw new IllegalArgumentException("Position out of bounds");
            }
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
        if (isEditingMode){
            if (position >= tempsize) {
                throw new IllegalArgumentException("Position too big for getting color");
            }
            return tempColors[position];
        }else {
            if (position >= size) {
                throw new IllegalArgumentException("Position too big for getting color");
            }
            return colors[position];
        }
    }


    /**
     * Set the selected box
     * Because you cannot select twice the same box. if the current selected box and the
     * new one are the same, it simply deselect it.
     * @param position
     */
    public void setSelectedBox(int position){
        selectedBox = selectedBox == position ? -1 : position;
        this.notifyDataSetChanged();
    }


    /**
     * Test if one box is selected
     * @return boolean
     */
    public boolean isBoxSelected() {
        return selectedBox > -1;
    }


    /**
     *  Enable the palette editing mode by init temporary palette color with the current one
     *  and deselecting all element
     *  notify the modification for display
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
     * changes.
     * notify the modification for display
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
        /// Sorting the palette by luminosity to be sure it is sorted as desired
        List<LabColor> colorsList = new ArrayList<>();
        for (int i = 0; i < tempsize; i++) {
            int color = tempColors[i];
            double[] lab_color = new double[3];
            ColorUtils.colorToLAB(color, lab_color);
            colorsList.add(new LabColor(lab_color));
        }

        // We sort the palette by luminance
        Collections.sort(colorsList, new Comparator<LabColor>() {
            @Override
            public int compare(LabColor o1, LabColor o2) {
                return (o1.getL() > o2.getL()) ? 1 : (o1.getL() < o2.getL()) ? -1 : 0;
            }
        });

        size = tempsize;
        for (int i = 0; i < tempsize; i++) {
            double[] lab = colorsList.get(i).getLab();

            colors[i] = ColorUtils.LABToColor(lab[0], lab[1], lab[2]);
        }
    }

    /**
     * Check if the palette editing mode is enable
     * @return true if the editing mode is enabled and false otherwise
     */
    public boolean isEditing(){
        return isEditingMode;
    }


    /**
     * Add a new palette color if the current size allows it
     * You must be in edit mode to perform this action
     * @param color the color to add
     */
    public void addColor(int color){

        if(isEditingMode && tempsize < PALETTE_MAX_SIZE) {
            tempColors[tempsize] = color;
            tempsize++;
            this.notifyDataSetChanged();
        }
    }

    public void addColorContainer(){
        if(isEditingMode && tempsize < PALETTE_MAX_SIZE) {
            tempsize++;
        }
    }

    /**
     * Remove the palette color at position if the current size allow it
     * You must be in edit mode to perform this action
     * @param position
     */
    public void removeColor(int position){

        if(isEditingMode && tempsize > PALETTE_MIN_SIZE) {

            for (int i = position; i < tempsize-1; i++){
                tempColors[i] = tempColors[i+1];
            }
            tempsize--;
            this.notifyDataSetChanged();
        }
    }

    public void removeColorContainer(int position){
        if (position < tempsize) {
            if (isEditingMode && tempsize > PALETTE_MIN_SIZE) {
                tempsize--;
            }
        }
    }



    @Override
    public int getCount() {

        if(isEditingMode){
            // Add 2 because of the two tools (add color button and magic extraction button)
//            return tempsize + 1;
            // TODO: Uncomment if you want the magic button
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
        // TODO: Uncomment if you want magic button
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