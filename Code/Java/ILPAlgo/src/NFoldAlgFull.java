import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gurobi.gurobi.*;

public class NFoldAlgFull {

    /// /////////////////////////////
    /// Generic helper functions ///
    /// /////////////////////////////
    // Returns a number with log_2
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    // Returns an array as a string
    private static String key(int[] v) {
        return Arrays.toString(v);
    }

    // Scales the vector by the given shift to the power of 2
    private static double[] scaleVector(int[] v, int shift) {
        double[] result = new double[v.length];
        double factor = Math.pow(2, shift);
        for (int i = 0; i < v.length; i++) {
            result[i] = v[i] / factor;
        }
        return result;
    }

    // Check if a given vector is in a list
    private static boolean containsVector(List<int[]> set, int[] toFind) {
        boolean found = false;
        int i = 0;
        while (!found && i < set.size()) {
            if (Arrays.equals(set.get(i), toFind)) {
                found = true;
            }
            ;
            i++;
        }
        return found;
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

    /// //////////////////////////////
    /// BUILDING THE B SUBVECTORS ///
    /// //////////////////////////////

    // Returns the next instance of bDown for the previous iteration steps
    private static int[][] deriveBLowers(int[] bLower, int K, int I) {
        // Stores all values for all iterations for bDown
        int[][] result = new int[I][bLower.length];
        // Initialize final iteration step
        result[I - 1] = Arrays.copyOf(bLower, bLower.length);
        // Go over every iteration step
        for (int i = I - 2; i >= 0; i--) {
            // Initialize the new array
            int[] newBDown = new int[bLower.length];
            int z;
            for (int j = 0; j < bLower.length; j++) {
                // Apply inductive logic if entry in next iteration is larger than K
                if (result[i + 1][j] > K) {
                    z = ((result[i + 1][j] % 2) == (K % 2)) ? 0 : 1;
                    newBDown[j] = (result[i + 1][j] - (K + z)) / 2;
                } else {
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
                    result[i][k] = (bDowns[i][k] - K) % 2 == 0 ? K : K - 1;
                }
            }
        }
        return result;
    }

    // Derives all the even subproblems given b_k and b~_k
    private static int[][] deriveEvenProblem(int[][] bDowns, int[][] bSmalls) {
        int[][] result = new int[bDowns.length][bDowns[0].length];
        for (int i = 0; i < bDowns.length; i++) {
            for (int k = 0; k < bDowns[0].length; k++) {
                result[i][k] = bDowns[i][k] - bSmalls[i][k];
            }
        }
        return result;
    }

    /// ///////////////////////////////////
    /// Solving feasibility for Ax = v ///
    /// ///////////////////////////////////

    // Gets all integer vectors of dimension r with a maximum size of base
    private static int[][] getAllVectors(int r, int base) {
        int amount = (int) Math.pow(base, r);
        // Storing result vectors
        int[][] result = new int[amount][r];
        int[] v = new int[r];
        for (int iteration = 0; iteration < amount; iteration++) {
            System.arraycopy(v, 0, result[iteration], 0, r);
            // Incrementing v
            int i = r - 1;
            boolean incremented = false;
            while (!incremented && i >= 0) {
                if (v[i] < base - 1) {
                    v[i]++;
                    incremented = true;
                } else {
                    v[i] = 0;
                    i--;
                }
            }
        }
        return result;
    }

    // Returns all valid indices for vDoublePrime given a vector allVectorsD[vectorIndex]
    // The index corresponds to the vectors in allVectorsK
    private static List<Integer> getAllValidIndices(
            int[][] allVectorsD,
            int[][] allVectorsK,
            int vectorIndex,
            int baseK) {

        int[] v = allVectorsD[vectorIndex];
        // All possibly vDoublePrime indices
        List<Integer> validIndices = new ArrayList<>();
        // Get index of the upper bound in the K vector-space by "clamping" the vector
        int[] vClamp = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            vClamp[i] = Math.min(v[i], baseK - 1);
        }
        int boundIndex = 0;
        for (int component : vClamp) {
            boundIndex = boundIndex * baseK + component;
        }
        // Find all valid indices
        for (int j = 0; j <= boundIndex; j++) {
            int[] vDoublePrime = allVectorsK[j];
            // Checking if each component is smaller, making it a valid summand
            boolean valid = true;
            int k = 0;
            while (k < v.length && valid) {
                valid = vDoublePrime[k] <= v[k];
                k++;
            }
            if (valid) {
                validIndices.add(j);
            }
        }
        return validIndices;
    }

    /*
    Dynamic program to build the base table.
    The bulk of the computation occurs here.
    Returns N~(i).
     */
    private static List<int[]> buildUpperSmallRHS(
            int[][][] ABricks,
            int[][] bLowerSmalls,
            int n, int K, int delta,
            int iteration)
            throws GRBException {
        // Calculating the number of possible vectors
        int r = ABricks[0].length;
        int baseK = (K * delta) + 1;
        int baseD = (K * delta * n) + 1;
        int vectorAmountK = 1;
        int vectorAmountD = 1;
        for (int i = 0; i < r; i++) {
            vectorAmountK *= baseK;
            vectorAmountD *= baseD;
        }
        // Initializing base table and dynamic table
        boolean[][] BT = new boolean[vectorAmountK][n];
        boolean[][] DT = new boolean[vectorAmountD][n];
        // Maps the K space vectors to the D space vectors
        int[][] allVectorsK = getAllVectors(r, baseK);
        int[][] allVectorsD = getAllVectors(r, baseD);
        // Maps the indices of K-Base to the indices of D-Base
        int[] kToDIndex = new int[vectorAmountK];
        for (int i = 0; i < vectorAmountK; i++) {
            int index = 0;
            for (int j = 0; j < r; j++) {
                index = index * baseD + allVectorsK[i][j];
            }
            kToDIndex[i] = index;
        }

        // Building the base table, and the first entry of the dynamic table.
        // Iterating over every possible vector for each brick
        for (int i = 0; i < vectorAmountK; i++) {
            for (int k = 0; k < n; k++) {
                BT[i][k] = GurobiFeasibilityChecker.isBrickFeasible(ABricks[k], allVectorsK[i], bLowerSmalls[iteration][k]);
            }
            // Reusing the loop to also build the first value of the dynamic table
            DT[kToDIndex[i]][0] = BT[i][0];
        }
        // System.out.println("Vectors to check for RHS: " + Integer.toString(vectorAmountD));
        // Building the dynamic table.
        // Majority of the computation happening here.
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < vectorAmountD; i++) {
            // All possible v'' indices
            List<Integer> validIndices = getAllValidIndices(allVectorsD, allVectorsK, i, baseK);
            // Updating every entry in the DT given a vector v
            for (int k = 1; k < n - 1; k++) {
                boolean feasible = false;
                int validIndex = 0;
                int vDoublePrime;
                int vPrime;
                // Big OR statement, checking all possible vector combinations to be valid
                while (validIndex < validIndices.size() && !feasible) {
                    // Indices corresponding to vectors in the BT and DT space
                    vDoublePrime = validIndices.get(validIndex);
                    vPrime = i - kToDIndex[vDoublePrime];
                    feasible = DT[vPrime][k - 1] && BT[vDoublePrime][k];
                    validIndex++;
                }
                DT[i][k] = feasible;
            }
            // Last step calculated separately to save on unnecessary comparisons for checking result values
            boolean feasible = false;
            int validIndex = 0;
            int vDoublePrime;
            int vPrime;
            // Big OR statement, checking all possible vector combinations to be valid
            while (validIndex < validIndices.size() && !feasible) {
                // Indices corresponding to vectors in the BT and DT space
                vDoublePrime = validIndices.get(validIndex);
                vPrime = i - kToDIndex[vDoublePrime];
                feasible = DT[vPrime][n - 2] && BT[vDoublePrime][n - 1];
                validIndex++;
            }
            DT[i][n - 1] = feasible;
            // Checking result
            if (feasible) {
                result.add(allVectorsD[i]);
            }
        }
        return result;
    }


    /// ////////////////////////
    /// FEASIBILITY CHECKER ///
    /// ////////////////////////
    // Checks if a given ILP problem is feasible
    public static boolean isFeasible(int[][] A, int[] rhs, int[] t, int r, int h) throws GRBException {
        /// PREPROCESSING
        // Creating relevant constant values for the algorithm
        int n = t.length;
        int delta = findLargestAbsValue(A, r, h);
        int K = (int) Math.ceil(2 * (r + 1) * log2(4 * (r + 1)) * delta);
        int D = delta * K * n;
        // Creating bUpper and bDown initial versions
        int[] bUpper = new int[r];
        int[] bLower = new int[n];
        System.arraycopy(rhs, 0, bUpper, 0, r);
        System.arraycopy(rhs, r, bLower, 0, n);
        // Maximum values in the lower rhs
        int bDownMax = findMax(bLower);
        // Determine the amount of iterations the algorithm will take
        int iterations = determineIterationAmount(K, bDownMax);
        /*
        Determining the initial bDowns on each iteration step.
        Then, determine the small and even problem portions as well
        First index = iteration, second index = component.
         */
        int[][] bLowers = deriveBLowers(bLower, K, iterations);
        int[][] bLowerSmalls = deriveSmallProblem(bLowers, K);
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
        // Helper prints
        // System.out.println("Value of K: " + Integer.toString(K));
        // System.out.println("Iteration amount: " + Integer.toString(iterations));

        /// MAIN ALGORITHM
        List<int[]> NCurr = new ArrayList<>();
        List<int[]> NPrev;
        List<int[]> NSmall;
        // Build upper RHS (small problem)
        NSmall = buildUpperSmallRHS(ABricks, bLowerSmalls, n, K, delta, 0);
        // System.out.println("RHS FINISHED BUILDING");
        // Initialize N
        NPrev = new ArrayList<>(NSmall);
        // Initializing helper variables
        int[] candidate = new int[r];
        double[] newBUpper;
        for (int i = 1; i < iterations; i++) {
            // Takes a long time
            NSmall = buildUpperSmallRHS(ABricks, bLowerSmalls, n, K, delta, i);
            // System.out.println("RHS FINISHED BUILDING");
            // Final part, checking all valid solutions
            newBUpper = scaleVector(bUpper, iterations - (i + 1));
            int count = NPrev.size() * NSmall.size();
            for (int[] bUpperPrev : NPrev) {
                for (int[] bUpperSmall : NSmall) {

                    double maxAbs = 0.0;
                    boolean valid = true;

                    int x = 0;
                    while (x < r && valid) {
                        int val = (bUpperPrev[x] * 2) + bUpperSmall[x];
                        candidate[x] = val;
                        double diff = newBUpper[x] - val;
                        maxAbs = Math.max(maxAbs, Math.abs(diff));
                        if (maxAbs > D) {
                            valid = false;
                        }
                        x++;
                    }
                    if (valid && !containsVector(NCurr, candidate)) {
                        NCurr.add(Arrays.copyOf(candidate, r));
                    }
                    count--;
                    // System.out.println(count);
                }
            }
            // Swap and free memory
            List<int[]> tmp = NPrev;
            NPrev = NCurr;
            NCurr = tmp;
            NCurr.clear();

        }
        // Check if the vector is in the final set
        return containsVector(NPrev, bUpper);
    }

    // Determines if a given ILP is feasible using the Jansen-Rohwedder algorithm
    public static boolean isFeasible(int[][] matrix, int[] rhs) {
        return false;
    }


    public static void main(String[] args) throws IOException, GRBException {
        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_test.txt");
        int count = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int r = i.getR();
            int h = i.getH();

            boolean result2 = isFeasible(matrix, rhs, t, r, h);
            System.out.printf("ILP instance %d is feasible: %b%n", count, result2);
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("Time elapsed: %d%n", timeElapsed);
        GurobiFeasibilityChecker.shutdown();
    }
}