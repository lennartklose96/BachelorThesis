import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_initial.txt");
        // Printing n
        System.out.println(inputs[0].getN());
        int[][] matrix = inputs[0].getMatrix();
        // Test printing matrix
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                System.out.print(matrix[i][j] + " ");
            }
            System.out.println();
        }
    }
}