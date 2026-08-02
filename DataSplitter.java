import java.util.*;

public class DataSplitter {
    private final Random random = new Random(42);

    // Splits into train/test STRATIFIED by class label, so both classes
    // are represented proportionally in both sets, instead of trusting
    // a plain random shuffle to preserve a rare minority class.
    // trainRatio e.g. 0.7 = 70% train, 30% test.
    public DataSplit splitData(List<String[]> data, double trainRatio) {

        Map<String, List<String[]>> byClass = new HashMap<>();

        for (String[] row : data) {
            String label = row[row.length - 1];
            byClass.computeIfAbsent(label, key -> new ArrayList<>()).add(row);
        }

        List<String[]> trainSet = new ArrayList<>();
        List<String[]> testSet = new ArrayList<>();

        for (List<String[]> rowsForClass : byClass.values()) {

            List<String[]> shuffled = new ArrayList<>(rowsForClass);
            Collections.shuffle(shuffled, random);

            int trainSize = (int) (shuffled.size() * trainRatio);

            // Guarantee at least one example of this class in BOTH sets,
            // as long as the class has at least 2 rows total. Without this,
            // a rare class with e.g. 1 row could vanish from one side entirely.
            if (shuffled.size() >= 2) {
                trainSize = Math.max(1, Math.min(trainSize, shuffled.size() - 1));
            }

            trainSet.addAll(shuffled.subList(0, trainSize));
            testSet.addAll(shuffled.subList(trainSize, shuffled.size()));
        }

        Collections.shuffle(trainSet, random);
        Collections.shuffle(testSet, random);

        return new DataSplit(trainSet, testSet);
    }
}