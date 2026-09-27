import java.util.List;
import java.util.Random;

public class DecisionTree {

    private final int maxDepth;
    private final int minSamplesSplit;
    private final int nFeaturesPerSplit;  // -1 means use all
    private final long seed;

    private Node root;

    public DecisionTree() {
        this(15, 5, -1, 42);
    }

    public DecisionTree(int maxDepth, int minSamplesSplit, int nFeaturesPerSplit, long seed) {
        this.maxDepth = maxDepth;
        this.minSamplesSplit = minSamplesSplit;
        this.nFeaturesPerSplit = nFeaturesPerSplit;
        this.seed = seed;
    }

    public Evaluation evaluate(double[][] trainData, int[] trainLabels,
                               double[][] testData, int[] testLabels,
                               List<Integer> selectedFeatures) {

        int nFeatures = selectedFeatures.size();

        double[][] Xtrain = new double[trainData.length][nFeatures];
        double[][] Xtest  = new double[testData.length][nFeatures];

        for (int i = 0; i < trainData.length; i++)
            for (int j = 0; j < nFeatures; j++)
                Xtrain[i][j] = trainData[i][selectedFeatures.get(j)];

        for (int i = 0; i < testData.length; i++)
            for (int j = 0; j < nFeatures; j++)
                Xtest[i][j] = testData[i][selectedFeatures.get(j)];

        int[] allIndices = new int[trainData.length];
        for (int i = 0; i < trainData.length; i++) allIndices[i] = i;

        root = build(Xtrain, trainLabels, allIndices, 0, new Random(seed));

        int tp = 0, tn = 0, fp = 0, fn = 0;

        for (int i = 0; i < Xtest.length; i++) {
            int pred = predict(Xtest[i]);
            int actual = testLabels[i];

            if (pred == 1 && actual == 1) tp++;
            else if (pred == 0 && actual == 0) tn++;
            else if (pred == 1 && actual == 0) fp++;
            else fn++;
        }

        return new Evaluation(tp, tn, fp, fn);
    }

    private int predict(double[] x) {
        Node node = root;
        while (node.featureIndex != -1) {
            node = (x[node.featureIndex] <= node.threshold) ? node.left : node.right;
        }
        return node.label;
    }

    private Node build(double[][] X, int[] y, int[] indices, int depth, Random rng) {
        Node node = new Node();

        int n0 = 0, n1 = 0;
        for (int i : indices) {
            if (y[i] == 0) n0++; else n1++;
        }

        // Stopping conditions
        if (depth >= maxDepth || indices.length < minSamplesSplit || n0 == 0 || n1 == 0) {
            node.featureIndex = -1;
            node.label = n1 > n0 ? 1 : 0;
            return node;
        }

        int nFeatures = X[0].length;
        int nTry = (nFeaturesPerSplit > 0) ? Math.min(nFeaturesPerSplit, nFeatures) : nFeatures;

        double bestGini = Double.MAX_VALUE;
        int bestFeature = -1;
        double bestThreshold = 0.0;
        int[] bestLeftIdx = null;
        int[] bestRightIdx = null;

        // For each feature, try thresholds at random data points
        for (int f = 0; f < nTry; f++) {
            int feature = (nTry == nFeatures) ? f : rng.nextInt(nFeatures);

            // Sample up to 10 candidate thresholds
            int nAttempts = Math.min(10, indices.length);
            for (int a = 0; a < nAttempts; a++) {
                double threshold = X[indices[rng.nextInt(indices.length)]][feature];

                int lc0 = 0, lc1 = 0, rc0 = 0, rc1 = 0;
                for (int i : indices) {
                    if (X[i][feature] <= threshold) {
                        if (y[i] == 0) lc0++; else lc1++;
                    } else {
                        if (y[i] == 0) rc0++; else rc1++;
                    }
                }

                if (lc0 + lc1 == 0 || rc0 + rc1 == 0) continue;

                double gini = weightedGini(lc0, lc1, rc0, rc1);
                if (gini < bestGini) {
                    bestGini = gini;
                    bestFeature = feature;
                    bestThreshold = threshold;

                    int nL = lc0 + lc1;
                    int nR = rc0 + rc1;
                    bestLeftIdx  = new int[nL];
                    bestRightIdx = new int[nR];
                    int li = 0, ri = 0;
                    for (int i : indices) {
                        if (X[i][feature] <= threshold) bestLeftIdx[li++] = i;
                        else bestRightIdx[ri++] = i;
                    }
                }
            }
        }

        if (bestFeature == -1) {
            node.featureIndex = -1;
            node.label = n1 > n0 ? 1 : 0;
            return node;
        }

        node.featureIndex = bestFeature;
        node.threshold = bestThreshold;
        node.left  = build(X, y, bestLeftIdx,  depth + 1, rng);
        node.right = build(X, y, bestRightIdx, depth + 1, rng);
        return node;
    }

    private double weightedGini(int lc0, int lc1, int rc0, int rc1) {
        double giniL = 1.0
                - Math.pow((double) lc0 / (lc0 + lc1), 2)
                - Math.pow((double) lc1 / (lc0 + lc1), 2);
        double giniR = 1.0
                - Math.pow((double) rc0 / (rc0 + rc1), 2)
                - Math.pow((double) rc1 / (rc0 + rc1), 2);
        int total = lc0 + lc1 + rc0 + rc1;
        return ((lc0 + lc1) * giniL + (rc0 + rc1) * giniR) / total;
    }

    static class Node {
        int featureIndex;
        double threshold;
        int label;
        Node left, right;
    }
}