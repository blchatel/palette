package ch.epfl.cs413.palettev01.processing;

import android.graphics.Color;

/**
 * Created by joachim on 3/25/17.
 */
public class RGBColor {
    RGBColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }
    RGBColor(int[] rgb) {
        if (rgb.length != 3) {
            throw new IllegalArgumentException("RGB color must have 3 int values");
        }
        r = rgb[0];
        g = rgb[1];
        b = rgb[2];
    }
    int r;
    int g;
    int b;

    static RGBColor intToRGB(int p) {
        int[] rgb = new int[3];
        rgb[0] = Color.red(p);
        rgb[1] = Color.green(p);
        rgb[2] = Color.blue(p);

        return new RGBColor(rgb);
    }

    float[] getRGB () {
        float[] rgb = new float[3];
        rgb[0] = (float)r;
        rgb[1] = (float)g;
        rgb[2] = (float)b;

        return rgb;
    }

    public void add (RGBColor c) {
        r = r+c.r;
        g = g+c.g;
        b = b+c.b;
    }

    public void divide(int div) {
        r = r/div;
        g = g/div;
        b = b/div;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RGBColor rgbColor = (RGBColor) o;

        if (r != rgbColor.r) return false;
        if (g != rgbColor.g) return false;
        return b == rgbColor.b;

    }

    @Override
    public int hashCode() {
        int result = r;
        result = 31 * result + g;
        result = 31 * result + b;
        return result;
    }
}
