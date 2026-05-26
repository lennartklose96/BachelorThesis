import java.util.Arrays;

public final class VectorKey {
    final int[] v;
    final int hash;

    VectorKey(int[] v) {
        this.v = v.clone();
        this.hash = Arrays.hashCode(this.v);
    }

    // Adding two vectors
    public static VectorKey add(VectorKey a, VectorKey b) {
        int[] res = new int[a.v.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = a.v[i] + b.v[i];
        }
        return new VectorKey(res);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof VectorKey other
                && Arrays.equals(v, other.v);
    }
    @Override
    public int hashCode() {
        return hash;
    }

    // Getter
    public int[] getVector() {
        return v;
    }
}