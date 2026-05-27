import java.io.IOException;
import java.util.*;

public class LarsAlgVectors {


    /// //////////////////////
    /// UTILITY FUNCTIONS ///
    /// //////////////////////

    // Find the largest absolute value in a matrix
    private static int findLargestAbsValue(int[][] matrix) {
        int largest = 0;
        for (int[] row : matrix) {
            for (int entry : row) {
                largest = Math.max(largest, Math.abs(entry));
            }
        }
        return largest;
    }


    // Checks if a given vector is smaller than the other one
    private static boolean isSmallerComponentWise(int[] v, int[] target) {
        boolean smaller = true;
        int i = 0;
        while (smaller && i < v.length) {
            if (v[i] > target[i]) {
                smaller = false;
            }
            i++;
        }
        return smaller;
    }


    /// /////////////////////////////
    /// MAIN VECTOR CALCULATIONS ///
    /// /////////////////////////////

    // Calculates the bound and mutates the out vector
    private static void calculateBound(int[] b, double[] out, int i, int l) {
        double scale = Math.pow(2.0, i - l);
        for (int x = 0; x < b.length; x++) {
            out[x] = b[x] * scale;
        }
    }

    // Returns if the distance between bPrime and the scaled rhs is within the box
    private static boolean isInBounds(int[] bPrime, double[] scaledB, int herDisc) {
        int i = 0;
        boolean inBounds = true;
        while (i < bPrime.length && inBounds) {
            if (Math.abs(bPrime[i] - scaledB[i]) > 4L * herDisc) {
                inBounds = false;
            }
            i++;
        }
        return inBounds;
    }

    // Check if a given vector is in a set
    private static boolean containsVector(Set<int[]> set, int[] toFind) {
        boolean found;
        for (int[] b : set) {
            found = Arrays.equals(b, toFind);
            if (found) {
                return true;
            }
        }
        return false;
    }

    /// ////////////////////////
    /// FEASIBILITY CHECKER ///
    /// ////////////////////////

    public static boolean isFeasible(int[][] A, int[] rhs, int[] t, int r, int h) {
        // Largest value in matrix
        int delta = findLargestAbsValue(A);
        // Upper bound for the hereditary discrepancy
        double raw = 6.0 * Math.sqrt((double) h) * (double) delta;
        if (raw > Integer.MAX_VALUE) {
            throw new ArithmeticException("herDisc overflow");
        }
        int herDisc = (int) Math.ceil(raw);
        // System.out.printf("HerDics is: %d%n", herDisc);
        // Vector length for b/rhs
        int m = rhs.length;
        // K for n-fold
        long K = 0;
        for (int i = r; i < m; i++) {
            K += rhs[i];
            if (K > Integer.MAX_VALUE) {
                throw new ArithmeticException("K overflow");
            }
        }
        // System.out.printf("Value of K: %d%n", K);
        // Getting the number of iterations
        int l = (int) Math.ceil(Math.log(K) / Math.log(6.0 / 5.0));
        // System.out.printf("Iterations: %d%n", l);


        // Columns decoded to a number of base 8H+1
        int[][] cols = new int[A[0].length][A.length];
        for (int j = 0; j < A[0].length; j++) {
            for (int i = 0; i < A.length; i++) {
                cols[j][i] = A[i][j];
            }
        }

        // Initialize it with iteration i = 0
        Set<VectorKey> prev = new HashSet<>();
        // Add zero zero
        prev.add(new VectorKey(new int[m]));
        for (int[] col : cols) {
            prev.add(new VectorKey(col));
        }
        VectorKey sum;
        double[] bound = new double[m];
        for (int i = 1; i <= l; i++) {
            // System.out.printf("Iteration: %d%n", i);
            Set<VectorKey> next = new HashSet<>();
            calculateBound(rhs, bound, i, l);
            // Iterate over all combinations of vectors
            // TODO: Implement FFT
            for (VectorKey bPrime : prev) {
                for (VectorKey bDoublePrime : prev) {
                    sum = VectorKey.add(bPrime, bDoublePrime);
                    // System.out.println(Arrays.toString(sum.getVector()));
                    if (isSmallerComponentWise(sum.getVector(), rhs)
                            && isInBounds(sum.getVector(), bound, herDisc)) {
                        next.add(sum);
                    }
                }
            }
            prev = next;
            /*
            System.out.println("NEXT ITERATION");
            for (VectorKey key : prev) {
                System.out.println(Arrays.toString(key.getVector()));

            }
             */
        }
        VectorKey rhsVKey = new VectorKey(rhs);
        return prev.contains(rhsVKey);
    }

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_test.txt");
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
            System.out.printf("ILP instance %d is feasible: %b%n", count, result);
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}

