import java.io.*;

public class InstanceParser {
    // Parsing method
    public ILPInstance[] parseFile(String filePath) throws IOException {
        // Reading the file
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Parsing the parameters responsible for generating the instance
            String inputParameters = br.readLine();
            String[] parameters = inputParameters.split("\\s+");
            int[] paramValues = new int[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                paramValues[i] = Integer.parseInt(parameters[i]);
            }
            // Number of instances to generate
            int instances = Integer.parseInt(br.readLine());
            ILPInstance[] result = new ILPInstance[instances];
            // Generating all instances
            for (int i = 0; i < instances; i++) {


                // Parsing input value data
                String inputValues = br.readLine();
                String[] values = inputValues.split("\\s+");

                // Input values for our ILP instance
                int n = Integer.parseInt(values[0]);
                int r = Integer.parseInt(values[1]);
                int h = Integer.parseInt(values[2]);

                // Parsing the single line vectors from the input file
                String tVector = br.readLine();
                int[] t = parseVector(tVector);
                String cVector = br.readLine();
                int[] c = parseVector(cVector);
                String rhsVector = br.readLine();
                int[] rhs = parseVector(rhsVector);

                // Parse the matrix
                int[][] matrix = new int[r+n][h];
                for (int j = 0; j < r+n; j++) {
                    String row = br.readLine();
                    String[] rowValues = row.split("\\s+");
                    for (int k = 0; k < h; k++) {
                        matrix[j][k] = Integer.parseInt(rowValues[k]);
                    }
                }
                // Creating and adding the instance
                ILPInstance ilp = new ILPInstance(paramValues, n, r, h, t, c, rhs, matrix);
                result[i] = ilp;
                // Reading empty line that separates the instances
                br.readLine();

            }
            return result;
        }
    }

    // Parses a string vector that contains integers values separated by white space
    // Returns a list of integers that represent the vector
    private int[] parseVector(String vector) {
        String[] vectorValues = vector.split("\\s+");
        int[] result = new int[vectorValues.length];
        for (int i = 0; i < vectorValues.length; i++) {
            result[i] = Integer.parseInt(vectorValues[i]);
        }
        return result;
    }
}