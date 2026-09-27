import java.util.*;

public class RemoveDuplicates {

    public List<String[]> remove(List<String[]> data) {

        List<String[]> cleanedData = new ArrayList<>();
        Set<String> seenRows = new HashSet<>();

        for (String[] row : data) {

            String key = Arrays.toString(row);

            if (seenRows.add(key)) {
                cleanedData.add(row);
            }
        }

        return cleanedData;
    }
}