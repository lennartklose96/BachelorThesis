import java.io.IOException;
import java.util.Arrays;

public class NfoldAlg {

    // Returns a number with log_2
    private static double log2 (double x) {
        return Math.log(x) / Math.log(2);
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

    // Find the maximum value in the given vector
    private static int findMax(int[] vector) {
        int largest = Integer.MIN_VALUE;
        for (int i : vector) {
            largest = Math.max(largest, i);
        }
        return largest;
    }

    // Determines the amount of iteration steps in the algorithm
    private static int determineIterationAmount(int K, int bDownMax) {
        double iterations = log2(((double) bDownMax + K) / (2*K + 1));
        if ((iterations % 1) == 0) {
            return (int) iterations + 2;
        } else {
            return (int) Math.ceil(iterations) + 1;
        }
    }

    // Returns the next instance of bDown for the previous iteration steps
    private static int[][] deriveBDowns(int[] bDown, int K, int I) {
        // Stores all values for all iterations for bDown
        int[][] result = new int[I][bDown.length];
        // Initialize final iteration step
        result[I - 1] = Arrays.copyOf(bDown, bDown.length);
        // Go over every iteration step
        for (int i = I-2; i >= 0; i--) {
            // Initialize the new array
            int[] newBDown = new int[bDown.length];
            int z;
            for (int j = 0; j < bDown.length; j++) {
                // Apply inductive logic if entry in next iteration is larger than K
                if (result[i+1][j] > K) {
                    z = ((result[i + 1][j] % 2) == (K % 2)) ? 0 : 1;
                    newBDown[j] = (result[i + 1][j] - (K + z)) / 2;
                } else{
                    newBDown[j] = 0;
                }
            }
            result[i] = newBDown;
        }
        return result;
    }

    // Derives all the small subproblems given every value of b_i(k), given i in I.
    private static int[][] deriveSmallProblem(int[][] bDowns, int K) {
        int[][] result = new int[bDowns.length][bDowns[0].length];
        for (int i = 0; i < bDowns.length; i++) {
            for (int k = 0; k < bDowns[0].length; k++) {
                // Assigning correct small sub problem values
                if (bDowns[i][k] <= K) {
                    result[i][k] = bDowns[i][k];
                } else {
                    result[i][k] = (bDowns[i][k] - K) % 2 == 0 ? K : K-1;
                }
            }
        }
        return result;
    }


    /*  ALTERNATIVE METHOD IN CASE THE ORIGINAL ONES FAILS (it should not)
    // Returns the next instance of bDown for the previous iteration steps
    private static int[][] deriveBDowns2(int[] bDown, int K, int I) {
        // Stores all values for all iterations for bDown
        int[][] result = new int[I][bDown.length];
        // Initialize final iteration step
        result[I - 1] = Arrays.copyOf(bDown, bDown.length);
        // Go over every iteration step
        for (int i = I-2; i >= 0; i--) {
            // Initialize the new array
            int[] newBDown = new int[bDown.length];
            for (int k = 0; k < bDown.length; k++) {
                // Apply inductive logic if entry in next iteration is larger than K
                if (result[i+1][k] <= K) {
                    newBDown[k] = 0;
                } else{
                    double sum = 0;
                    for (int l = i+1; l <= I-1; l++) {
                        sum += (1.0 / Math.pow(2, I-l));
                    }
                    newBDown[k] = (int) Math.floor( (bDown[k]/Math.pow(2,I-(i+1)) - K * sum) );
                }
            }
            result[i] = newBDown;
        }
        return result;
    }
     */



    // Checks if a given ILP problem is feasible
    public static boolean isFeasible(int[][] matrix, int[] rhs, int[] t, int r, int h) {
        // Creating relevant constant values for the algorithm
        int n = t.length;
        int delta = findLargestAbsValue(matrix, r, h);
        int K = (int) Math.ceil(2 * (r+1) * log2(4 * (r+1)) * delta);
        // Creating bUp and bDown initial versions
        int[] bUp = new int[r];
        int[] bDown = new int[n];
        System.arraycopy(rhs, 0, bUp, 0, r);
        System.arraycopy(rhs, r, bDown, 0, n);
        // Maximum values in the upper and lower rhs
        int bUpMax = findMax(bUp);
        int bDownMax =  findMax(bDown);
        // Determine the amount of iterations the algorithm will take
        int iterations = determineIterationAmount(K, bDownMax);

        /*
        Determining the initial bDowns on each iteration step
        Then, determine the small Problem portion of each bDown as well
         */
        int[][] bDowns = deriveBDowns(bDown, K, iterations);
        int[][] smallProblem = deriveSmallProblem(bDowns, K);




        // TODO: REMOVE LATER
        System.out.println("Value of K: " + Integer.toString(K));
        System.out.println("Iteration amount: " + Integer.toString(iterations));

        System.out.println(Arrays.toString(bDown));
        System.out.println(Arrays.toString(bDowns[5]));
        System.out.println(Arrays.toString(smallProblem[5]));

        // TODO: real return value
        return K > 5;
    }

    public static void main(String[] args) throws IOException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_test.txt");
        // Testing instance
        int instanceNumber = 0;
        int[][] matrix = inputs[instanceNumber].getMatrix();
        int[] rhs = inputs[instanceNumber].getRhs();
        int[] t = inputs[instanceNumber].getT();
        int r = inputs[instanceNumber].getR();
        int h = inputs[instanceNumber].getH();
        System.out.println(NfoldAlg.isFeasible(matrix, rhs, t, r, h));
    }
}
