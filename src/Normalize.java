import java.util.*;

public class Normalize {

    public List<String[]> scale(List<String[]> data) {

        int columns = data.get(0).length;
        int features = columns - 1;

        double[] min = new double[features];
        double[] max = new double[features];

        Arrays.fill(min, Double.MAX_VALUE);
        Arrays.fill(max, Double.MIN_VALUE);


        for (String[] row : data) {

            for (int i = 0; i < features; i++) {

                double value = Double.parseDouble(row[i]);

                if (value < min[i]) {
                    min[i] = value;
                }

                if (value > max[i]) {
                    max[i] = value;
                }
            }
        }


        List<String[]> normalized = new ArrayList<>();

        for (String[] row : data) {

            String[] newRow = new String[columns];

            for (int i = 0; i < features; i++) {

                double value = Double.parseDouble(row[i]);

                if (max[i] == min[i]) {
                    newRow[i] = "0";
                } else {

                    double scaled =
                            (value - min[i]) /
                            (max[i] - min[i]);

                    newRow[i] = String.valueOf(scaled);
                }
            }

            newRow[columns - 1] = row[columns - 1];

            normalized.add(newRow);
        }

        return normalized;
    }
}