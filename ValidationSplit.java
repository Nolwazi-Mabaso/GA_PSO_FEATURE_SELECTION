import java.util.List;

public class ValidationSplit {
    private final List<String[]> trainingSet;
    private final List<String[]> validationSet;

    public ValidationSplit(
            List<String[]> trainingSet,
            List<String[]> validationSet) {
        this.trainingSet = trainingSet;
        this.validationSet = validationSet;

    }

    public List<String[]> getTrainingSet() {

        return trainingSet;

    }

    public List<String[]> getValidationSet() {

        return validationSet;

    }
}