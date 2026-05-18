import java.io.IOException;
import java.util.*;

public class LarsAlg {

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

    // Helper object to eliminate duplicates
    static class NoDupesMatrix {
        int[][] matrix;
        int[] c;
        // Constructor
        public NoDupesMatrix(int[][] matrix, int[] c) {
            this.matrix = matrix;
            this.c = c;
        }
        // Getters
        public int[][] getMatrix() {
            return matrix;
        }

        public int[] getC() {
            return c;
        }
    }


    public static void main(String[] args) throws IOException {

        /*
        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_debug.txt");
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
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("Time elapsed: %d%n", timeElapsed);
        */
    }
}
