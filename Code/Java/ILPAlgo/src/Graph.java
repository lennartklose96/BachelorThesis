import java.util.*;

public class Graph {

    // Adjacency list
    private final Map<Vertex, Set<Edge>> adjacency = new HashMap<>();
    // Layers of the graph
    private final Map<Integer, List<Vertex>> layers = new HashMap<>();
    // Canonicalization of vectors
    // This is to ensure duplicate vectors refer to the same object
    private final Map<Vertex, Vertex> canonical = new HashMap<>();


    ////////////////////////
    /// CANONICALIZATION ///
    ////////////////////////

    // Create a vertex if none is there
    public Vertex getOrCreate(Vertex v) {
        return canonical.computeIfAbsent(v, k -> {
            adjacency.putIfAbsent(k, new HashSet<>());
            layers.computeIfAbsent(k.j(), x -> new ArrayList<>()).add(k);
            return k;
        });
    }


    ////////////////////////////////
    /// VERTEX AND EDGE ADDITION ///
    ////////////////////////////////

    // Adding a vertex to the graph
    public void addVertex(Vertex v) {
        getOrCreate(v);
    }

    // Adding an edge to the graph
    public void addEdge(Vertex from, Vertex to, int variableIndex, int weight) {
        from = getOrCreate(from);
        to = getOrCreate(to);

        adjacency.get(from).add(new Edge(from, to, variableIndex, weight));
    }

    // Return all outgoing edges for a vertex v
    public Collection<Edge> getOutgoing(Vertex v) {
        Vertex canonicalVertex = canonical.get(v);
        if (canonicalVertex == null) {
            return Collections.emptySet();
        }
        return adjacency.get(canonicalVertex);
    }


    //////////////
    /// LAYERS ///
    //////////////

    // Returns layer j
    public List<Vertex> getLayer(int j) {
        return layers.getOrDefault(j, Collections.emptyList());
    }

    // Returns all vectors in layer j
    public List<int[]> getVectorsInLayer(int j) {
        List<int[]> vectors = new ArrayList<>();

        for (Vertex vertex : getLayer(j)) {
            vectors.add(vertex.v());
        }

        return vectors;
    }

    ///////////
    /// BFS ///
    ///////////

    // Finds the shortest path using BFS
    public List<Edge> layeredBFS(Vertex start, Vertex target) {

        start = canonical.get(start);
        target = canonical.get(target);

        if (start == null || target == null) {
            return null;
        }

        Set<Vertex> visited = new HashSet<>();
        Queue<Vertex> queue = new ArrayDeque<>();

        // To reconstruct the path
        Map<Vertex, Edge> parentEdge = new HashMap<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {

            Vertex current = queue.poll();

            if (current.equals(target)) {
                break;
            }

            for (Edge edge : getOutgoing(current)) {

                Vertex next = edge.to();

                if (visited.add(next)) {
                    queue.add(next);
                    parentEdge.put(next, edge);
                }
            }
        }

        // Target was not reached
        if (!visited.contains(target)) {
            return null;
        }

        // Reconstruct path backwards
        List<Edge> path = new ArrayList<>();

        Vertex v = target;

        while (!v.equals(start)) {
            Edge e = parentEdge.get(v);
            // Safety check
            if (e == null) {
                return null;
            }
            path.add(e);
            v = e.from();
        }

        Collections.reverse(path);
        return path;
    }

    ///////////////////////////
    /// LONGEST PATH IN DAG ///
    ///////////////////////////

    public record LongestPathResult(int[] x, int[] cost) {}

    // Find the longest path in a graph from the specified starting point
    // to the specified target
    // Unused in the implementation
    public LongestPathResult longestPath(Vertex start, Vertex target, int numberOfVariables) {
        start = canonical.get(start);
        target = canonical.get(target);

        if (start == null || target == null) {
            return null;
        }

        // Maximum distance (objective value) to each vertex
        Map<Vertex, Integer> distance = new HashMap<>();

        // The edge used to reach each vertex optimally
        Map<Vertex, Edge> parentEdge = new HashMap<>();

        // Distance from start to itself is 0
        distance.put(start, 0);

        // Process layers in topological order
        for (int layer = start.j(); layer <= target.j(); layer++) {

            for (Vertex current : getLayer(layer)) {

                Integer currentDistance = distance.get(current);

                // Vertex is not reachable from start
                if (currentDistance == null) {
                    continue;
                }

                for (Edge edge : getOutgoing(current)) {

                    Vertex next = edge.to();

                    int newDistance =
                            currentDistance + edge.weight();

                    // If this is the first way to reach next,
                    // or this path is better than the previous one
                    if (!distance.containsKey(next)
                            || newDistance > distance.get(next)) {

                        distance.put(next, newDistance);
                        parentEdge.put(next, edge);
                    }
                }
            }
        }

        // Target is unreachable
        if (!distance.containsKey(target)) {
            return null;
        }

        // Reconstruct the optimal path
        List<Edge> path = new ArrayList<>();

        Vertex current = target;

        while (!current.equals(start)) {
            Edge edge = parentEdge.get(current);

            if (edge == null) {
                return null;
            }

            path.add(edge);
            current = edge.from();
        }

        Collections.reverse(path);

        // Construct x
        int[] x = new int[numberOfVariables];

        // Construct costs
        int[] cost = new int[path.size()];

        for (int i = 0; i < path.size(); i++) {
            Edge edge = path.get(i);

            // The edge corresponds to incrementing x_i
            x[edge.variableIndex()]++;

            // Store the cost of this selected edge
            cost[i] = edge.weight();
        }

        return new LongestPathResult(x, cost);
    }


    /////////////////////
    /// DEBUG HELPERS ///
    /////////////////////

    public void printVertices() {
        System.out.println("=== VERTICES ===");

        for (var entry : layers.entrySet()) {
            System.out.println("Layer " + entry.getKey());

            for (Vertex v : entry.getValue()) {
                System.out.println("    " + v);
            }
        }
    }

    public void printAdjacency() {
        System.out.println("=== ADJACENCY LIST ===");

        for (var entry : adjacency.entrySet()) {

            System.out.println(entry.getKey());

            if (entry.getValue().isEmpty()) {
                System.out.println("    (no outgoing edges)");
                continue;
            }

            for (Edge e : entry.getValue()) {
                System.out.println(
                        "    -> " + e.to() +
                                " (var=" + e.variableIndex() +
                                ", w=" + e.weight() + ")"
                );
            }
        }
    }

    public void printEdges() {
        System.out.println("=== EDGES ===");

        for (Set<Edge> edges : adjacency.values()) {
            for (Edge e : edges) {
                System.out.println(
                        e.from() + " -> " +
                                e.to() +
                                " (var=" + e.variableIndex() +
                                ", w=" + e.weight() + ")"
                );
            }
        }
    }


    //////////////
    /// VERTEX ///
    //////////////

    public record Vertex(int j, int[] v) {

        public Vertex {
            v = v.clone();
        }

        @Override
        public int[] v() {
            return v.clone();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Vertex other)) return false;

            return j == other.j &&
                    Arrays.equals(v, other.v);
        }

        @Override
        public int hashCode() {
            return 31 * Integer.hashCode(j)
                    + Arrays.hashCode(v);
        }

        @Override
        public String toString() {
            return "h(" + j + ", " + Arrays.toString(v) + ")";
        }
    }


    ////////////
    /// EDGE ///
    ////////////

    public record Edge(Vertex from, Vertex to, int variableIndex, int weight) {}
}