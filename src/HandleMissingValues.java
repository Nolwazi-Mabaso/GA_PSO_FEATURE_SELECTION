import java.util.*;

public class HandleMissingValues {

    public List<String[]> clean(List<String[]> data) {

        if (data.isEmpty()) {
            return data;
        }

        int cols = data.get(0).length;

        double[] sum = new double[cols];
        int[] count = new int[cols];

        for (String[] row : data) {

            for (int i = 0; i < cols; i++) {

                String value = row[i];

                if (isMissing(value)) {
                    continue;
                }

                try {
                    double num =
                            Double.parseDouble(value);

                    sum[i] += num;
                    count[i]++;

                } catch (NumberFormatException e) {
                    
                }
            }
        }

        double[] mean = new double[cols];

        for (int i = 0; i < cols; i++) {

            if (count[i] == 0) {
                mean[i] = 0;
            } else {
                mean[i] = sum[i] / count[i];
            }
        }

        List<String[]> cleaned =
                new ArrayList<>();

        for (String[] row : data) {

            String[] newRow =
                    new String[cols];

            for (int i = 0; i < cols; i++) {

                String value = row[i];

                if (isMissing(value)) {

                    newRow[i] =
                            String.valueOf(mean[i]);

                } else {
                    newRow[i] = value;
                }
            }

            cleaned.add(newRow);
        }

        System.out.println(
                "Missing values replaced using mean imputation."
        );

        return cleaned;
    }

    private boolean isMissing(String value) {

        return value == null ||
               value.trim().isEmpty() ||
               value.equalsIgnoreCase("NA") ||
               value.equalsIgnoreCase("null") ||
               value.equals("?");

    }
}