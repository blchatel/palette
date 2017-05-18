package ch.epfl.cs413.palettev01.processing;

import android.util.Pair;


public class LabColor {

    double L;
    double a;
    double b;

    public LabColor(double[] Lab) {
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

    /// ---------------------------------------------------- ///
    /// ---------  Getters and Setters functions  ---------- ///
    /// ---------------------------------------------------- ///

    public double[] getLab() {
        double[] lab = new double[3];
        lab[0] = L;
        lab[1] = a;
        lab[2] = b;
        return lab;
    }

    public double getL() {
        return L;
    }

    public void setL(double l) {
        L = l;
    }

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }


    /// --------------------------------------------------- ///
    /// ---             Static functions                --- ///
    /// --------------------------------------------------- ///

    public LabColor addColor(LabColor c) {
        return new LabColor(L+c.L, a+c.a, b+c.b);
    }

    public LabColor addColor(Pair<LabColor, Integer> pair) {
        return new LabColor(L+pair.first.L * pair.second, a+pair.first.a * pair.second, b+pair.first.b * pair.second);
    }

    public LabColor divide(int div) {
        return new LabColor(L/div, a/div, b/div);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        int precision = 1000;

        LabColor labColor = (LabColor) o;

        if ((int)(labColor.L*precision) != (int)(L*precision)) return false;
        if ((int)(labColor.a*precision) != (int)(a*precision)) return false;
        return (int)(labColor.b*precision) == (int)(b*precision);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(L);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(a);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(b);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "(" + L + ", " + a + ", " + b + ")";
    }
}
