package eu.trentorise.smartcampus.mobility.gamification.challenges;

/**
 * Plane point function
 */
public class PlanePointFunction {

    private int nrow;
    private int ncol;
    private Double min;
    private Double max;
    private Double intermediate;
    private Double matrix[][];
    private Double approximator;

    /**
     * Compute a plane point function (aka a matrix) of values with given size
     * (nrow and ncol) using min, max and intermediate for values. Uses
     * approximator to round matrix values to the closest value (i.e 183 => 180,
     * 199 => 200)
     */
    public PlanePointFunction(int nrow, int ncol, Double min, Double max,
                              Double intermediate, Double approximator) {
        if (min == 0 || max == 0 || min > max || max < min) {
            throw new IllegalArgumentException(
                    "Min and max must be not null and min more than max");
        }
        if (approximator <= 0) {
            throw new IllegalArgumentException(
                    "Approximator must be greater than zero");
        }
        if (approximator > min) {
            throw new IllegalArgumentException(
                    "Approximator must be greater than minimum: " + min);
        }
        this.nrow = nrow;
        this.ncol = ncol;
        this.min = min;
        this.max = max;
        this.intermediate = intermediate;
        this.approximator = approximator;
        // init
        this.matrix = new Double[nrow][ncol];
        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                matrix[i][j] = 0d;
            }
        }
        // build matrix
        calculate();
    }

    private void calculate() {
        double dh = (intermediate - min) / (double) (ncol - 1);
        double dv = (max - intermediate) / (double) (nrow - 1);

        for (int i = 0; i < nrow; i++) {
            for (int j = 0; j < ncol; j++) {
                matrix[i][j] = Math.round(Math.round(min + dh * j + i * dv)
                        / approximator)
                        * approximator;
            }
        }
    }

    public Double get(int x, int y) {
        return matrix[x][y];
    }

    public Double getMin() {
        return min;
    }

    public int getNrow() {
        return nrow;
    }

    public int getNcol() {
        return ncol;
    }

    public Double getMax() {
        return max;
    }

    public Double getIntermediate() {
        return intermediate;
    }

    public Double getTryOncePrize(int x, int y) {
        return matrix[x][y];
    }
}
