import java.io.IOException;
import com.gurobi.gurobi.*;

public class Main {
    public static void main(String[] args) throws IOException, GRBException {
        InstanceParser p = new InstanceParser();
        ILPInstance[] inputs = p.parseFile("Datasets/dataset_debug.txt");
        for (ILPInstance i : inputs) {
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int r = i.getR();

            boolean result = GurobiFeasibilityChecker.isFeasible(matrix, rhs, t, r);
            System.out.println(result);
        }
        GurobiFeasibilityChecker.shutdown();
    }
}