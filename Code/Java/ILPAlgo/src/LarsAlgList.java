import java.io.IOException;
import java.util.*;

public class LarsAlgList {


    /////////////////////////
    /// UTILITY FUNCTIONS ///
    /////////////////////////

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
        while (smaller & i < v.length) {
            if (v[i] > target[i]) {
                smaller = false;
            }
            i++;
        }
        return smaller;
    }

    public static int[] addVectors(int[] a, int[] b) {
        int n = a.length;
        int[] res = new int[n];
        for (int i = 0; i < n; i++) {
            res[i] = a[i] + b[i];
        }
        return res;
    }

    ////////////////////////////////
    /// MAIN VECTOR CALCULATIONS ///
    ////////////////////////////////

    private static boolean isInBounds(int[] bPrime, int[] b, int i, int l, int herDisc) {
        double scale = Math.pow(2.0, i - l);
        int x = 0;
        boolean inBounds = true;
        double first;
        double second;
        while (x < b.length && inBounds) {
            first = bPrime[x];
            second = b[x] * scale;
            if (Math.abs(first - second) > 4L * herDisc) {
                inBounds = false;
            }
            x++;
        }
        return inBounds;
    }

    // Check if a given vector is a given list
    private static boolean containsVector(List<int[]> list, int[] toFind) {
        boolean found;
        for  (int[] b : list) {
            found = Arrays.equals(b, toFind);
            if (found) {
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
        int delta = findLargestAbsValue(A);
        // Upper bound for the hereditary discrepancy
        int herDisc = (int) Math.ceil(6 * Math.sqrt((double) h) * delta);
        // System.out.printf("HerDics is: %d%n", herDisc);
        // Vector length for b/rhs
        int m = rhs.length;
        // K for n-fold 
        int K = 0;
        for (int i = r; i < m; i++) {
            K += rhs[i];
        }
        // System.out.printf("Value of K: %d%n", K);
        // Getting the number of iterations
        int l = (int) Math.ceil(Math.log(K) / Math.log(6.0 / 5.0));
        // System.out.printf("Iterations: %d%n", l);

        // The maximum amount of vectors we can check
        // Equal to 8H + 1
        int base = 8 * herDisc + 1;
        // System.out.printf("Base is %d%n", base);
        // Columns decoded to a number of base 8H+1
        int[][] cols = new int[A[0].length][A.length];
        for (int j = 0; j < A[0].length; j++) {
            for (int i = 0; i < A.length; i++) {
                cols[j][i] = A[i][j];
            }
        }

        List<int[]> prev = new ArrayList<>();
        // Add zero iteration
        prev.add(new int[m]);
        Collections.addAll(prev, cols);
        // Initializing the sum
        int[] sum;
        for (int i = 1; i < l; i++) {
            // System.out.printf("Iteration: %d%n", i);
            List<int[]> next = new ArrayList<>();
            // Iterate over all combinations of vectors
            // TODO: Implement FFT
            for (int a = 0; a < prev.size(); a++) {
                for (int b = a;  b < prev.size(); b++) {
                    sum = addVectors(prev.get(a), prev.get(b));
                    // System.out.println(Arrays.toString(sum));
                    if (isSmallerComponentWise(sum, rhs)
                    && isInBounds(sum, rhs, i, l, herDisc)
                    && !containsVector(next, sum)) {
                        next.add(sum);
                    }
                }
            }
            prev = next;
        }
        return containsVector(prev, rhs);
    }

    public static void main(String[] args) throws IOException {
        // long start = System.currentTimeMillis();
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
        // long finish = System.currentTimeMillis();
        // long timeElapsed = finish - start;
        // System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}
