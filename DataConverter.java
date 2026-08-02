import java.util.List;

/*
 * Converts rows of String[] (as produced by DataSplitter / ValidationSplitter)
 * into the numeric double[][] features and int[] labels that KNN and
 * FitnessFunction expect.
 *
 * ASSUMPTION: the LAST column in each row is the class label (0 or 1),
 * and every other column is an already-preprocessed numeric feature.
 * If your label lives in a different column, change labelIndex below.
 */
public class DataConverter {

    public static double[][] toFeatureArray(List<String[]> rows) {
        int numFeatures = rows.get(0).length - 1; // last column excluded
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
        int labelIndex = rows.get(0).length - 1; // last column
        int[] labels = new int[rows.size()];

        for (int i = 0; i < rows.size(); i++) {
            labels[i] = (int) Double.parseDouble(rows.get(i)[labelIndex]);
        }

        return labels;
    }
}