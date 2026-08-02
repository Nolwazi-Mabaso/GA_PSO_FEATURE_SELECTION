import java.util.List;


public class DataSplit {

    private final List<String[]> trainSet;
    private final List<String[]> testSet;



    public DataSplit(
            List<String[]> trainSet,
            List<String[]> testSet) {

        this.trainSet = trainSet;
        this.testSet = testSet;

    }

    public List<String[]> getTrainSet() {

        return trainSet;

    }


    public List<String[]> getTestSet() {

        return testSet;

    }
}