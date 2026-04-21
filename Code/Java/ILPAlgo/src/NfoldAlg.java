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

    private static int determineIterationAmount(int K, int bDownMax) {
        double iterations = log2(((double) bDownMax + K) / (2*K + 1));
        if ((iterations % 1) == 0) {
            return (int) iterations + 2;
        } else {
            return (int) Math.ceil(iterations) + 1;
        }
    }

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
        // Amount of iterations
        int iterations = determineIterationAmount(K, bDownMax);









        // TODO: REMOVE LATER
        System.out.println(K);
        System.out.println(bDownMax);
        System.out.println(iterations);

        // TODO: REMOVE LATER
        // System.out.println(Arrays.toString(bUp));
        // System.out.println(Arrays.toString(bDown));



        // TODO: real return value
        return K > 5;
    }

    public static void main(String[] args) throws IOException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_test.txt");
        // Testing instance
        int instanceNumber = 6;
        int[][] matrix = inputs[instanceNumber].getMatrix();
        int[] rhs = inputs[instanceNumber].getRhs();
        int[] t = inputs[instanceNumber].getT();
        int r = inputs[instanceNumber].getR();
        int h = inputs[instanceNumber].getH();
        System.out.println(NfoldAlg.isFeasible(matrix, rhs, t, r, h));
    }
}
