import java.io.IOException;
import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) throws IOException, GRBException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_test.txt");
        for (ILPInstance i : inputs) {
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int r = i.getR();
            int h = i.getH();


            boolean result = GurobiFeasibilityChecker.isFeasible(matrix, rhs, t, r);
            boolean result2 = NFoldAlg.isFeasible(matrix, rhs, t, r, h);

            // Checking if Gurobi and Algorithm produce the same results
            System.out.println(result == result2);
        }
        GurobiFeasibilityChecker.shutdown();
    }
}