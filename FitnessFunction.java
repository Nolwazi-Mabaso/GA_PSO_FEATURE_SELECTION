import java.util.List;

public class FitnessFunction {

    private final KNN knn;
    private final double alpha; 

    public FitnessFunction(int k) {
        this(k, 0.3); 
    }

    public FitnessFunction(int k, double alpha) {
        this.knn = new KNN(k);
        this.alpha = alpha;
    }

    public double calculateFitness(
            double[][] trainData,
            int[] trainLabels,
            double[][] validationData,
            int[] validationLabels,
            List<Integer> selectedFeatures,
            int totalFeatureCount) {

        if (selectedFeatures == null || selectedFeatures.isEmpty()) {
            return 0.0;
        }

        Evaluation evaluation = knn.evaluate(
                trainData,
                trainLabels,
                validationData,
                validationLabels,
                selectedFeatures
        );

        double f1 = evaluation.calculateF1Score();
        double gMean = evaluation.calculateGMean();

        double baseFitness = (f1 + gMean) / 2.0;

        double featureRatio = (double) selectedFeatures.size() / totalFeatureCount;
        double penalty = alpha * featureRatio;

        return baseFitness - penalty;
    }
}