import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class JKPT25Alg {

    /// /////////////////////////////
    /// Generic helper functions ///
    /// /////////////////////////////
    ///
    // Returns a number with log_2
    private static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    // Encodes a vector of length r into a single integer using the given base
    private static int encode(int[] v, int base, int r) {
        int value = 0;
        for (int i = 0; i < r; i++) {
            value = value * base + v[i];
        }
        return value;
    }

    // Decodes an index/decimal number of the given base as a vector of length r
    // Used to save a lot of memory down the line
    private static void decode(int[] out, int value, int base, int r) {
        for (int i = r-1; i >= 0; i--) {
            out[i] = value % base;
            value /= base;
        }
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
        // Edge case
        if (bDownMax == 0) return 1;
        double value = ((double) bDownMax + K) / (2.0 * K + 1.0);
        double log = log2(value);
        int ceil = (int) Math.ceil(log);
        boolean isInt = Math.abs(log - Math.round(log)) < 1e-9;
        return isInt ? ceil + 2 : ceil + 1;
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
    private static int[][] deriveSmallProblem(int[][] bLowers, int K) {
        int[][] result = new int[bLowers.length][bLowers[0].length];
        for (int i = 0; i < bLowers.length; i++) {
            for (int k = 0; k < bLowers[0].length; k++) {
                // Assigning correct small sub problem values
                if (bLowers[i][k] <= K) {
                    result[i][k] = bLowers[i][k];
                } else {
                    result[i][k] = (bLowers[i][k] - K) % 2 == 0 ? K : K - 1;
                }
            }
        }
        return result;
    }

    /// ///////////////////////////////////
    /// Solving feasibility for Ax = v ///
    /// ///////////////////////////////////

    // Checks if a vector addition for v' with baseD, and v'' with baseK produces a carry
    private static boolean hasCarry(int vPrime, int vDoublePrime, int baseD, int baseK, int r) {
        int vD;
        int vK;
        int i = r-1;
        boolean carry = false;
        while (i >= 0 && !carry) {
            // Encoded vector coordinate
            vD = vPrime % baseD;
            vK = vDoublePrime % baseK;
            carry = vK + vD >= baseD;
            // Moving the digit pointer
            vPrime /= baseD;
            vDoublePrime /= baseK;
            i--;
        }
        return carry;
    }

    /*
    Dynamic program to build the base table.
    The bulk of the computation occurs here.
    Returns N~(i).
     */
    private static List<int[]> buildSmallRHS(
            int[][][] ABricks,
            int[][] bLowerSmalls,
            int n, int K, int delta,
            int r, int h,
            int iteration) {
        // Calculating the number of possible vectors
        int baseK = (K * delta) + 1;
        int baseD = (K * delta * n) + 1;
        int vectorAmountK = (int) Math.pow(baseK, r);


        // Initializing base table and dynamic table
        BitSet BT = new BitSet();
        BitSet DT = new BitSet();
        BitSet DTprev = new BitSet();

        // Helper variable
        int[] indexVector = new int[r];

        // Maps the indices of K-Base to the indices of D-Base
        int[] kToDIndex = new int[vectorAmountK];
        for (int i = 0; i < vectorAmountK; i++) {
            // Decode the vector properly
            decode(indexVector, i, baseK, r);
            kToDIndex[i] = encode(indexVector, baseD, r);
        }

        // Helper variable
        int[] kVector = new int[r + 1];
        // Building base table (BT) and dynamic table (DT) for iteration k = 1
        for (int v = 0; v < vectorAmountK; v++) {
            // Encode and add bLowerSmall_k
            decode(kVector, v, baseK, r);
            kVector[r] = bLowerSmalls[iteration][0];
            // Setting feasibility

            if (JRAlg.isFeasible(ABricks[0], kVector, r, h)) {
                // Set vector as feasible
                BT.set(v);
                // Change encoding to D vector space
                DTprev.set(kToDIndex[v]);
            }
        }

        // Building base table (BT) and dynamic table (DT) for iteration k = 2 ... n
        // Majority of the computation happening here.
        for (int k = 1; k < n; k++) {
            // Building base table
            BT.clear();
            for (int v = 0; v < vectorAmountK; v++) {
                // Encode and add bLowerSmall_k
                decode(kVector, v, baseK, r);
                kVector[r] = bLowerSmalls[iteration][k];
                if (JRAlg.isFeasible(ABricks[k], kVector, r, h)) {
                    // Set vector as feasible
                    BT.set(v);
                }
            }
            // Building dynamic table
            // Checking every set bit in DT[k-1]
            for (int vPrime = DTprev.nextSetBit(0);
                vPrime >= 0;
                vPrime = DTprev.nextSetBit(vPrime + 1)) {
                // Checking every set bit in BT[k];
                for (int vDoublePrime = BT.nextSetBit(0);
                    vDoublePrime >= 0;
                    vDoublePrime = BT.nextSetBit(vDoublePrime + 1)) {
                    // Only check vectors that are in bounds of the problem, i.e. produce no carry
                    if (!hasCarry(vPrime, vDoublePrime, baseD, baseK, r)) {
                        DT.set(vPrime + kToDIndex[vDoublePrime]);
                    }
                }
            }
            // Update the previous iteration
            DTprev.clear();
            DTprev.or(DT);
            DT.clear();
        }

        // Storing the result
        List<int[]> result = new ArrayList<>();
        int[] feasibleVector;
        for (int v = DTprev.nextSetBit(0); v >= 0; v = DTprev.nextSetBit(v + 1)) {
            feasibleVector = new int[r];
            decode(feasibleVector, v, baseD, r);
            result.add(feasibleVector);
        }
        return result;
    }


    /// ////////////////////////
    /// FEASIBILITY CHECKER ///
    /// ////////////////////////
    // Checks if a given ILP problem is feasible
    public static boolean isFeasible(int[][] A, int[] rhs, int[] t, int r, int h) {
        /// PREPROCESSING
        // Creating relevant constant values for the algorithm
        int n = t.length;
        int delta = findLargestAbsValue(A, r, h);
        int K = (int) Math.floor(2 * (r + 1) * log2(4 * (r + 1)) * delta);
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
        int[][][] ABricks = new int[n][r+1][];
        int startIndex = 0;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < r; i++) {
                ABricks[k][i] = new int[t[k]];
                System.arraycopy(A[i], startIndex, ABricks[k][i], 0, t[k]);
            }
            // last row filled with ones
            ABricks[k][r] = new int[t[k]];
            Arrays.fill(ABricks[k][r], 1);
            startIndex += t[k];
        }

        /// MAIN ALGORITHM
        List<int[]> NCurr = new ArrayList<>();
        List<int[]> NPrev;
        List<int[]> NSmall;
        // Build upper RHS (small problem)
        NSmall = buildSmallRHS(ABricks, bLowerSmalls, n, K, delta, r, h, 0);
        // System.out.println("RHS FINISHED BUILDING");
        // Initialize N
        NPrev = new ArrayList<>(NSmall);
        // Initializing helper variables
        int[] candidate = new int[r];
        for (int i = 1; i < iterations; i++) {
            // Building the box boundary bound for later
            // Scaled up to avoid floating point computation
            int shift = iterations - (i + 1);
            // Safety check
            if (shift < 0 || shift >= 63) {
                throw new ArithmeticException("invalid shift: " + shift);
            }
            // Box boundary
            long bound = (long) D << shift;
            // Building the upper small RHS
            // WARNING: Computation expensive!
            NSmall = buildSmallRHS(ABricks, bLowerSmalls, n, K, delta, r, h, i);
            // System.out.println("RHS FINISHED BUILDING");
            // Final part, checking all valid solutions
            for (int[] bUpperPrev : NPrev) {
                for (int[] bUpperSmall : NSmall) {

                    boolean valid = true;
                    int x = 0;
                    while (x < r && valid) {
                        // Getting out doubled + added candidate
                        int doubleVal = (bUpperPrev[x] * 2) + bUpperSmall[x];
                        candidate[x] = doubleVal;
                        // Building comparison value
                        long comp = ((long) doubleVal) << shift;
                        long diff = bUpper[x] - comp;
                        // Checking component wise size
                        if (diff < 0 || diff > bound) {
                            valid = false;
                        }
                        x++;
                    }
                    if (valid && !containsVector(NCurr, candidate)) {
                        NCurr.add(Arrays.copyOf(candidate, r));
                    }
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

    // Read a specified input file to parse
    public static void main(String[] args) throws IOException {

        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        if (args.length == 0) {
            System.err.println("No input file provided");
            System.exit(1);
        }
        String inputFile = args[0];
        ILPInstance[] inputs = p.parseFile(inputFile);
        System.out.println("Lis");
        System.out.printf("Parameters: \n" + Arrays.toString(inputs[0].getParams()) + "\n");
        int count = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int r = i.getR();
            int h = i.getH();
            boolean result = isFeasible(matrix, rhs, t, r, h);
            // System.out.printf("ILP instance %d is feasible: %b%n", count, result);
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("Time elapsed: %d%n", timeElapsed);



    }
}