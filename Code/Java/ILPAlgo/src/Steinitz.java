import java.io.IOException;
import java.util.Arrays;

public class Steinitz {

    //////////////////////////////////////
    /// Functions defined in the paper ///
    //////////////////////////////////////

    // Returns the value q for the Algorithm
    private static int calculateQ(int[] rhs, int r) {
        int sum = 0;
        for (int i = r; i < rhs.length; i++) {
            sum += rhs[i];
        }
        return sum;
    }

    // Calculates the number of occurrences of e up to column j in mSigma
    private static int occ(int[] mSigma, int e, int j) {
        int count = 0;
        for (int i = 0; i < j; i++) {
            count += mSigma[i] == e ? 1 : 0;
        }
        return count;
    }

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

    ////////////////&/////////////////
    /// Algorithms from the paper ////
    //////////////////////////////////

    // Algorithm 1 from the paper
    private static int[] algorithm1(int[] b, int n, int q) {
        int[] result = new int[q];
        int bestE;
        double bestImb;
        for (int j = 1; j <= q; j++) {
            // Checking occurrences of e up until j
            bestE = -1;
            bestImb = Double.POSITIVE_INFINITY;
            for (int e = 1; e <= n; e++) {
                int occE = occ(result, e, j-1);
                // Calculate lowest imbalance value
                double imb = occE - ((double) j/q) * b[e-1];
                if (imb < bestImb) {
                    bestImb = imb;
                    bestE = e;
                }
            }
            // Update resulting mSigma
            result[j-1] = bestE;
        }
        return result;
    }

    // Constructs the graph from Construction 1 from the paper
    private static boolean construction1(int[][] matrix, int[] b, int[] mSigma, int r, int n, int delta) {
        // Helper variable
        int q = mSigma.length;
        int[][] lowerBounds = new int[r][n];
        int[][] upperBounds = new int[r][n];
        // Getting all the bounds for the vectors v that we can possibly construct
        for (int k = 0; k < r; k++) {
            for (int j = 1; j <= n; j++) {
                int lowerBound = (int) Math.ceil(((double) j/q) * b[k] - n*delta*(n+2*r));
                int upperBound = (int) Math.floor(((double) j/q) * b[k] + n*delta*(1 + 2*r));
                lowerBounds[k][j] = lowerBound;
                upperBounds[k][j] = upperBound;
            }
        }
        return false;
    }


    private static boolean isFeasible(int[][] matrix, int[] rhs, int[] tFull, int r, int h) {

        // Variables relevant to the algorithm
        int t = tFull[0];
        int n = tFull.length;
        int q = calculateQ(rhs, r);
        int delta = findLargestAbsValue(matrix, r, h);
        System.out.println("rhs:");
        System.out.println(Arrays.toString(rhs));
        System.out.printf("Value of n: %d%n", n);
        System.out.printf("Value of r: %d%n", r);
        System.out.printf("Value of q: %d%n", q);
        System.out.printf("Value of delta: %d%n", delta);

        // Initialize the permutation of M
        int[] mSigma = new int[q];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < rhs[i+r]; j++) {
                mSigma[idx++] = i+1;
            }
        }
        System.out.println("Initial MSigma:");
        System.out.println(Arrays.toString(mSigma));

        int[] bLower = Arrays.copyOfRange(rhs, r, rhs.length);
        mSigma = algorithm1(bLower, n, q);
        System.out.println("Reordered MSigma:");
        System.out.println(Arrays.toString(mSigma));

        System.out.println("Construction1 starts here: ");
        construction1(matrix, rhs, mSigma, r, n, delta);

        // Separator print
        System.out.println(" ");

        return false;
    }

    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_stein.txt");
        int count = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] tFull = i.getT();
            int r = i.getR();
            int h = i.getH();
            boolean result = isFeasible(matrix, rhs, tFull, r, h);
            // System.out.printf("ILP instance %d is feasible: %b%n", count, result);
            break;
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        // System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}
