import java.util.List;

public class LogisticRegression {

    private final double learningRate;
    private final int maxIterations;
    private final double l2;

    public LogisticRegression() {
        this(0.1, 500, 0.01);
    }

    public LogisticRegression(double learningRate, int maxIterations, double l2) {
        this.learningRate = learningRate;
        this.maxIterations = maxIterations;
        this.l2 = l2;
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

        // Compute class weights for imbalance
        int nPos = 0, nNeg = 0;
        for (int label : trainLabels) {
            if (label == 1) nPos++; else nNeg++;
        }
        double posWeight = (nPos > 0) ? (double)(nPos + nNeg) / (2.0 * nPos) : 1.0;
        double negWeight = (nNeg > 0) ? (double)(nPos + nNeg) / (2.0 * nNeg) : 1.0;

        // Weights and bias
        double[] w = new double[nFeatures];
        double b = 0.0;

        for (int iter = 0; iter < maxIterations; iter++) {

            double[] gradW = new double[nFeatures];
            double gradB = 0.0;

            for (int i = 0; i < Xtrain.length; i++) {
                double z = b;
                for (int j = 0; j < nFeatures; j++) {
                    z += w[j] * Xtrain[i][j];
                }
                double p = sigmoid(z);
                double y = trainLabels[i];
                double weight = (y == 1) ? posWeight : negWeight;
                double error = (p - y) * weight;

                for (int j = 0; j < nFeatures; j++) {
                    gradW[j] += error * Xtrain[i][j];
                }
                gradB += error;
            }

            int n = Xtrain.length;
            for (int j = 0; j < nFeatures; j++) {
                w[j] -= learningRate * (gradW[j] / n + l2 * w[j]);
            }
            b -= learningRate * (gradB / n);
        }

        // Predict
        int tp = 0, tn = 0, fp = 0, fn = 0;

        for (int i = 0; i < Xtest.length; i++) {
            double z = b;
            for (int j = 0; j < nFeatures; j++) {
                z += w[j] * Xtest[i][j];
            }
            int pred = z >= 0 ? 1 : 0;
            int actual = testLabels[i];

            if (pred == 1 && actual == 1) tp++;
            else if (pred == 0 && actual == 0) tn++;
            else if (pred == 1 && actual == 0) fp++;
            else fn++;
        }

        return new Evaluation(tp, tn, fp, fn);
    }

    private double sigmoid(double z) {
        if (z >= 0) {
            return 1.0 / (1.0 + Math.exp(-z));
        } else {
            double ez = Math.exp(z);
            return ez / (1.0 + ez);
        }
    }
}