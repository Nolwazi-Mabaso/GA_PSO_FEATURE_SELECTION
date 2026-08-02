import java.util.*;


public class KNN {

    private final int k;


    public KNN(int k) {
        this.k = k;
    }



    // Predict one sample
    public int predict(
            double[][] trainData,
            int[] trainLabels,
            double[] testSample,
            List<Integer> selectedFeatures) {


        List<Neighbor> neighbors = new ArrayList<>();


        // Calculate distance from test sample to every training sample
        for (int i = 0; i < trainData.length; i++) {


            double distance = calculateDistance(
                    trainData[i],
                    testSample,
                    selectedFeatures
            );


            neighbors.add(
                    new Neighbor(distance, trainLabels[i])
            );
        }



        // Sort closest neighbours first
        Collections.sort(neighbors);



        int countClass0 = 0;
        int countClass1 = 0;



        // Voting
        for (int i = 0; i < k; i++) {


            if (neighbors.get(i).label == 1) {

                countClass1++;

            } else {

                countClass0++;

            }
        }



        return countClass1 > countClass0 ? 1 : 0;
    }




    // Euclidean distance using selected chromosome features
    private double calculateDistance(
            double[] point1,
            double[] point2,
            List<Integer> selectedFeatures) {


        double sum = 0;


        for (int feature : selectedFeatures) {


            double difference =
                    point1[feature] - point2[feature];


            sum += difference * difference;

        }


        return Math.sqrt(sum);
    }




    // Evaluate many samples
    public Evaluation evaluate(
            double[][] trainData,
            int[] trainLabels,
            double[][] validationData,
            int[] validationLabels,
            List<Integer> selectedFeatures) {


        int truePositive = 0;
        int trueNegative = 0;
        int falsePositive = 0;
        int falseNegative = 0;



        for (int i = 0; i < validationData.length; i++) {


            int prediction = predict(
                    trainData,
                    trainLabels,
                    validationData[i],
                    selectedFeatures
            );


            int actual = validationLabels[i];



            if (prediction == 1 && actual == 1) {

                truePositive++;

            } 
            else if (prediction == 0 && actual == 0) {

                trueNegative++;

            } 
            else if (prediction == 1 && actual == 0) {

                falsePositive++;

            } 
            else {

                falseNegative++;

            }

        }



        return new Evaluation(
                truePositive,
                trueNegative,
                falsePositive,
                falseNegative
        );
    }




    // Stores neighbour information
    private static class Neighbor implements Comparable<Neighbor> {


        double distance;
        int label;



        Neighbor(double distance, int label) {

            this.distance = distance;
            this.label = label;

        }



        @Override
        public int compareTo(Neighbor other) {

            return Double.compare(
                    this.distance,
                    other.distance
            );
        }
    }
}