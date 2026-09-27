import java.util.*;

public class PearsonCorrelation {

    private double threshold;

    public PearsonCorrelation(double threshold) {
        this.threshold = threshold;
    }

    public List<List<Integer>> groupFeatures(List<String[]> data) {

        int totalColumns = data.get(0).length;
        int featureCount = totalColumns - 1; 
        int numRows = data.size();

        double[][] values = new double[numRows][featureCount];

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < featureCount; j++) {
                values[i][j] = Double.parseDouble(data.get(i)[j]); 
            }
        }

        List<List<Integer>> groups = new ArrayList<>();
        boolean[] grouped = new boolean[featureCount];

        for (int i = 0; i < featureCount; i++) {

            if (grouped[i]) {
                continue;
            }

            List<Integer> group = new ArrayList<>();
            group.add(i);

            for (int j = i + 1; j < featureCount; j++) {

                if (Math.abs(calculateCorrelation(values, i, j)) >= threshold) {
                    group.add(j);
                    grouped[j] = true;
                }
            }

            groups.add(group);
        }

        return groups;
    }

    private double calculateCorrelation(double[][] data, int x, int y) {

        double meanX = 0;
        double meanY = 0;

        for (double[] row : data) {
            meanX += row[x];
            meanY += row[y];
        }

        meanX /= data.length;
        meanY /= data.length;

        double numerator = 0;
        double denominatorX = 0;
        double denominatorY = 0;

        for (double[] row : data) {

            double xValue = row[x] - meanX;
            double yValue = row[y] - meanY;

            numerator += xValue * yValue;
            denominatorX += xValue * xValue;
            denominatorY += yValue * yValue;
        }

        if (denominatorX == 0 || denominatorY == 0) {
            return 0;
        }

        return numerator / Math.sqrt(denominatorX * denominatorY);
    }

    public static void printFeatureGroups(List<List<Integer>> featureGroups) {
        System.out.println("\n");
        System.out.println("            FEATURE GROUPS:            ");
        System.out.println("");
        System.out.println("Total Groups Formed: " + featureGroups.size());
        System.out.println("--------------------------------------------------");

        for (int i = 0; i < featureGroups.size(); i++) {
            List<Integer> group = featureGroups.get(i);
            
            if (group.size() == 1) {
                System.out.println("Group " + (i + 1) + " [Independent] -- Column " + group.get(0));
            } else {
                System.out.println("Group " + (i + 1) + " [" + group.size() + " correlated features] -- Columns: " + group);
            }
        }
        System.out.println("_______________________________________________\n");
    }
}