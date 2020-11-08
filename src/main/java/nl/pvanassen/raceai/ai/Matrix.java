package nl.pvanassen.raceai.ai;

import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.ThreadLocalRandom;

@ToString
public class Matrix {

    @Getter
    private final int rows;

    @Getter
    private final int cols;

    private final double[][] matrix;

    Matrix(int r, int c) {
        rows = r;
        cols = c;
        matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = (float)ThreadLocalRandom.current().nextDouble(-1, 1);
            }
        }
    }

    Matrix(double[][] matrix) {
        this.matrix = copy(matrix);
        this.rows = matrix.length;
        this.cols = matrix[0].length;
    }

    public double[][] getMatrix() {
        return copy(matrix);
    }

    public static double[][] copy(double[][] src) {
        int length = src.length;
        double[][] target = new double[length][src[0].length];
        for (int i = 0; i < length; i++) {
            System.arraycopy(src[i], 0, target[i], 0, src[i].length);
        }
        return target;
    }
//
//    public void output() {
//        for (int i = 0; i < rows; i++) {
//            for (int j = 0; j < cols; j++) {
//                PApplet.print(matrix[i][j] + " ");
//            }
//            PApplet.println();
//        }
//        PApplet.println();
//    }

    public Matrix dot(Matrix n) {
        double[][] matrix = createEmptyMatrix(rows, n.cols);

        if (cols == n.rows) {
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < n.cols; j++) {
                    float sum = 0;
                    for (int k = 0; k < cols; k++) {
                        sum += this.matrix[i][k] * n.matrix[k][j];
                    }
                    matrix[i][j] = sum;
                }
            }
        }
        return new Matrix(matrix);
    }

    public Matrix singleColumnMatrixFromArray(double[] arr) {
        double[][] matrix = createEmptyMatrix(arr.length, 1);
        for (int i = 0; i < arr.length; i++) {
            matrix[i][0] = arr[i];
        }
        return new Matrix(matrix);
    }

    public double[] toArray() {
        double[] arr = new double[rows * cols];
        for (int i = 0; i < rows; i++) {
            if (cols >= 0) {
                System.arraycopy(matrix[i], 0, arr, i * cols, cols);
            }
        }
        return arr;
    }

    public Matrix addBias() {
        double[][] matrix = createEmptyMatrix(rows + 1, 1);
        for (int i = 0; i < rows; i++) {
            matrix[i][0] = this.matrix[i][0];
        }
        matrix[rows][0] = 1;
        return new Matrix(matrix);
    }

    private double[][] createEmptyMatrix(int rows, int cols) {
        return new double[rows][cols];
    }

    public Matrix activate() {
        double[][] matrix = createEmptyMatrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = relu(this.matrix[i][j]);
            }
        }
        return new Matrix(matrix);
    }

    public double relu(double x) {
        return Math.max(0, x);
    }

    public Matrix crossoverAndMutate(Matrix partner, float mutationRate) {
        double[][]matrix = createEmptyMatrix(rows, cols);

        int randC = ThreadLocalRandom.current().nextInt(cols);
        int randR = ThreadLocalRandom.current().nextInt(rows);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double rand = ThreadLocalRandom.current().nextFloat();
                if (rand < mutationRate) {
                    matrix[i][j] += ThreadLocalRandom.current().nextGaussian() / 5;

                    if (matrix[i][j] > 1) {
                        matrix[i][j] = 1;
                    }
                    if (matrix[i][j] < -1) {
                        matrix[i][j] = -1;
                    }
                }
                else {
                    if ((i < randR) || (i == randR && j <= randC)) {
                        matrix[i][j] = this.matrix[i][j];
                    } else {
                        matrix[i][j] = partner.matrix[i][j];
                    }
                }
            }
        }
        return new Matrix(matrix);
    }

    public Matrix copy() {
        double[][]matrix = createEmptyMatrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            if (cols >= 0) {
                System.arraycopy(this.matrix[i], 0, matrix[i], 0, cols);
            }
        }
        return new Matrix(matrix);
    }
}
