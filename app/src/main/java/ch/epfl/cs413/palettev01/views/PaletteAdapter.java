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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.epfl.cs413.palettev01.R;
import ch.epfl.cs413.palettev01.processing.LabColor;

import static java.lang.Math.exp;
import static java.lang.Math.log;

/**
 *  This is an adapter for the palette list of colors used to change the palette
 */
public class PaletteAdapter extends BaseAdapter{

    private static final boolean DEBUG_LOG = false;

    /**
     * Maximum amount of color in a palette
     */
    private static final int PALETTE_MAX_SIZE = 6;


    /**
     * Minimum amount of color in a palette
     */
    public static final int PALETTE_MIN_SIZE = 3;


    /**
     * Default number of colors in a palette at start
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
     * Array of the temporary palette colors as integer
     * Theses colors are used in edit mode
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
     * Boolean flag indicating if a color has been changed to pop the magic tool for extracting
     * the palette with kmeans
     */
    private boolean colorManuallyChanged;


    /**
     * Boolean flag indicating if the editing palette mode is enable or not
     * true : edit mode enabled
     * false : main mode enabled
     */
    private boolean isEditingMode = false;


    /**
     * Setter for @colorManuallyChanged
     * @param colorManuallyChanged a boolean
     */
    public void setColorManuallyChanged(boolean colorManuallyChanged) {
        this.colorManuallyChanged = colorManuallyChanged;
    }


    /**
     * Getter
     * @return @colorManuallyChanged
     */
    public boolean isColorManuallyChanged () {
        return colorManuallyChanged;
    }


    /**
     * Palette adapter constructor
     * @param c the context
     * @param size the palette size
     */
    public PaletteAdapter(Context c, int size) {
        mContext = c;
        colorManuallyChanged = false;

        // Check whether the init palette size is correct. If not write an error message
        // and set the size value with the default size
        if(size > PALETTE_MAX_SIZE || size < PALETTE_MIN_SIZE){
            Log.d("ERROR", "Palette size must be between "+PALETTE_MIN_SIZE+ " and "+PALETTE_MAX_SIZE+" size will be set to "+PALETTE_SIZE);
            size = PALETTE_SIZE;
        }
        this.size = size;
        this.tempsize = size;

        // Init array with maximum color size. It does not take too much place...only a few integers
        colors = new int[PALETTE_MAX_SIZE];
        tempColors = new int[PALETTE_MAX_SIZE];

        // Initialization of the palette color to gray scale colors while waiting for image
        for (int i = 0; i<size; i++) {
            colors[i] = Color.argb( 255, 255/(size-i), 255/(size-i), 255/(size-i));
            tempColors[i] = Color.argb( 255, 255/(size-i), 255/(size-i), 255/(size-i));
        }
    }


    /**
     * Get the palette size
     * @return palette size of the current mode
     */
    public int getSize() {
        return isEditingMode ? tempsize : size;
    }


    /**
     * Set the palette element on position with newColor
     * And notify the modification for display
     * The modified element is the temporary one if the edit mode is enable
     * @param position the position to set
     * @param newColor the new color for the item at position
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
     * @param newColor the new color for the selected item
     */
    public void setColor(int newColor){
        colorManuallyChanged = true;
        if (selectedBox != -1){
            setColor(selectedBox, newColor);
        }
    }


    /**
     * Get the palette element Color on position
     * The returned element is the temporary one if editing mode is enable
     * @param position to get
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
     * @param position of the box to select
     */
    public void setSelectedBox(int position){
        selectedBox = selectedBox == position ? -1 : position;
        this.notifyDataSetChanged();
    }


    /**
     * Test if one box is selected
     * @return if any box is selected
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
        colorManuallyChanged = false;
        this.notifyDataSetChanged();
    }

    /**
     * Disable the palette editing mode
     * And update the current color with temporary one if the user validate the temporary colors
     * changes.
     * notify the modification for display
     * @param isValidated a boolean that indicate if the user leaves the config mode by validation
     *                    or cancel
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
     * Add a new ColorContainer
     */
    public void addColorContainer(){
        colorManuallyChanged = false;
        if(isEditingMode && tempsize < PALETTE_MAX_SIZE) {
            tempsize++;
        }
    }


    /**
     * Remove the palette color at given position
     *
     * @param position  index to remove
     */
    public void removeColorContainer(int position){
        colorManuallyChanged = false;
        if (position < tempsize) {
            if (isEditingMode && tempsize > PALETTE_MIN_SIZE) {
                tempsize--;
            }
        }
    }


    /**
     * Get the number of elements in the adapter
     *
     * @return adapter size != paletteSize
     */
    @Override
    public int getCount() {

        if(isEditingMode){
            // Add 2 because of the two tools (add color button and magic extraction button)
            if (colorManuallyChanged) {
                return tempsize + 2;
            } else {
                return tempsize + 1;
            }

        }
        return size;
    }


    /**
     * Get the color at given position
     *
     * @param position of the color to return
     * @return Color of the given position
     */
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


    /**
     * Unused :  This function is unused in this program
     * @param position a unused parameter
     * @return and so return 0 by definition
     */
    @Override
    public long getItemId(int position) {
        return 0;
    }


    /**
     * Get the view of the given position of the palette
     *
     * @param position we want get the view
     * @param convertView NOT USED
     * @param parent NOT USED
     * @return a view as a list of elements
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View listElem = new View(mContext);
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


        int size = isEditingMode ? this.tempsize : this.size;

        // We check if we really are in the good range
        if(position < size && position >= 0) {

            listElem = inflater.inflate(R.layout.color_box, null);

            ColorBox button = (ColorBox) listElem.findViewById(R.id.grid_color_box);

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
            listElem = inflater.inflate(R.layout.plus_box, null);
        }
        else if (position == size+1 && isEditingMode && colorManuallyChanged){
            listElem = inflater.inflate(R.layout.magic_box, null);
        }

        return listElem;
    }


    /**
     * A function to get a smooth luminance value for palette updating
     *
     * @param x
     * @param d
     * @return smooth extrapolated value
     */
    private double smoothL (double x, double d) {
        double lambda = 0.2 * log(2.0);
        double toRet = (log(exp(lambda*x) + exp(lambda*d) - 1.0) / lambda);
        return toRet - x;
    }


    /**
     * Update all luminance values of the palette colors
     *
     * @param position pivot position
     * @param color to update with
     */
    public boolean updateAll(int position, int color) {
        double[] new_lab = new double[3];
        ColorUtils.colorToLAB(color, new_lab);
        double[] old_lab_pos = new double[3];
        ColorUtils.colorToLAB(getColor(position), old_lab_pos);
        double delta = old_lab_pos[0] - new_lab[0];

        // The colors are sorted by luminance so i < pos => luminance is lower and vice-versa
        for (int i = 0; i < size; i++) {
            double[] old_lab = new double[3];
            int newColor;
            if (i < position) { // Palette colors with smaller luminance - DARKER
                ColorUtils.colorToLAB(getColor(i), old_lab);
                old_lab[0] = ((int)((new_lab[0] - smoothL(delta, old_lab_pos[0]-old_lab[0]))*10))/10.0;

                if(DEBUG_LOG) {
                    Log.d("PaletteColor", "Dark Palette " + i + " luminance is : " + old_lab[0]);
                }

            } else if (i > position) {
                ColorUtils.colorToLAB(getColor(i), old_lab);
                old_lab[0] = ((int)((new_lab[0] + smoothL(-delta, old_lab[0]-old_lab_pos[0]))*10))/10.0;

                if(DEBUG_LOG) {
                    Log.d("PaletteColor", "Light Palette " + i + " luminance is : " + old_lab[0]);
                }
            }
            else {
                old_lab[0] = ((int)(new_lab[0]*10)) / 10.0;
                old_lab[1] = new_lab[1];
                old_lab[2] = new_lab[2];
                if(DEBUG_LOG){
                    Log.d("PaletteColor", "Same Palette " + i + " luminance is : " + old_lab[0]);
                }
            }

            newColor = ColorUtils.LABToColor(old_lab[0], old_lab[1], old_lab[2]);

            if (Double.isNaN(old_lab[0])) {
                return false;
            }

            // We update the displayed color
            setColor(i, newColor);
        }

        return true;
    }
}
