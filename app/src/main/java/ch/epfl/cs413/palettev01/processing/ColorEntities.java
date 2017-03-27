package ch.epfl.cs413.palettev01.processing;

import android.graphics.Color;
import android.util.Log;

import java.util.Random;

/**
 * Created by joachim on 3/25/17.
 */
class RGBColor {
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
//        rgb[0] = (p & 0xff0000) >> 16;
//        rgb[1] = (p & 0xff00) >> 8;
//        rgb[2] = p & 0xff;

//        Log.d("<RGB>", "R is " + rgb[0] + " G : " + rgb[1] + " B : " + rgb[2]);

        rgb[0] = Color.red(p);
        rgb[1] = Color.green(p);
        rgb[2] = Color.blue(p);

        return new RGBColor(rgb);
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
}

class LabColor {

    double L;
    double a;
    double b;

    LabColor(double[] Lab) {
        if (Lab.length != 3) {
            throw new IllegalArgumentException("Lab color must have 3 float values");
        }
        L = Lab[0];
        a = Lab[1];
        b = Lab[2];
    }
    LabColor (double L, double a, double b) {
        this.L = L;
        this.a = a;
        this.b = b;
    }

    /**
     * Compute the square lab distance -> More efficient than doing the sqrt
     *
     * @param otherColor Color to compare with
     *
     * @return the squared distance as a float between this and otherColor
     */
    double squareDistanceTo (LabColor otherColor) {
        return (L-otherColor.L)*(L-otherColor.L) + (a-otherColor.a)*(a-otherColor.a) + (b-otherColor.b)*(b-otherColor.b);
    }


    /// --------------------------------------------------- ///
    /// ---             Static functions                --- ///
    /// --------------------------------------------------- ///

    /**
     * Random color generator
     *
     * @return a random color
     */
    final static LabColor generateRandom() {
        float rL, rA, rB;
        Random r = new Random();
        rL = r.nextFloat() * 100;
        rA = r.nextFloat() * 200 - 100;
        rB = r.nextFloat() * 200 - 100;

        return new LabColor(rL, rA, rB);
    }

    public LabColor addColor(LabColor c) {
        return new LabColor(L+c.L, a+c.a, b+c.b);
    }

    public LabColor divide(int div) {
        return new LabColor(L/div, a/div, b/div);
    }
}