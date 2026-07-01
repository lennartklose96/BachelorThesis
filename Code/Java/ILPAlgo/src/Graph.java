import java.util.*;

public class Graph {

    /* =========================
       CORE STORAGE
       ========================= */

    private final Map<Vertex, List<Edge>> adjacency = new HashMap<>();
    private final Map<Integer, List<Vertex>> layers = new HashMap<>();
    private final Map<Vertex, Vertex> canonical = new HashMap<>();

    /* =========================
       CANONICALIZATION
       ========================= */

    public Vertex getOrCreate(Vertex v) {
        return canonical.computeIfAbsent(v, k -> {
            adjacency.putIfAbsent(k, new ArrayList<>());
            layers.computeIfAbsent(k.j(), x -> new ArrayList<>()).add(k);
            return k;
        });
    }

    /* =========================
       VERTEX ADDITION
       ========================= */

    public Vertex addVertex(Vertex v) {
        return getOrCreate(v);
    }

    /* =========================
       EDGE ADDITION (IMPORTANT)
       ========================= */

    public void addEdge(Vertex from, Vertex to, int weight) {
        from = getOrCreate(from);
        to = getOrCreate(to);

        adjacency.get(from).add(new Edge(from, to, weight));
    }

    /* =========================
       OUTGOING EDGES
       ========================= */

    public List<Edge> getOutgoing(Vertex v) {
        return adjacency.getOrDefault(getOrCreate(v), Collections.emptyList());
    }

    /* =========================
       LAYERS
       ========================= */

    public List<Vertex> getLayer(int j) {
        return layers.getOrDefault(j, Collections.emptyList());
    }

    /* =========================
       BFS (CORRECT)
       ========================= */

    public boolean layeredBFS(Vertex start, Vertex target) {

        start = getOrCreate(start);
        target = getOrCreate(target);

        Set<Vertex> visited = new HashSet<>();
        Queue<Vertex> queue = new ArrayDeque<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {

            Vertex v = queue.poll();

            if (v.equals(target)) {
                return true;
            }

            for (Edge e : getOutgoing(v)) {

                Vertex nxt = getOrCreate(e.to);

                if (visited.add(nxt)) {
                    queue.add(nxt);
                }
            }
        }

        return false;
    }

    /* =========================
       DEBUG HELPERS
       ========================= */

    public void printAdjacency() {
        for (var entry : adjacency.entrySet()) {
            System.out.println(entry.getKey());
            for (Edge e : entry.getValue()) {
                System.out.println("  -> " + e.to);
            }
        }
    }

    /* =========================
       VERTEX
       ========================= */

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
            return j == other.j && Arrays.equals(v, other.v);
        }

        @Override
        public int hashCode() {
            return 31 * Integer.hashCode(j) + Arrays.hashCode(v);
        }

        @Override
        public String toString() {
            return "h(" + j + ", " + Arrays.toString(v) + ")";
        }
    }

    /* =========================
       EDGE
       ========================= */

    public record Edge(Vertex from, Vertex to, int weight) {}


}