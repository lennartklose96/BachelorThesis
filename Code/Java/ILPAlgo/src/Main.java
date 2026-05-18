import java.io.IOException;
import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) throws IOException, GRBException {
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

            // Computing
            boolean gurobiResult = GurobiFeasibilityChecker.isFeasible(matrix, rhs, t, r);
            boolean algoResult = NFoldAlg.isFeasible(matrix, rhs, t, r, h);
            // Checking if Gurobi and Algorithm produce the same results
            System.out.printf("Iteration: %d. Same feasibility: %b%n", count, gurobiResult == algoResult);
        }
        GurobiFeasibilityChecker.shutdown();
    }
}