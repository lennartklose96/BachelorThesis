import com.gurobi.gurobi.*;

public class GurobiFeasibilityChecker {

    /**
     * matrix: full A matrix (r × h)
     * rhs: concatenated (bUp, bDown)
     * t: block sizes t_i
     * r: number of global constraints
     */
    public static boolean isFeasible(
            int[][] matrix,
            int[] rhs,
            int[] t,
            int r
    ) throws GRBException {

        int n = t.length;

        // -----------------------------
        // Split RHS into bUp and bDown
        // -----------------------------
        int[] bUp = new int[r];
        int[] bDown = new int[n];

        System.arraycopy(rhs, 0, bUp, 0, r);
        System.arraycopy(rhs, r, bDown, 0, n);

        // -----------------------------
        // Compute block offsets
        // -----------------------------
        int[] start = new int[n];
        start[0] = 0;
        for (int i = 1; i < n; i++) {
            start[i] = start[i - 1] + t[i - 1];
        }

        GRBEnv env = new GRBEnv(true);
        env.set(GRB.IntParam.OutputFlag, 0);
        env.start();

        GRBModel model = new GRBModel(env);

        // -----------------------------
        // Variables: x[i][j]
        // -----------------------------
        GRBVar[][] x = new GRBVar[n][];

        for (int i = 0; i < n; i++) {
            x[i] = new GRBVar[t[i]];

            for (int j = 0; j < t[i]; j++) {
                x[i][j] = model.addVar(
                        0.0,
                        GRB.INFINITY,
                        0.0,
                        GRB.INTEGER,
                        "x_" + i + "_" + j
                );
            }
        }

        // -----------------------------
        // Local constraints:
        // sum_j x[i][j] = bDown[i]
        // -----------------------------
        for (int i = 0; i < n; i++) {

            GRBLinExpr expr = new GRBLinExpr();

            for (int j = 0; j < t[i]; j++) {
                expr.addTerm(1.0, x[i][j]);
            }

            model.addConstr(expr, GRB.EQUAL, bDown[i], "local_" + i);
        }

        // -----------------------------
        // Global constraints:
        // matrix is r × h
        // -----------------------------
        for (int k = 0; k < r; k++) {

            GRBLinExpr expr = new GRBLinExpr();

            for (int i = 0; i < n; i++) {
                for (int j = 0; j < t[i]; j++) {

                    int col = start[i] + j;

                    expr.addTerm(matrix[k][col], x[i][j]);
                }
            }

            model.addConstr(expr, GRB.EQUAL, bUp[k], "global_" + k);
        }

        // -----------------------------
        // Feasibility objective
        // -----------------------------
        model.setObjective(new GRBLinExpr(), GRB.MINIMIZE);

        // -----------------------------
        // Solve
        // -----------------------------
        model.optimize();

        int status = model.get(GRB.IntAttr.Status);

        boolean feasible = (status == GRB.OPTIMAL);

        model.dispose();
        env.dispose();

        return feasible;
    }
}