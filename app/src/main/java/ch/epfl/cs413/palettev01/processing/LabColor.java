package ch.epfl.cs413.palettev01.processing;


/**
 * Class describing a Lab triplet color with Luminance and a and b channels
 */
public class LabColor {

    /**
     * Luminance channel
     */
    private double L;

    /**
     * a channel
     */
    private double a;

    /**
     * b channel
     */
    private double b;


    /**
     * Constructor from an array
     *
     * @param Lab array of size 3 containing L, a and b value
     */
    public LabColor(double[] Lab) {
        if (Lab.length != 3) {
            throw new IllegalArgumentException("Lab color must have 3 float values");
        }
        L = Lab[0];
        a = Lab[1];
        b = Lab[2];
    }


    /**
     * Constructor from the 3 values
     *
     * @param L the luminance channel
     * @param a channel
     * @param b channel
     */
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

    /**
     * Get the Lab color as an array
     *
     * @return lab color
     */
    public double[] getLab() {
        double[] lab = new double[3];
        lab[0] = L;
        lab[1] = a;
        lab[2] = b;
        return lab;
    }

    /**
     * Get the luminance only
     *
     * @return luminance
     */
    public double getL() {
        return L;
    }

    /**
     * Set the luminance channel
     *
     * @param l new luminance value
     */
    public void setL(double l) {
        L = l;
    }

    /**
     * Get the a channel only
     *
     * @return luminance
     */
    public double getA() {
        return a;
    }

    /**
     * Set the a channel
     *
     * @param a new a value
     */
    public void setA(double a) {
        this.a = a;
    }

    /**
     * Get the b channel only
     *
     * @return luminance
     */
    public double getB() {
        return b;
    }

    /**
     * Set the b channel
     *
     * @param b new b value
     */
    public void setB(double b) {
        this.b = b;
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
