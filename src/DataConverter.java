import java.util.List;

public class DataConverter {

    public static double[][] toFeatureArray(List<String[]> rows) {
        int numFeatures = rows.get(0).length - 1; 
        double[][] features = new double[rows.size()][numFeatures];

        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            for (int f = 0; f < numFeatures; f++) {
                features[i][f] = Double.parseDouble(row[f]);
            }
        }

        return features;
    }

    public static int[] toLabelArray(List<String[]> rows) {
        int labelIndex = rows.get(0).length - 1; 
        int[] labels = new int[rows.size()];

        for (int i = 0; i < rows.size(); i++) {
            labels[i] = (int) Double.parseDouble(rows.get(i)[labelIndex]);
        }

        return labels;
    }
}