import java.util.List;

public class FitnessFunction {

    private final KNN knn;
    private final DecisionTree tree;
    private final LogisticRegression lr;
    private final String classifierType;

    public FitnessFunction(int k) {
        this(k, "KNN");
    }

    public FitnessFunction(int k, String classifierType) {
        this.knn = new KNN(k);
        this.tree = new DecisionTree();
        this.lr = new LogisticRegression();
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
    int[] idx = new int[n];
    for (int i = 0; i < n; i++) idx[i] = i;

    java.util.Random rng = new java.util.Random(42);
    for (int i = n - 1; i > 0; i--) {
        int j = rng.nextInt(i + 1);
        int tmp = idx[i]; idx[i] = idx[j]; idx[j] = tmp;
    }

    double totalGMean = 0.0;

    for (int f = 0; f < folds; f++) {
        int foldStart = (int) ((double) f       / folds * n);
        int foldEnd   = (int) ((double) (f + 1) / folds * n);

        java.util.List<double[]> foldTestXList = new java.util.ArrayList<>();
        java.util.List<Integer> foldTestYList  = new java.util.ArrayList<>();
        java.util.List<double[]> foldTrainXList = new java.util.ArrayList<>();
        java.util.List<Integer> foldTrainYList  = new java.util.ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (i >= foldStart && i < foldEnd) {
                foldTestXList.add(trainData[idx[i]]);
                foldTestYList.add(trainLabels[idx[i]]);
            } else {
                foldTrainXList.add(trainData[idx[i]]);
                foldTrainYList.add(trainLabels[idx[i]]);
            }
        }

        double[][] foldTrainX = foldTrainXList.toArray(new double[0][]);
        int[] foldTrainY = foldTrainYList.stream().mapToInt(Integer::intValue).toArray();
        double[][] foldTestX = foldTestXList.toArray(new double[0][]);
        int[] foldTestY = foldTestYList.stream().mapToInt(Integer::intValue).toArray();

        Evaluation eval;
        if (classifierType.equals("TREE")) {
            eval = tree.evaluate(foldTrainX, foldTrainY, foldTestX, foldTestY, selectedFeatures);
        } else if (classifierType.equals("LR")) {
            eval = lr.evaluate(foldTrainX, foldTrainY, foldTestX, foldTestY, selectedFeatures);
        } else {
            eval = knn.evaluate(foldTrainX, foldTrainY, foldTestX, foldTestY, selectedFeatures);
        }

        totalGMean += eval.calculateGMean();
    }

    return totalGMean / folds;
}
}