import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GJSZ25Alg {

    ///////////////////////////////
    /// Generic helper function ///
    ///////////////////////////////

    // Sums up two integers vectors componentwise
    public static int[] add(int[] a, int[] b) {
        int[] result = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    // Increments a vector given the lower and upper bounds on each index
    public static void incBoundedVector(int[] v, int[] lowerBounds, int[] upperBounds) {
        // Increment at the back.
        int i = v.length - 1;
        boolean incremented = false;
        // Repeatedly try to increment a number
        while (i >= 0 && !incremented) {
            if (v[i] < upperBounds[i]) {
                v[i]++;
                incremented = true;
            } else {
                v[i] = lowerBounds[i];
                i--;
            }
        }
    }

    // Returns if a vector is in the given bounds given for each index
    public static boolean isInBounds(int[] v, int[] lowerBounds, int[] upperBounds) {
        for (int i = 0; i < v.length; i++) {
            if (!(v[i] >= lowerBounds[i] && v[i] <= upperBounds[i])) {
                return false;
            }
        }
        return true;
    }


    // Returns all columns for a given block i
    public static int[][] getBlockColumns(int[][] matrix, int i, int r, int t) {
        int[][] result = new int[t][r];
        int baseCol = i * t;
        for (int k = 0; k < t; k++) {
            for (int row = 0; row < r; row++) {
                result[k][row] = matrix[row][baseCol + k];
            }
        }
        return result;
    }

    //////////////////////////////////////
    /// Functions defined in the paper ///
    //////////////////////////////////////

    // Returns the value q for the Algorithm
    private static int calculateQ(int[] rhs, int r) {
        int sum = 0;
        for (int i = r; i < rhs.length; i++) {
            sum += rhs[i];
        }
        return sum;
    }

    // Calculates the number of occurrences of e up to column j in mSigma
    private static int occ(int[] mSigma, int e, int j) {
        int count = 0;
        for (int i = 0; i < j; i++) {
            count += mSigma[i] == e ? 1 : 0;
        }
        return count;
    }

    // Find the largest absolute value in an ILP matrix, given r and h
    private static int findLargestAbsValue(int[][] matrix, int r, int h) {
        int largest = 1;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < h; j++) {
                largest = Math.max(largest, Math.abs(matrix[i][j]));
            }
        }
        return largest;
    }

    ////////////////&/////////////////
    /// Algorithms from the paper ////
    //////////////////////////////////

    // Algorithm 1 from the paper
    private static int[] algorithm1(int[] bLower, int n, int q) {
        int[] result = new int[q];
        int bestE;
        double bestImb;
        for (int j = 1; j <= q; j++) {
            // Checking occurrences of e up until j
            bestE = -1;
            bestImb = Double.POSITIVE_INFINITY;
            for (int e = 1; e <= n; e++) {
                int occE = occ(result, e, j-1);
                // Calculate lowest imbalance value
                double imb = occE - ((double) j/q) * bLower[e-1];
                if (imb < bestImb) {
                    bestImb = imb;
                    bestE = e;
                }
            }
            // Update resulting mSigma
            result[j-1] = bestE;
        }
        return result;
    }

    // Constructs the graph from Construction 1 from the paper
    private static Graph construction1(int[][] matrix, int[] b, int[] c, int[] mSigma, int r, int n, int t, int delta) {
        /// CALCULATING BOUNDS
        // Helper variable
        int q = mSigma.length;
        int[][] lowerBounds = new int[q][r];
        int[][] upperBounds = new int[q][r];
        // Getting all the bounds for the vectors v that we can possibly construct
        for (int j = 1; j <= q; j++) {
            for (int k = 0; k < r; k++) {
                int lowerBound = (int) Math.ceil(((double) j/q) * b[k] - n*delta*(n+2*r));
                int upperBound = (int) Math.floor(((double) j/q) * b[k] + n*delta*(1 + 2*r));
                lowerBounds[j-1][k] = lowerBound;
                upperBounds[j-1][k] = upperBound;
            }
        }

        //////////////////////
        /// GRAPH BUILDING ///
        //////////////////////
        // The graph to add vertices and edges(arcs) to
        Graph graph = new Graph();
        /// ADDING VERTICES
        // Adding the vertices
        for (int j = 0; j < q; j++) {
            // Initializing vector v
            int[] v = Arrays.copyOf(lowerBounds[j], r);
            // Calculating the amount of incrementation
            // Alternatively: Use return value
            int incAmount = 1;
            for (int k = 0; k < r; k++) {
                incAmount *= (upperBounds[j][k] - lowerBounds[j][k] + 1);
            }
            // Add each vertex to the graph
            for (int i = 0; i < incAmount; i++) {
                graph.addVertex(new Graph.Vertex(j+1, Arrays.copyOf(v, r)));
                incBoundedVector(v, lowerBounds[j], upperBounds[j]);
            }
        }
        // Add the h_(0,0) vertex
        graph.addVertex(new Graph.Vertex(0, new int[r]));

        /// ADDING EDGES
        // Reusable variables to store information for the blocks
        int i;
        int[][] colVectors;
        int[] vCol;
        int[] v2;
        for (int j = 0; j < q; j++) {
            // Finding out the block index we need to look in
            i = mSigma[j];
            colVectors = getBlockColumns(matrix, i-1, r, t);
            // Check over j-1
            // Because of how the vertices are defined, j = j-1 in this case
            for (Graph.Vertex v1 : graph.getLayer(j)) {
                // Seeing if the combinations are in bound
                for (int k = 0; k < t; k++) {
                    vCol = colVectors[k];
                    v2 = add(v1.v(), vCol);
                    if (isInBounds(v2, lowerBounds[j], upperBounds[j])) {
                        graph.addEdge(
                                v1,
                                new Graph.Vertex(j+1, v2),
                                (i - 1) * t + k,
                                c[(i-1)*t+k]
                        );
                    }
                }
            }
        }
        return graph;
    }


    public static boolean isFeasible(int[][] matrix, int[] rhs, int[]c, int[] tFull, int r, int h) {
        // Variables relevant to the algorithm
        int t = tFull[0];
        int n = tFull.length;
        int q = calculateQ(rhs, r);
        int[] bLower = Arrays.copyOfRange(rhs, r, rhs.length);
        int delta = findLargestAbsValue(matrix, r, h);

        // Performing algorithm 1 from the paper for the optimal permutation
        int[] mSigma = algorithm1(bLower, n, q);

        // Building the graph used for BFS
        Graph graph = construction1(matrix, rhs, c, mSigma, r, n, t, delta);
        Graph.Vertex start = new Graph.Vertex(0, new int[r]);
        Graph.Vertex target = new Graph.Vertex(q, Arrays.copyOf(rhs, r));
        List<Graph.Edge> path = graph.layeredBFS(start, target);
        // Check feasibility
        boolean feasible;
        // Building the result
        int[] x = new int[n * t];
        int cost = 0;
        if (path == null) {
            feasible = false;
        } else {
            feasible = true;
            for (Graph.Edge e : path) {
                x[e.variableIndex()]++;
                cost += e.weight();
            }
        }
        return feasible;
    }

    // Read a specified input file to parse
    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        InstanceParser p = new InstanceParser();
        if (args.length == 0) {
            System.err.println("No input file provided");
            System.exit(1);
        }
        String inputFile = args[0];
        ILPInstance[] inputs = p.parseFile(inputFile);
        System.out.println("Gupta");
        System.out.printf("Parameters: \n" + Arrays.toString(inputs[0].getParams()) + "\n");
        int count = 0;
        for (ILPInstance i : inputs) {
            count++;
            int[][] matrix = i.getMatrix();
            int[] rhs = i.getRhs();
            int[] tFull = i.getT();
            int[] c = i.getC();
            int r = i.getR();
            int h = i.getH();
            boolean result = isFeasible(matrix, rhs, c, tFull, r, h);
            // System.out.printf("ILP instance %d is feasible: %b%n", count, result);
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        System.out.printf("Time elapsed: %d%n", timeElapsed);
    }
}