import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;


public class LarsAlg {

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

    // Returns the largest entry in a vector
    private static int maxEntry(int[] v) {
        int largest = 0;
        for (int entry : v) {
            largest = Math.max(largest, entry);
        }
        return largest;
    }

    // Calculates the bound and mutates the out vector
    private static void calculateBound(int[] b, double[] out, int i, int l) {
        double scale = Math.pow(2.0, i - l);
        for (int x = 0; x < b.length; x++) {
            out[x] = b[x] * scale;
        }
    }


    // Checks if a given encoded vector is smaller than rhs and in bounds
    private static boolean isSmallerAndInBounds(int v, int[] rhs, double[] bound, double herDisc, int base) {
        boolean valid = true;
        int i = 0;
        int tmp = v;
        int component;
        while (valid && i < rhs.length) {
            // Decode next component
            component = tmp % base;
            tmp /= base;
            if (component > rhs[i] || Math.abs((component - bound[i])) > 4*herDisc) {
                valid = false;
            }
            i++;
        }
        return valid;
    }


    ////////////////
    /// ENCODING ///
    ////////////////

    static int encode(int[] v, int base) {
        int idx = 0;
        int mul = 1;
        for (int x : v) {
            if (x >= base || x < 0)
                throw new RuntimeException("encoding overflow");
        }
        for (int x : v) {
            int add = x * mul;
            idx += add;
            mul *= base;
            // Safety check
            if (mul < 0) throw new ArithmeticException("overflow in encoding");
        }
        return idx;
    }

    // Decodes a base-'base' vector into a single integer
    private static void decode(int value, int[] out, int base, int m) {
        for (int i = 0; i < m; i++) {
            out[i] = value % base;
            value /= base;
        }
    }

    // Checks if with the addition of two vectors, a carry would occur
    private static boolean hasCarry(int a, int b, int base, int m) {
        boolean carry = false;
        int i = 0;
        while (i < m && !carry) {
            int ai = a % base;
            int bi = b % base;
            if (ai + bi >= base) {
                carry = true;
            }
            a /= base;
            b /= base;
            i++;
        }
        return carry;
    }

    ///////////////////////////
    /// FEASIBILITY CHECKER ///
    ///////////////////////////

    public static boolean isFeasible(int[][] A, int[] rhs, int r, int h) {
        // Largest value in matrix
        int delta = findLargestAbsValue(A);
        // Upper bound for the hereditary discrepancy
        double herDisc = DiscrepancyCalculator.hereditaryDiscrepancy(A);
        // System.out.printf("HerDics is: %f%n", herDisc);
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
        int base = Math.max(delta,maxEntry(rhs))+1;
        // System.out.printf("Base is %d%n", base);
        // Maximum vector size that can be reached
        int maxSize = 1;
        for (int i = 0; i < m; i++){
            maxSize *= base;
            if (maxSize < 0) throw new ArithmeticException("overflow in encoding");
        }

        // Columns decoded to a number of base 8H+1
        int[][] cols = new int[A[0].length][A.length];
        for (int j = 0; j < A[0].length; j++) {
            for (int i = 0; i < A.length; i++) {
                cols[j][i] = A[i][j];
            }
        }
        // Bulk computation
        BitSet prev = new BitSet();
        prev.set(0);
        for (int[] v : cols) {
            prev.set(encode(v, base));
        }

        // Initialize sum
        int sum;
        // Box boundary
        double[] bound = new double[m];
        // Iterate over pairs
        int rhsEncoded = encode(rhs, base);
        for (int i = 1; i <= l; i++) {
            // System.out.printf("Iteration: %d%n", i);
            // Initialize the next set
            BitSet next = new BitSet();
            calculateBound(rhs, bound, i, l);
            // Iterate over all combinations of vectors
            for (int a = prev.nextSetBit(0);
                a >= 0;
                a = prev.nextSetBit(a + 1)) {
                for (int b = prev.nextSetBit(a);
                    b >= 0;
                    b = prev.nextSetBit(b + 1)) {
                    if (!hasCarry(a, b, base, m)) {
                        sum = a + b;
                        if (isSmallerAndInBounds(sum, rhs, bound, herDisc, base)) {
                            next.set(sum);
                        }
                    }
                }
            }
            // Early return
            if (prev.get(rhsEncoded)) {
                return true;
            }
            prev = next;
        }
        return prev.get(rhsEncoded);
    }

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        if (args.length == 0) {
            System.err.println("No input file provided");
            System.exit(1);
        }
        String inputFile = args[0];
        ILPInstance[] inputs = p.parseFile(inputFile);
        System.out.println("Lars1");
        System.out.printf("Parameters: \n" + Arrays.toString(inputs[0].getParams()) + "\n");
        // Read instances
        int count = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int r = i.getR();
            int h = i.getH();
            // Get the result
            boolean result = isFeasible(matrix, rhs, r, h);
            // System.out.printf("ILP instance %d is feasible: %b%n", count, result);
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}
