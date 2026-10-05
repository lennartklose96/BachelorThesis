import java.io.IOException;

import com.gurobi.gurobi.*;

public class GurobiTest {
    // This class is used to compare the results of different algorithms in a testing environment
    public static void main(String[] args) throws IOException, GRBException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_test.txt");
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
            boolean algoResult = JKPT25Alg.isFeasible(matrix, rhs, t, r, h);
            // boolean larsResult = LarsAlg.isFeasible(matrix, rhs, r, h);
            // boolean steinitzResult = Steinitz.isFeasible(matrix, rhs, c, t, r, h);
            // boolean makeshiftResult = MakeshiftAlg.isFeasible(matrix, rhs, r, h, c, t);
            boolean same = gurobiResult == algoResult;
            allPassed = allPassed && same;
            failCount += same ? 0 : 1;
            // Checking if Gurobi and Algorithm produce the same results

            System.out.printf("Iteration: %d. Same feasibility: %b%n", count, same);
            /*
            for (int[] x : matrix) {
                System.out.println(Arrays.toString(x));
            }
             */
        }
        if (allPassed) {
            System.out.println("All tests passed!");
        } else {
            System.out.printf("Not all tests successful. %d tests failed.%n", failCount);
        }
        GurobiFeasibilityChecker.shutdown();
    }
}