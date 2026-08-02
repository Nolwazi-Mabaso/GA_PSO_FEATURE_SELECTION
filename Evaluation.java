public class Evaluation {


    private final int truePositive;
    private final int trueNegative;
    private final int falsePositive;
    private final int falseNegative;



    public Evaluation(
            int truePositive,
            int trueNegative,
            int falsePositive,
            int falseNegative) {


        this.truePositive = truePositive;
        this.trueNegative = trueNegative;
        this.falsePositive = falsePositive;
        this.falseNegative = falseNegative;

    }




    public double calculateF1Score() {


        double precision = calculatePrecision();
        double recall = calculateRecall();



        if (precision + recall == 0) {

            return 0;

        }


        return 2 * (precision * recall)
                /
                (precision + recall);
    }





    private double calculatePrecision() {


        if (truePositive + falsePositive == 0) {

            return 0;

        }


        return (double) truePositive /
                (truePositive + falsePositive);

    }





    private double calculateRecall() {


        if (truePositive + falseNegative == 0) {

            return 0;

        }


        return (double) truePositive /
                (truePositive + falseNegative);

    }





    public double calculateGMean() {


        double sensitivity = calculateRecall();


        double specificity;


        if (trueNegative + falsePositive == 0) {

            specificity = 0;

        } else {

            specificity = (double) trueNegative /
                    (trueNegative + falsePositive);

        }



        return Math.sqrt(
                sensitivity * specificity
        );

    }





    public int getTruePositive() {
        return truePositive;
    }


    public int getTrueNegative() {
        return trueNegative;
    }


    public int getFalsePositive() {
        return falsePositive;
    }


    public int getFalseNegative() {
        return falseNegative;
    }
}