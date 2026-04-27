import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gurobi.gurobi.*;

public class NfoldAlg {

    ////////////////////////////////
    /// Generic helper functions ///
    ////////////////////////////////
    // Returns a number with log_2
    private static double log2 (double x) {
        return Math.log(x) / Math.log(2);
    }

    // Returns a vector where each entry is doubled
    private static int[] getDoubledVector(int[] v) {
        int[] result = new int[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = v[i] * 2;
        }
        return result;
    }

    // Returns a vector that is the sum of the two other vectors
    // If negative == true, subtracts the second vector from the first one
    private static int[] addVectors(int[] v1, int[]v2, boolean negative) {
        int[] result = new int[v1.length];
        if (!negative) {
            for (int i = 0; i < v1.length; i++) {
                result[i] = v1[i] + v2[i];
            }
        } else {
            for (int i = 0; i < v2.length; i++) {
                result[i] = v1[i] - v2[i];
            }
        }
        return result;
    }

    // Scales the vector by the given shift to the power of 2
    private static int[] scaleVector(int[] v, int shift) {
        int[] result = new int[v.length];
        int factor = 1 << shift;
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
            if (Arrays.equals(set.get(i), toFind)) { found = true;};
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

    // Find the maximum absolute value in the given vector
    private static int findMaxAbs(int[] vector) {
        int largest = 0;
        for (int i : vector) {
            largest = Math.max(largest, Math.abs(i));
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

    //////////////////////////////////////
    /// Solving feasibility for Ax = v ///
    //////////////////////////////////////

    // Gets all integer vectors of dimension r with a maximum size of base
    private static int[][] getAllVectors(int r, int base) {
        int amount = 1;
        for (int i = 0; i < r; i++) {
            amount *= base;
        }
        // Storing result vectors
        int[][] result = new int[amount][r];
        int[] v = new int[r];
        for (int iteration = 0; iteration < amount; iteration++) {
            System.arraycopy(v, 0, result[iteration], 0, r);
            // Incrementing v
            int i = r-1;
            boolean incremented = false;
            while (!incremented && i >= 0) {
                if (v[i] < base-1) {
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

    // TODO: Remove? not used atm
    private static void incrementV(int[] v, int base) {
        int incIndex = v.length-1;
        boolean found = false;
        while (!found && incIndex >= 0) {
            if (v[incIndex] < base) {
                v[incIndex]++;
                found = true;
            } else {
                v[incIndex] = 0;
                incIndex--;
            }
        }
    }

    // Returns all valid indices for vPrime given a vector allVectors[vectorIndex]
    private static List<Integer> getAllValidIndices(int[][] allVectors, int vectorIndex) {
        int[] v = allVectors[vectorIndex];
        // All possibly vPrime indices
        List<Integer> validIndices = new ArrayList<>();
        for (int j = 0; j <= vectorIndex; j++) {
            int[] vPrime = allVectors[j];
            // Checking if each component is smaller, making it a valid summand
            boolean valid = true;
            int k = 0;
            while (k < v.length && valid) {
                valid = vPrime[k] <= v[k];
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
        int base = (K * delta) + 1;
        int vectorAmount = 1;
        for (int i = 0; i < r; i++) {
            vectorAmount *= base;
        }
        // Initializing base table and dynamic table
        boolean[][] BT = new boolean[vectorAmount][n];
        boolean[][] DT = new boolean[vectorAmount][n];

        // Building the base table, and the first entry of the dynamic table.
        // Iterating over every possible vector for each brick
        int[][] allVectors = getAllVectors(r, base);
        for (int i = 0; i < vectorAmount; i++) {
            for (int k = 0; k < n; k++) {
                BT[i][k] = GurobiFeasibilityChecker.isBrickFeasible(ABricks[k], allVectors[i], bLowerSmalls[iteration][k]);
            }
            // Reusing the loop to also build the first value of the dynamic table
            DT[i][0] = BT[i][0];
        }
        // Building the dynamic table.
        // Majority of the computation happening here.
        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < vectorAmount; i++) {
            // All possibly vPrime indices
            List<Integer> validIndices = getAllValidIndices(allVectors, i);
            // Updating every entry in the DT given a vector v
            for (int k = 1; k < n-1; k++) {
                boolean feasible = false;
                int vIndex = 0;
                // Big OR statement, checking all possible vector combinations to be valid
                while(vIndex < validIndices.size() && !feasible) {
                    feasible = DT[validIndices.get(vIndex)][k-1] && BT[i-validIndices.get(vIndex)][k];
                    vIndex++;
                }
                DT[i][k] = feasible;
            }
            // Last step calculated separately to save on unnecessary comparisons for checking result values
            boolean feasible = false;
            int vIndex = 0;
            // Big OR statement, checking all possible vector combinations to be valid
            while(vIndex < validIndices.size() && !feasible) {
                feasible = DT[validIndices.get(vIndex)][n-2] && BT[i-validIndices.get(vIndex)][n-1];
                vIndex++;
            }
            DT[i][n-1] = feasible;
            // Checking result
            if (feasible) {
                result.add(allVectors[i]);
            }
        }
        return result;
    }


    ///////////////////////////
    /// FEASIBILITY CHECKER ///
    ///////////////////////////
    // Checks if a given ILP problem is feasible
    public static boolean isFeasible(int[][] A, int[] rhs, int[] t, int r, int h) throws GRBException {
        /// PREPROCESSING
        // Creating relevant constant values for the algorithm
        int n = t.length;
        int delta = findLargestAbsValue(A, r, h);
        int K = (int) Math.ceil(2 * (r+1) * log2(4 * (r+1)) * delta);
        // Creating bUpper and bDown initial versions
        int[] bUpper = new int[r];
        int[] bLower = new int[n];
        System.arraycopy(rhs, 0, bUpper, 0, r);
        System.arraycopy(rhs, r, bLower, 0, n);
        // Maximum values in the lower rhs
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
        int[][] bLowerEvens = deriveEvenProblem(bLowers, bLowerSmalls); // Might not be needed? TODO: Remove, maybe
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
        System.out.println("Value of K: " + Integer.toString(K));
        System.out.println("Iteration amount: " + Integer.toString(iterations));

        /// MAIN ALGORITHM
        @SuppressWarnings("unchecked")
        List<int[]>[] Ns = new ArrayList[iterations];
        @SuppressWarnings("unchecked")
        List<int[]>[] NEvens = new ArrayList[iterations];
        @SuppressWarnings("unchecked")
        List<int[]>[] NSmalls = new ArrayList[iterations];
        NSmalls[0] = buildUpperSmallRHS(ABricks, bLowerSmalls, n, K, delta, 0);
        Ns[0] = NSmalls[0];
        int D = delta * K * n;
        // Initializing helper variables
        int[] candidate;
        int[] newBUpper;
        int[] difference;
        for (int i = 1; i < iterations; i++) {
            NEvens[i] = new ArrayList<>();
            // Check all previous RHS and double them
            for (int[] upperRHS: Ns[i-1]) {
                NEvens[i].add(getDoubledVector(upperRHS));
            }
            // Takes a long time
            NSmalls[i] = buildUpperSmallRHS(ABricks, bLowerSmalls, n, K, delta, i);
            // Final part, checking all valid solutions
            Ns[i] = new ArrayList<>();
            newBUpper = scaleVector(bUpper, iterations-(i+1));
            for(int[] bUpperEven: NEvens[i]) {
                for (int[] bUpperSmall: NSmalls[i]) {
                    candidate = addVectors(bUpperEven, bUpperSmall, false);
                    difference = addVectors(newBUpper, candidate, true);
                    // System.out.println(findMaxAbs(difference));
                    if (findMax(difference) <= D) {
                        Ns[i].add(candidate);
                    }
                }
            }
        }
        // Shutting down the checker
        // TODO: Read more into it, move this, or maybe replace entirely
        GurobiFeasibilityChecker.shutdown();
        // Check if solution is valid
        return containsVector(Ns[iterations-1], bUpper);
    }

    public static void main(String[] args) throws IOException, GRBException {
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
