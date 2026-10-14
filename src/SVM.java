import java.util.List;

public class SVM {

    private final double c;
    private final int maxIterations;
    private final double learningRate;

    public SVM() {
        this.c = 0.1;
        this.maxIterations = 200;
        this.learningRate = 0.001;
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

        // Convert labels to ±1 and compute class weights
        int[] yTrain = new int[trainLabels.length];
        int nPos = 0, nNeg = 0;
        for (int i = 0; i < trainLabels.length; i++) {
            if (trainLabels[i] == 1) { yTrain[i] = 1;  nPos++; }
            else                     { yTrain[i] = -1; nNeg++; }
        }

        double posWeight = (nPos > 0) ? (double)(nPos + nNeg) / (2.0 * nPos) : 1.0;
        double negWeight = (nNeg > 0) ? (double)(nPos + nNeg) / (2.0 * nNeg) : 1.0;

        double[] w = new double[nFeatures];
        double b = 0.0;

        for (int iter = 0; iter < maxIterations; iter++) {

            double eta = learningRate / (1.0 + learningRate * iter);

            double[] gradW = new double[nFeatures];
            double gradB = 0.0;

            for (int i = 0; i < Xtrain.length; i++) {
                double dot = b;
                for (int j = 0; j < nFeatures; j++) {
                    dot += w[j] * Xtrain[i][j];
                }
                double margin = yTrain[i] * dot;

                if (margin < 1) {
                    double weight = (yTrain[i] == 1) ? posWeight : negWeight;
                    for (int j = 0; j < nFeatures; j++) {
                        gradW[j] -= c * yTrain[i] * Xtrain[i][j] * weight;
                    }
                    gradB -= c * yTrain[i] * weight;
                }
            }

            for (int j = 0; j < nFeatures; j++) {
                w[j] = w[j] - eta * (w[j] + gradW[j]);
            }
            b = b - eta * gradB;
        }

        int tp = 0, tn = 0, fp = 0, fn = 0;

        for (int i = 0; i < Xtest.length; i++) {
            double dot = b;
            for (int j = 0; j < nFeatures; j++) {
                dot += w[j] * Xtest[i][j];
            }
            int pred = dot >= 0 ? 1 : 0;
            int actual = testLabels[i];

            if (pred == 1 && actual == 1) tp++;
            else if (pred == 0 && actual == 0) tn++;
            else if (pred == 1 && actual == 0) fp++;
            else fn++;
        }

        return new Evaluation(tp, tn, fp, fn);
    }
}