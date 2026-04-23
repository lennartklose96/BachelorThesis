import java.io.IOException;
import java.util.Arrays;
import com.gurobi.gurobi.*;

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
        double value = ((double) bDownMax + K) / (2.0 * K + 1.0);
        double iterations = log2(value);
        return (int) Math.ceil(iterations) + 1;
    }

    /////////////////////////////////
    /// BUILDING THE B SUBVECTORS ///
    /////////////////////////////////

    // Returns the next instance of bDown for the previous iteration steps
    private static int[][] deriveBLowers(int[] bLower, int K, int I) {
        // Stores all values for all iterations for bDown
        int[][] result = new int[I][bLower.length];
        // Initialize final iteration step
        result[I - 1] = Arrays.copyOf(bLower, bLower.length);
        // Go over every iteration step
        for (int i = I-2; i >= 0; i--) {
            // Initialize the new array
            int[] newBDown = new int[bLower.length];
            int z;
            for (int j = 0; j < bLower.length; j++) {
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

    // Derives all the even subproblems given b_k and b~_k
    private static int[][] deriveEvenProblem(int[][] bDowns, int[][]bSmalls) {
        int[][] result = new int[bDowns.length][bDowns[0].length];
        for (int i = 0; i < bDowns.length; i++) {
            for (int k = 0; k < bDowns[0].length; k++) {
                result[i][k] = bDowns[i][k] - bSmalls[i][k];
            }
        }
        return result;
    }

    // TODO: REMOVE (probably)
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

    //////////////////////////////////////
    /// Solving feasibility for Ax = v ///
    //////////////////////////////////////

    // Increments a vector read as a number with a given base.
    // Returns a zero vector if the value is already maxed out (it lopos around).
    private static void incrementV(int[] v, int base) {
        int incIndex = v.length-1;
        boolean found = false;
        while (!found && incIndex >= 0) {
            if (v[incIndex] < base) {
                v[incIndex] += 1;
                found = true;
            } else {
                v[incIndex] = 0;
                incIndex -= 1;
            }
        }
    }

    // Returns all possible sums of integral vectors that add to v.
    private static int[][] getVectorCombinations(int[] v) {
        // TODO: Implement
        return null;
    }

    /*
    Dynamic program to build the base table.
    The bulk of the computation occurs here.
    Returns N~(i).
     */
    private static boolean[][] buildUpperSmallRHS(int[][][] ABricks, int[][] bLowerSmalls, int n, int K, int delta, int iteration)
            throws GRBException {
        // Calculating the number of possible vectors
        int r = ABricks[0].length;
        int base = (K * delta) + 1;
        int vectorAmount = 1;
        for (int i = 0; i < r; i++) {
            vectorAmount *= base;
        }
        // Initializing base table and dynamic table
        boolean[][] BT = new boolean[vectorAmount][n];
        boolean[][] DT = new boolean[vectorAmount][n];

        // Building the base table.
        // Iterating over every possible vector for each brick
        int[] v = new int[r];
        for (int i = 0; i < vectorAmount; i++) {
            for (int k = 0; k < n; k++) {
                BT[i][k] = GurobiFeasibilityChecker.isBrickFeasible(ABricks[k], v, bLowerSmalls[iteration][k]);
            }
            // Checks the next possible vector
            incrementV(v, base);
        }

        // Building the dynamic table.
        // Majority of the computation happening here.


        GurobiFeasibilityChecker.shutdown();
        return BT;
    }



    // Checks if a given ILP problem is feasible
    public static boolean isFeasible(int[][] A, int[] rhs, int[] t, int r, int h) throws GRBException {
        // Creating relevant constant values for the algorithm
        int n = t.length;
        int delta = findLargestAbsValue(A, r, h);
        int K = (int) Math.ceil(2 * (r+1) * log2(4 * (r+1)) * delta);
        // Creating bUp and bDown initial versions
        int[] bUpper = new int[r];
        int[] bLower = new int[n];
        System.arraycopy(rhs, 0, bUpper, 0, r);
        System.arraycopy(rhs, r, bLower, 0, n);
        // Maximum values in the upper and lower rhs
        int bUpMax = findMax(bUpper);
        int bDownMax =  findMax(bLower);
        // Determine the amount of iterations the algorithm will take
        int iterations = determineIterationAmount(K, bDownMax);

        /*
        Determining the initial bDowns on each iteration step.
        Then, determine the small and even problem portions as well
        First index = iteration, second index = component.
         */
        int[][] bLowers = deriveBLowers(bLower, K, iterations);
        int[][] bLowerSmalls = deriveSmallProblem(bLowers, K);
        int[][] bLowerEvens = deriveEvenProblem(bLowers, bLowerSmalls); // Might not be necessary? TODO: Figure out if this is needed

        /*
        Determining the A bricks.
        The first index indicates which brick k in n it is.
         */
        int[][][] ABricks = new int[n][r][];
        int startIndex = 0;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < r; i++) {
                ABricks[k][i] = new int[t[k]];
                System.arraycopy(A[i], startIndex, ABricks[k][i], 0, t[k]);
            }
            startIndex += t[k];
        }

        // TODO: REMOVE LATER
        System.out.println("Value of K: " + Integer.toString(K));
        System.out.println("Iteration amount: " + Integer.toString(iterations));

        System.out.println(Arrays.toString(bLowers[0]));
        System.out.println(Arrays.toString(bLowerSmalls[0]));
        System.out.println(Arrays.toString(bLowerEvens[0]));


        // TODO: real return value
        return true;
    }

    public static void main(String[] args) throws IOException, GRBException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_debug.txt");
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
