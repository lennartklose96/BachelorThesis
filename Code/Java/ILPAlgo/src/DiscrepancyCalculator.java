// A class to calculate the discrepancy and hered
public class DiscrepancyCalculator {
    // Returns the discrepancy for a matrix A
    public static double discrepancy(int[][] A) {
        // Initializing variables
        int n = A[0].length;
        int iterations = 1 << n;
        double[] vec = new double[n];
        int value;
        // Minimum value for discrepancy
        double discrepancy = Integer.MAX_VALUE;
        for (int mask = 0; mask < iterations; mask++) {
            // Building the vector
            for (int i = 0; i < n; i++) {
                value = (mask >> i) & 1;
                vec[i] = value - 0.5;
            }
            // Expensive part: Matrix x vector multiplication
            double sum;
            double max = Integer.MIN_VALUE;
            for (int[] row : A) {
                sum = 0;
                for (int j = 0; j < n; j++) {
                    sum += row[j] * vec[j];
                }
                max = Math.max(Math.abs(sum), max);
            }
            discrepancy = Math.min(discrepancy, max);
        }
        return discrepancy;
    }

    // Returns the hereditary discrepancy for a matrix A
    public static double hereditaryDiscrepancy(int[][] A) {
        // Indices
        int m = A.length;
        int n = A[0].length;
        int iterations = 1 << n;
        // Helper variables
        int[] colPositions;
        int columnAmount;
        int colIndex;
        // Result variable
        double hereditaryDiscrepancy = Integer.MIN_VALUE;
        for (int mask = 1; mask < iterations; mask++) {
            // Amount of columns chosen in this iteration
            columnAmount = Integer.bitCount(mask);
            // The positions of the chosen columns
            colPositions = new int[columnAmount];
            colIndex = 0;
            for (int i = 0; i < n; i++) {
                if (((mask >> i) & 1) > 0) {
                    colPositions[colIndex] = i;
                    colIndex++;
                }
            }
            // Build the new restricted matrix
            int[][] ARestricted = new int[m][columnAmount];
            for (int i = 0; i < m; i++) {
                for (int j = 0; j < columnAmount; j++) {
                    ARestricted[i][j] = A[i][colPositions[j]];
                }
            }
            hereditaryDiscrepancy = Math.max(hereditaryDiscrepancy, discrepancy(ARestricted));
        }
        return hereditaryDiscrepancy;
    }

    // Example used for testing/debugging
    public static void main(String[] args) {
        int[][] A = {
                {3, 2, 0, 2, 4, 2, 3},
                {1, 1, 1, 0, 0, 0, 0},
                {0, 0, 0, 1, 1, 0, 0},
                {0, 0, 0, 0, 0, 1, 1}
        };

        System.out.println("discrepancy = " + discrepancy(A));
        System.out.println("hereditary discrepancy = " + hereditaryDiscrepancy(A));
    }
}