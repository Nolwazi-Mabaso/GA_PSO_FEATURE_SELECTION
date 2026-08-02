import java.util.*;

public class ValidationSplitter {
    private final Random random = new Random(42);

    // Splits the TRAIN set into an inner training portion and a validation
    // portion, STRATIFIED by class label so the GA's fitness evaluation
    // actually sees both classes, instead of risking a validation slice
    // with zero minority-class examples (which forces G-mean to 0 always).
    public ValidationSplit splitValidation(List<String[]> trainSet, double trainingRatio) {

        Map<String, List<String[]>> byClass = new HashMap<>();

        for (String[] row : trainSet) {
            String label = row[row.length - 1];
            byClass.computeIfAbsent(label, key -> new ArrayList<>()).add(row);
        }

        List<String[]> training = new ArrayList<>();
        List<String[]> validation = new ArrayList<>();

        for (List<String[]> rowsForClass : byClass.values()) {

            List<String[]> shuffled = new ArrayList<>(rowsForClass);
            Collections.shuffle(shuffled, random);

            int trainingSize = (int) (shuffled.size() * trainingRatio);

            // Guarantee at least one example of this class in BOTH sides,
            // as long as the class has at least 2 rows total.
            if (shuffled.size() >= 2) {
                trainingSize = Math.max(1, Math.min(trainingSize, shuffled.size() - 1));
            }

            training.addAll(shuffled.subList(0, trainingSize));
            validation.addAll(shuffled.subList(trainingSize, shuffled.size()));
        }

        Collections.shuffle(training, random);
        Collections.shuffle(validation, random);

        return new ValidationSplit(training, validation);
    }
}