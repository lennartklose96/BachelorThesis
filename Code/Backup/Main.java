import java.io.IOException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_initial.txt");
        // Printing n
        System.out.println(inputs[0].getN());
        int[][] matrix = inputs[0].getMatrix();
        // Test printing matrix
        for (int[] row : matrix) {
            System.out.println(Arrays.toString(row));
        }
    }
}