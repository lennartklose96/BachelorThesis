import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class LarsAlg {

    // Find the largest absolute value in a matrix
    private static int findLargestAbsValueFull(int[][] matrix) {
        int largest = 0;
        for (int[] row : matrix) {
            for (int entry : row) {
                largest = Math.max(largest, Math.abs(entry));
            }
        }
        return largest;
    }
    /////////////////////////
    /// UTILITY FUNCTIONS ///
    /////////////////////////

    // Find the largest absolute value in an ILP matrix, given r and h
    private static int findLargestAbsValue(int[][] matrix, int r, int h) {
        int largest = 1;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < h; j++) {
                largest = Math.max(largest, Math.abs(matrix[i][j]));
            }
        }
        return largest;
    }

    // Returns the maximum absolute value in a vector
    public static int maxAbs(int[] v) {
        int max = 0;
        for (int x : v) {
            max = Math.max(max, Math.abs(x));
        }
        return max;
    }

    public static double maxAbs(double[] v) {
        double max = 0;
        for (double x : v) {
            max = Math.max(max, Math.abs(x));
        }
        return max;
    }

    // Transposes a given matrix
    private static int[][] transpose(int[][] matrix) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        int[][] result = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[j][i] = matrix[i][j];
            }
        }
        return result;
    }

    // Multiplies a matrix with a given vector
    public static double[] matrixVectorMult(double[][] matrix, double[] vector) {
        int rows = matrix.length;
        int cols = matrix[0].length;
        double[] result = new double[rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i] += matrix[i][j] * vector[j];
            }
        }
        return result;
    }

    ////////////////////////
    /// ENCODING VECTORS ///
    ////////////////////////

    // Encodes an index/decimal number of the given base as a vector of length r
    // Used to save a lot of memory down the line
    private static void encode(int[] out, long value, int base, int r) {
        for (int i = r - 1; i >= 0; i--) {
            out[i] = (int) value % base;
            value /= base;
        }
    }
    // Decodes a base-'base' vector into a single integer
    private static long decode(int[] v, int base) {
        long value = 0;
        for (int j : v) {
            value = value * base + j;
        }
        return value;
    }

    ////////////////////////////////
    /// MAIN VECTOR CALCULATIONS ///
    ////////////////////////////////











    ///////////////////////////
    /// FEASIBILITY CHECKER ///
    ///////////////////////////

    public static boolean isFeasible(int[][] A, int[] rhs, int[] t, int r, int h) {

        // Largest value in matrix
        int delta = findLargestAbsValue(A, r, h);
        // Upper bound for the hereditary discrepancy
        int herDisc = (int) Math.ceil(6 * Math.sqrt((double) h) * delta);
        // Vector length for b/rhs
        int m = rhs.length;
        // K for n-fold 
        int K = 0;
        for (int i = r; i < m; i++) {
            K += rhs[i];
        }
        System.out.printf("Value of K: %d%n", K);
        // Getting the number of iterations
        int iterations = (int) Math.ceil(Math.log(K) / Math.log(6.0 / 5.0));
        System.out.printf("Iterations: %d%n", iterations);

        // The maximum amount of vectors we can check
        // Equal to 8H + 1
        int base = 8 * herDisc + 1;
        System.out.printf("Base is %d%n", base);
        // Columns decoded to a number of base 8H+1
        long[] cols = new long[h];
        for (int j = 0; j < A[0].length; j++) {
            int[] col = new int[A.length];
            for (int i = 0; i < A.length; i++) {
                col[i] = A[i][j];
            }
            cols[j] = decode(col, base);
        }
        // First iteration of vectors
        long[] initialVectors = new long[cols.length + 1];
        initialVectors[0] = 0;
        int[] v = new int[m];
        System.arraycopy(cols, 0, initialVectors, 1, cols.length);
        for (long vector : initialVectors) {
            encode(v, vector, base, m);
            // System.out.println(Arrays.toString(v));
        }

        // Store the previous and current iteration of the dynamic table
        List<Long> current = new ArrayList<>();
        List<Long> prev = new ArrayList<>();
        // Initialize previous list
        for (long vector : initialVectors) {
            prev.add(vector);
        }

        for (int i = 1; i < iterations; i++) {
            continue;
        }


        return false;
    }

    public static void main(String[] args) throws IOException {

        // long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_debug.txt");
        // Read instances
        int count = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int[] c = i.getC();
            int r = i.getR();
            int h = i.getH();
            // Get the result
            boolean result = isFeasible(matrix, rhs, t, r, h);
            // TODO: Remove
            break;
        }

        // long finish = System.currentTimeMillis();
        // long timeElapsed = finish - start;
        // System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}
