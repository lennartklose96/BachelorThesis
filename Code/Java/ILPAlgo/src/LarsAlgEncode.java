import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LarsAlgEncode {

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

    // Adds two int vectors
    static int[] add(int[] a, int[] b) {
        int[] res = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            res[i] = a[i] + b[i];
        }
        return res;
    }

    ////////////////////////////////
    /// MAIN VECTOR CALCULATIONS ///
    ////////////////////////////////

    private static boolean isInBounds(int[] bPrime, int[] b, int i, int l, int herDisc) {
        long scalePrime = 1L << l;
        long scaleB = 1L << i;
        long bound = 4L * herDisc * scalePrime;
        int idx = 0;
        boolean inBounds = true;
        // Checking component wise bounds
        long diff;
        while (idx < b.length && inBounds) {
            diff = Math.abs((long) bPrime[idx] * scalePrime - (long) b[idx] * scaleB);
            // System.out.println(diff);
            inBounds = diff <= bound;
            idx++;
        }
        return inBounds;
    }

    // Check if a given vector is in a list
    private static boolean containsVector(Set<Long> set, int[] toFind, int m, int base) {
        int[] v = new int[m];
        for (long value: set) {
            encode(v, value, base, m);
            if (Arrays.equals(v, toFind)) {
                return true;
            }
        }
        return false;
    }

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
        int l = (int) Math.ceil(Math.log(K) / Math.log(6.0 / 5.0));
        System.out.printf("Iterations: %d%n", l);

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
        System.arraycopy(cols, 0, initialVectors, 1, cols.length);

        // Store the previous and current iteration of the dynamic table
        Set<Long> prev = new HashSet<>();
        Set<Long> current = new HashSet<>();
        // Initialize first iteration
        int[] v = new int[m];
        for (long vector : initialVectors) {
            encode(v, vector, base, m);
            System.out.println(Arrays.toString(v));
            if (isInBounds(v, rhs, 0, l, herDisc)) {
                prev.add(vector);
            }
        }
        // Main build of the work
        // Builds the dynamic table
        int[] bPrime = new int[m];
        int[] bDoublePrime  = new int[m];
        int[] bSum;
        for (int i = 1; i <= l; i++) {
            System.out.println(i);
            // Iterating over every possible combination of vectors b
            for (Long a : prev) {
                encode(bPrime, a, base, m);
                for (Long b : prev) {
                    encode(bDoublePrime, b, base, m);
                    // Checking in bounds condition
                    bSum = add(bPrime, bDoublePrime);
                    if (isInBounds(bSum, rhs, i, l, herDisc)) {
                        current.add(decode(bSum, base));
                    }
                }
            }
            // Swap and free memory
            Set<Long> tmp = prev;
            prev = current;
            current = new HashSet<>();

            System.out.println((prev.size()));
        }
        return prev.contains(decode(rhs, base));
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
            System.out.println(result);
            // TODO: Remove
            break;
        }
        // long finish = System.currentTimeMillis();
        // long timeElapsed = finish - start;
        // System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}
