import com.gurobi.gurobi.GRBException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NFoldAlg {

    /// /////////////////////////////
    /// Generic helper functions ///
    /// /////////////////////////////
    // Returns a number with log_2
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    // Encodes an index/decimal number of the given base  as a vector of length r
    // Used to save a lot of memory down the line
    private static void encode(int[] out, int value, int base, int r) {
        for (int i = r - 1; i >= 0; i--) {
            out[i] = value % base;
            value /= base;
        }
    }

    // Decodes a vector to an integer
    static int decode(int[] vector, int base) {
        int result = 0;
        for (int digit : vector) {
            result = result * base + digit;
        }
        return result;
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

    // Returns the amount of vectors that exist in a given base of dimension r
    private static int getAllVectors(int r, int base) {
         return (int) Math.pow(base, r);
    }

    // Returns all valid indices for vDoublePrime given a vector allVectorsD[vectorIndex]
    // The index corresponds to the vectors in allVectorsK
    private static List<Integer> getAllValidIndices(
            int vectorIndex,
            int baseD,
            int baseK,
            int r) {

        // Getting the vector limit in base D
        int[] v = new int[r];
        encode(v, vectorIndex, baseD, r);
        // Get index of the upper bound in the K vector-space by "clamping" the vector
        int[] vClamp = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            vClamp[i] = Math.min(v[i], baseK - 1);
        }

        // All possibly vDoublePrime indices
        List<Integer> validIndices = new ArrayList<>();
        // Find all valid indices
        int[] vDoublePrime = new int[r];
        int pos = 0;
        boolean clamped;
        while (pos >= 0) {

            // Encode current digit vector and add it to results
            int validIndex = 0;
            for (int i = 0; i < r; i++) {
                validIndex = validIndex * baseK + vDoublePrime[i];
            }
            validIndices.add(validIndex);

            // Start at the rightmost position
            pos = r - 1;

            clamped = false;
            while (pos >= 0 && !clamped) {
                vDoublePrime[pos]++;
                // Digit position is within the bounds
                if (vDoublePrime[pos] <= vClamp[pos]) {
                    clamped = true;
                // Iterate to next more significant digit and reset to 0 (carry)
                } else {
                    vDoublePrime[pos] = 0;
                    pos--;
                }
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
        int vectorAmountK = getAllVectors(r, baseK);
        int vectorAmountD = getAllVectors(r, baseD);

        // Initializing base table and dynamic table
        boolean[] BT = new boolean[vectorAmountK];
        boolean[] DT = new boolean[vectorAmountD];
        boolean[] DTprev = new boolean[vectorAmountD];

        // Helper variable
        int[] kVector =  new int[r];

        // Maps the indices of K-Base to the indices of D-Base
        int[] kToDIndex = new int[vectorAmountK];
        for (int i = 0; i < vectorAmountK; i++) {
            encode(kVector, i, baseK, r);
            int index = 0;
            for (int j = 0; j < r; j++) {
                index = index * baseD + kVector[j];
            }
            kToDIndex[i] = index;
        }

        /*
        // For each vector in v ~in {0,...,D}^r check the valid indices for v''
        List<List<Integer>> allValidIndices = new ArrayList<>();
        for (int i = 0; i < vectorAmountD; i++) {
            allValidIndices.add(getAllValidIndices(i, baseD, baseK, r));
        }
         */


        // Building base table (BT) and dynamic table (DT) for iteration k = 1
        for (int v = 0; v < vectorAmountK; v++) {
            encode(kVector, v, baseK, r);
            BT[v] = GurobiFeasibilityChecker.isBrickFeasible(ABricks[0], kVector, bLowerSmalls[iteration][0]);
            DTprev[v] = BT[v];
        }

        // Building base table (BT) and dynamic table (DT) for iteration k = 2 ... n
        // Majority of the computation happening here.
        List<int[]> result = new ArrayList<>();
        List <Integer> validIndices;

        for (int k = 1; k < n; k++) {
            // Building base table
            for (int v = 0; v < vectorAmountK; v++) {
                encode(kVector, v, baseK, r);
                BT[v] = GurobiFeasibilityChecker.isBrickFeasible(ABricks[k], kVector, bLowerSmalls[iteration][k]);
            }
            // Building dynamic table
            for (int v = 0; v < vectorAmountD; v++) {
                validIndices =  getAllValidIndices(v, baseD, baseK, r);
                boolean feasible = false;
                int validIndex = 0;
                int vDoublePrime;
                int vPrime;
                // Big OR statement, checking all possible vector combinations to be valid
                while (validIndex < validIndices.size() && !feasible) {
                    // Indices corresponding to vectors in the BT and DT space
                    vDoublePrime = validIndices.get(validIndex);
                    vPrime = v - kToDIndex[vDoublePrime];
                    feasible = DTprev[vPrime] && BT[vDoublePrime];
                    validIndex++;
                }
                // Add the value to the dynamic table
                DT[v] = feasible;
                if (feasible && k == n-1) {
                    int[] feasibleVector = new int[r];
                    encode(feasibleVector, v, baseD, r);
                    result.add(feasibleVector);
                }
            }
            // Update the previous iteration
            DTprev = Arrays.copyOf(DT, DT.length);
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

    // FOR TESTING ONLY
    public static void main(String[] args) throws IOException, GRBException {

        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_test.txt");
        for (ILPInstance i : inputs) {
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int r = i.getR();
            int h = i.getH();

            boolean result = isFeasible(matrix, rhs, t, r, h);
            System.out.println(result);
        }
        GurobiFeasibilityChecker.shutdown();
    }
}