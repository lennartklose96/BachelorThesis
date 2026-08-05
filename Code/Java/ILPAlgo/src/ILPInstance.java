public class ILPInstance {

    // The relevant variables for an ILP problem
    private int n;
    private int r;
    private int h;
    private int[] t;
    private int[] c;
    private int[] rhs;
    private int[][] matrix;
    private int[] params;

    // Constructor
    public ILPInstance(int[] params, int n, int r, int h, int[] t, int[] c, int[] rhs, int[][] matrix) {
        this.params = params;
        this.n = n;
        this.r = r;
        this.h = h;
        this.t = t;
        this.c = c;
        this.rhs = rhs;
        this.matrix = matrix;
    }

    // Getters and setters
    public int[] getParams() {
        return params;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getR() {
        return r;
    }

    public void setR(int r) {
        this.r = r;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public int[] getT() {
        return t;
    }

    public void setT(int[] t) {
        this.t = t;
    }

    public int[] getC() {
        return c;
    }

    public void setC(int[] c) {
        this.c = c;
    }

    public int[] getRhs() {
        return rhs;
    }

    public void setRhs(int[] rhs) {
        this.rhs = rhs;
    }

    public int[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(int[][] matrix) {
        this.matrix = matrix;
    }
}

