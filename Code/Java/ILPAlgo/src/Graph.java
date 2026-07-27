import java.util.*;

public class Graph {

    // Storage
    private final Map<Vertex, Set<Edge>> adjacency = new HashMap<>();
    private final Map<Integer, List<Vertex>> layers = new HashMap<>();
    private final Map<Vertex, Vertex> canonical = new HashMap<>();


    ////////////////////////
    /// CANONICALIZATION ///
    ////////////////////////

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

    public void addVertex(Vertex v) {
        getOrCreate(v);
    }


    public void addEdge(Vertex from, Vertex to, int variableIndex, int weight) {
        from = getOrCreate(from);
        to = getOrCreate(to);

        adjacency.get(from).add(new Edge(from, to, variableIndex, weight));
    }


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

    public List<Vertex> getLayer(int j) {
        return layers.getOrDefault(j, Collections.emptyList());
    }
    public List<int[]> getVectorsInLayer(int layer) {
        List<int[]> vectors = new ArrayList<>();

        for (Vertex vertex : getLayer(layer)) {
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

        // To reconstruct path
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

        // If target was never reached
        if (!visited.contains(target)) {
            return null;
        }

        // Reconstruct path backwards
        List<Edge> path = new ArrayList<>();

        Vertex v = target;

        while (!v.equals(start)) {
            Edge e = parentEdge.get(v);
            if (e == null) {
                return null; // safety check
            }
            path.add(e);
            v = e.from();
        }

        Collections.reverse(path);
        return path;
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