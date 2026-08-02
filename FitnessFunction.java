import java.util.List;

public class FitnessFunction {

    private final KNN knn;

    public FitnessFunction(int k) {

        this.knn = new KNN(k);

    }



    public double calculateFitness(
            double[][] trainData,
            int[] trainLabels,
            double[][] validationData,
            int[] validationLabels,
            List<Integer> selectedFeatures) {


        Evaluation evaluation = knn.evaluate(
                trainData,
                trainLabels,
                validationData,
                validationLabels,
                selectedFeatures
        );


        double f1 = evaluation.calculateF1Score();

        double gMean = evaluation.calculateGMean();


        return (f1 + gMean) / 2.0;
    }
}