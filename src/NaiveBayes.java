import java.util.List;

public class NaiveBayes {

    public Evaluation evaluate(double[][] trainData, int[] trainLabels,
                               double[][] testData, int[] testLabels,
                               List<Integer> selectedFeatures) {

        int nFeatures = selectedFeatures.size();
        int nClasses = 2;

        double[][] means = new double[nClasses][nFeatures];
        double[][] variances = new double[nClasses][nFeatures];
        double[] priors = new double[nClasses];
        int[] classCounts = new int[nClasses];

        // Compute means per class per feature
        for (int i = 0; i < trainData.length; i++) {
            int cls = trainLabels[i];
            classCounts[cls]++;
            for (int j = 0; j < nFeatures; j++) {
                means[cls][j] += trainData[i][selectedFeatures.get(j)];
            }
        }

        for (int c = 0; c < nClasses; c++) {
            for (int j = 0; j < nFeatures; j++) {
                if (classCounts[c] > 0) means[c][j] /= classCounts[c];
            }
            priors[c] = (double) classCounts[c] / trainData.length;
        }

        // Compute variances
        for (int i = 0; i < trainData.length; i++) {
            int cls = trainLabels[i];
            for (int j = 0; j < nFeatures; j++) {
                double diff = trainData[i][selectedFeatures.get(j)] - means[cls][j];
                variances[cls][j] += diff * diff;
            }
        }

        for (int c = 0; c < nClasses; c++) {
            for (int j = 0; j < nFeatures; j++) {
                if (classCounts[c] > 1) variances[c][j] /= (classCounts[c] - 1);
                variances[c][j] += 1e-9;
            }
        }

        // Predict
        int tp = 0, tn = 0, fp = 0, fn = 0;

        for (int i = 0; i < testData.length; i++) {
            double[] logPost = new double[nClasses];

            for (int c = 0; c < nClasses; c++) {
                double logProb = Math.log(priors[c] + 1e-9);
                for (int j = 0; j < nFeatures; j++) {
                    double x = testData[i][selectedFeatures.get(j)];
                    double mu = means[c][j];
                    double var = variances[c][j];
                    logProb += -0.5 * Math.log(2 * Math.PI * var)
                             - ((x - mu) * (x - mu)) / (2 * var);
                }
                logPost[c] = logProb;
            }

            int pred = logPost[1] > logPost[0] ? 1 : 0;
            int actual = testLabels[i];

            if (pred == 1 && actual == 1) tp++;
            else if (pred == 0 && actual == 0) tn++;
            else if (pred == 1 && actual == 0) fp++;
            else fn++;
        }

        return new Evaluation(tp, tn, fp, fn);
    }
}