import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

// Helper class used to sort the breakpoints later
class Breakpoint {
    double d;
    int blockIndex;

    Breakpoint(double d, int setId) {
        this.d = d;
        this.blockIndex = setId;
    }

    // Getters
    public double getD() {
        return d;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    // Pretty printing
    @Override
    public String toString() {
        return String.format("(d=%.4f, blockIndex=%d)", d, blockIndex);
    }
}

class MakeshiftAlg {

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
    // Calculates if Ai + bPrime is in bounds
    // Also prunes partial solutions that already exceed the final RHS
    private static boolean isInBounds(int[] Ai, int[] bPrime, int[] b, double dk, int bound) {
        int val;
        for (int i = 0; i < bPrime.length; i++) {
            val = Ai[i] + bPrime[i];
            if (Math.abs(val - dk * b[i]) > bound || val > b[i]) {
                return false;
            }
        }
        return true;
    }

    // Sums up two integers vectors componentwise
    public static int[] add(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    //////////////////////
    /// MAIN ALGORITHM ///
    //////////////////////


    public static boolean isFeasible(int[][] matrix, int[] rhs, int d, int n, int[] c, int[] blockSizes) {
        // Copying the upper part or the whole matrix => A
        int[][] A = new int[d][n];
        for (int i = 0; i < d; i++) {
            A[i] = Arrays.copyOf(matrix[i], matrix[i].length);
        }
        // Copying the upper part of rhs => b
        int[] b = Arrays.copyOf(rhs, d);

        // Calculating the value for t (sum of lower rhs)
        int t = 0;
        int[] tAmount = Arrays.copyOfRange(rhs, d, rhs.length);
        for (int i = d; i < rhs.length; i++) {
            t += rhs[i];

        }
        // Amount of sets
        int P = tAmount.length;
        // Starting index for each block of length tS
        int[] blockStartIndices = new int[blockSizes.length];
        int startIdx = 0;
        for (int i = 0; i < blockSizes.length; i++) {
            blockStartIndices[i] = startIdx;
            startIdx += blockSizes[i];
        }

        // Initialize array of breakpoints
        Breakpoint[] breakpoints = new Breakpoint[t];
        int tPtr = 0;
        int setID = 0;
        double val;
        for (int ts : tAmount) {
            for (int i = 1; i <= ts; i++) {
                val = (double) i / ts;
                breakpoints[tPtr] = new Breakpoint(val, setID);
                tPtr++;
            }
            setID++;
        }
        // Sorting the breakpoints non-decreasingly
        Arrays.sort(breakpoints, Comparator.comparingDouble(bp -> bp.d));

        // Finding the largest entry in A
        int delta = findLargestAbsValue(A);
        int bound = 4 * d * delta * P;
        // Transposing A so we can index by column
        int[][] AT = new int[n][d];
        for (int i = 0; i < d; i++) {
            for (int j = 0; j < n; j++) {
                AT[j][i] = A[i][j];
            }
        }

        /////////////////////////////////////
        /// MAIN COMPUTATION OF THE GRAPH ///
        /////////////////////////////////////

        // Initializing graph
        Graph graph = new Graph();
        // Adding vertex at the first layer
        graph.addVertex(new Graph.Vertex(0, new int[d]));
        // Building the graph with vertices and edges
        // Declaring variables to be reused in runtime
        double dk;
        int blockIndex;
        int start;
        int end;
        int[] bPrime;
        int[] bDoublePrime;
        Graph.Vertex vDoublePrime;
        for (int k = 0; k < t; k++) {
            // Getting value for the bound of the next layer
            if (k + 1 == t) {
                // Final layer corresponds to time 1
                dk = 1.0;
            } else {
                // Normal next breakpoint
                dk = breakpoints[k + 1].getD();
            }
            // Getting the index range to check
            blockIndex = breakpoints[k].getBlockIndex();
            start = blockStartIndices[blockIndex];
            end = start + blockSizes[blockIndex] - 1;
            // The vertices in the previous layer
            for (Graph.Vertex vPrime : graph.getLayer(k)) {
                bPrime = vPrime.v();
                for (int i = start; i <= end; i++) {
                    // Add Vertex and Edge if they Vertex is in bounds
                    if (isInBounds(AT[i], bPrime, b, dk, bound)) {
                        bDoublePrime = add(AT[i], bPrime);
                        vDoublePrime = new Graph.Vertex(k + 1, bDoublePrime);
                        graph.addVertex(vDoublePrime);
                        graph.addEdge(vPrime, vDoublePrime, i, c[i]);
                    }
                }
            }
        }

        // Checking feasibility
        for (Graph.Vertex v : graph.getLayer(t)) {
            if (Arrays.equals(v.v(), b)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        if (args.length == 0) {
            System.err.println("No input file provided");
            System.exit(1);
        }
        String inputFile = args[0];
        ILPInstance[] inputs = p.parseFile(inputFile);
        System.out.println("Lars2");
        System.out.printf("Parameters: \n" + Arrays.toString(inputs[0].getParams()) + "\n");
        // Read instances
        int count = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] t = i.getT();
            int[] c = i.getC();
            int d = i.getR();
            int n = i.getH();
            // Get the result
            boolean result = isFeasible(matrix, rhs, d, n, c, t);
            // System.out.printf("ILP instance %d is feasible: %b%n", count, result);
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}
