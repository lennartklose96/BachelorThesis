import java.io.IOException;
import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) throws IOException, GRBException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_stein.txt");
        int count = 0;
        boolean allPassed = true;
        int failCount = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int[] c = i.getC();
            int r = i.getR();
            int h = i.getH();

            // Computing
            boolean gurobiResult = GurobiFeasibilityChecker.isFeasible(matrix, rhs, t, r);
            boolean algoResult = NFoldAlgLars.isFeasible(matrix, rhs, t, r, h);
            boolean steinitzResult = Steinitz.isFeasible(matrix, rhs, c, t, r, h);
            // boolean larsResult = LarsAlg.isFeasible(matrix, rhs, r, h);
            boolean same = gurobiResult == steinitzResult;// && algoResult == larsResult;
            allPassed = allPassed && same;
            failCount += same ? 0 : 1;
            // Checking if Gurobi and Algorithm produce the same results
            System.out.printf("Iteration: %d. Same feasibility: %b%n", count, same);
        }
        if (allPassed) {
            System.out.println("All tests passed!");
        } else {
            System.out.printf("Not all tests successful. %d tests failed.%n", failCount);
        }
        GurobiFeasibilityChecker.shutdown();
    }
}