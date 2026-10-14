import java.util.*;

public class FitnessFunction {

    private final KNN knn;
    private final SVM svm;
    private final NaiveBayes nb;
    private final String classifierType;

    public FitnessFunction(int k) {
        this(k, "KNN");
    }

    public FitnessFunction(int k, String classifierType) {
        this.knn = new KNN(k);
        this.svm = new SVM();
        this.nb  = new NaiveBayes();
        this.classifierType = classifierType;
    }

    public double calculateFitnessCV(
            double[][] trainData,
            int[] trainLabels,
            List<Integer> selectedFeatures,
            int folds) {

        if (selectedFeatures == null || selectedFeatures.isEmpty()) {
            return 0.0;
        }

        int n = trainData.length;

        // ---- Stratified fold assignment ----
        List<Integer> pos = new ArrayList<>();
        List<Integer> neg = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (trainLabels[i] == 1) pos.add(i);
            else                     neg.add(i);
        }

        Random rng = new Random(42);
        Collections.shuffle(pos, rng);
        Collections.shuffle(neg, rng);

        int[] foldOf = new int[n];
        for (int i = 0; i < pos.size(); i++) foldOf[pos.get(i)] = i % folds;
        for (int i = 0; i < neg.size(); i++) foldOf[neg.get(i)] = i % folds;

        double totalGMean = 0.0;
        int validFolds = 0;

        for (int f = 0; f < folds; f++) {
            List<double[]> foldTrainX = new ArrayList<>();
            List<Integer>  foldTrainY = new ArrayList<>();
            List<double[]> foldTestX  = new ArrayList<>();
            List<Integer>  foldTestY  = new ArrayList<>();

            for (int i = 0; i < n; i++) {
                if (foldOf[i] == f) {
                    foldTestX.add(trainData[i]);
                    foldTestY.add(trainLabels[i]);
                } else {
                    foldTrainX.add(trainData[i]);
                    foldTrainY.add(trainLabels[i]);
                }
            }

            // Skip folds with only one class in test set
            Set<Integer> unique = new HashSet<>(foldTestY);
            if (unique.size() < 2 || foldTestX.isEmpty()) continue;

            double[][] trX = toArray(foldTrainX);
            int[] trY = toIntArray(foldTrainY);
            double[][] teX = toArray(foldTestX);
            int[] teY = toIntArray(foldTestY);

            Evaluation eval;
            if (classifierType.equals("SVM")) {
                eval = svm.evaluate(trX, trY, teX, teY, selectedFeatures);
            } else if (classifierType.equals("NB")) {
                eval = nb.evaluate(trX, trY, teX, teY, selectedFeatures);
            } else {
                eval = knn.evaluate(trX, trY, teX, teY, selectedFeatures);
            }

            totalGMean += eval.calculateGMean();
            validFolds++;
        }

        if (validFolds == 0) return 0.0;

        return totalGMean / validFolds;
    }

    private double[][] toArray(List<double[]> list) {
        double[][] arr = new double[list.size()][];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }

    private int[] toIntArray(List<Integer> list) {
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
}