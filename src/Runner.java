import java.util.ArrayList;
import java.util.List;

public class Runner {

    private final String filePath;
    private final LogSink sink;
    private final List<String> classifiersToRun;

    // ---- exposed to WebServer ----
    public int totalOriginalFeatures;
    public int numGroups;
    public List<Result> results = new ArrayList<>();
    public List<BaselineResult> baselineResults = new ArrayList<>();

    // ---- result containers ----
    public static class Result {
        public String classifierName;
        public Evaluation gaEval;
        public Evaluation psoEval;
        public long gaDurationMs;
        public long psoDurationMs;
        public List<Integer> bestGaFeatures;
        public List<Integer> bestPsoFeatures;
    }

    public static class BaselineResult {
        public String classifierName;
        public Evaluation eval;
        public long durationMs;
        public int numFeatures;
    }

    // ---- constructors ----
    public Runner(String filePath, LogSink sink) {
        this(filePath, sink, List.of("KNN", "TREE", "LR"));
    }

    public Runner(String filePath, LogSink sink, List<String> classifiersToRun) {
        this.filePath = filePath;
        this.sink = sink;
        this.classifiersToRun = classifiersToRun;
    }

    // ================================================================
    //  MAIN PIPELINE
    // ================================================================
    public void run() throws Exception {

        sink.log("             DATA LOADING & PREPROCESSING         ");
        sink.log("___________________________________________________");

        LoadData loader = new LoadData(filePath);
        if (!loader.load()) {
            sink.log("Could not load dataset.");
            return;
        }

        List<String[]> rawData = loader.getData();
        sink.log("Data loaded successfully.");
        sink.log("Rows loaded: " + rawData.size());

        int labelColumn = 0;
        rawData = moveColumnToEnd(rawData, labelColumn);

        Preprocessor preprocessor = new Preprocessor(rawData);
        List<String[]> processedData = preprocessor.preprocess();
        sink.log("Missing values replaced using mean imputation.");

        List<List<Integer>> groups = preprocessor.getGroups();
        numGroups = groups.size();

        if (!processedData.isEmpty()) {
            totalOriginalFeatures = processedData.get(0).length - 1;
        }

        sink.log("Formed " + numGroups + " feature groups from "
                + totalOriginalFeatures + " original features.");

        // ---- emit groups to the UI ----
        StringBuilder groupsJson = new StringBuilder("[");
        for (int i = 0; i < groups.size(); i++) {
            if (i > 0) groupsJson.append(",");
            groupsJson.append("{\"id\":").append(i)
                      .append(",\"size\":").append(groups.get(i).size())
                      .append(",\"features\":").append(groups.get(i))
                      .append("}");
        }
        groupsJson.append("]");
        sink.groups(groupsJson.toString());

        // ---- data splits ----
        DataSplitter dataSplitter = new DataSplitter();
        DataSplit dataSplit = dataSplitter.splitData(processedData, 0.7);

        ValidationSplitter validationSplitter = new ValidationSplitter();
        ValidationSplit validationSplit = validationSplitter.splitValidation(dataSplit.getTrainSet(), 0.8);

        double[][] innerTrainData = DataConverter.toFeatureArray(validationSplit.getTrainingSet());
        int[] innerTrainLabels = DataConverter.toLabelArray(validationSplit.getTrainingSet());

        double[][] validationData = DataConverter.toFeatureArray(validationSplit.getValidationSet());
        int[] validationLabels = DataConverter.toLabelArray(validationSplit.getValidationSet());

        double[][] finalTrainData = DataConverter.toFeatureArray(dataSplit.getTrainSet());
        int[] finalTrainLabels = DataConverter.toLabelArray(dataSplit.getTrainSet());

        double[][] testData = DataConverter.toFeatureArray(dataSplit.getTestSet());
        int[] testLabels = DataConverter.toLabelArray(dataSplit.getTestSet());

        int k = 5;

        // ============================================================
        //  BASELINE — all features, no selection
        // ============================================================
        sink.log("\n====================================================");
        sink.log("   BASELINE — ALL FEATURES (NO SELECTION)");
        sink.log("====================================================");

        int totalFeatures = finalTrainData[0].length;
        for (String clf : classifiersToRun) {
            runBaseline(clf, finalTrainData, finalTrainLabels,
                        testData, testLabels, totalFeatures, k);
        }

        // ============================================================
        //  GA + PSO FOR EACH SELECTED CLASSIFIER
        // ============================================================
        int step = 100 / Math.max(classifiersToRun.size(), 1);
        int done = 0;

        for (String clf : classifiersToRun) {
            runPipeline(clf,
                    innerTrainData, innerTrainLabels,
                    validationData, validationLabels,
                    finalTrainData, finalTrainLabels,
                    testData, testLabels,
                    groups, numGroups, k);

            done += step;
            sink.progress(Math.min(done, 100));
        }

        sink.log("\nFramework Execution Completed Successfully!");
    }

    // ================================================================
    //  BASELINE — ALL FEATURES, NO SELECTION
    // ================================================================
    private void runBaseline(String classifierName,
                             double[][] trainData, int[] trainLabels,
                             double[][] testData, int[] testLabels,
                             int numFeatures, int k) {

        sink.log("\nRunning BASELINE (all features) with " + classifierName + "...");

        List<Integer> allFeatures = new ArrayList<>();
        for (int i = 0; i < numFeatures; i++) allFeatures.add(i);

        long start = System.currentTimeMillis();
        Evaluation eval;

        if (classifierName.equals("TREE")) {
            eval = new DecisionTree().evaluate(trainData, trainLabels, testData, testLabels, allFeatures);
        } else if (classifierName.equals("LR")) {
            eval = new LogisticRegression().evaluate(trainData, trainLabels, testData, testLabels, allFeatures);
        } else {
            eval = new KNN(k).evaluate(trainData, trainLabels, testData, testLabels, allFeatures);
        }

        long duration = System.currentTimeMillis() - start;

        sink.log(String.format("[BASELINE-%s] F1=%.4f  GMean=%.4f  (all %d features, %d ms)",
                classifierName, eval.calculateF1Score(), eval.calculateGMean(),
                numFeatures, duration));

        BaselineResult r = new BaselineResult();
        r.classifierName = classifierName;
        r.eval = eval;
        r.durationMs = duration;
        r.numFeatures = numFeatures;
        baselineResults.add(r);
    }

    // ================================================================
    //  GA + PSO FOR ONE CLASSIFIER
    // ================================================================
    private void runPipeline(String classifierName,
                             double[][] innerTrainData, int[] innerTrainLabels,
                             double[][] validationData, int[] validationLabels,
                             double[][] finalTrainData, int[] finalTrainLabels,
                             double[][] testData, int[] testLabels,
                             List<List<Integer>> groups, int numGroups, int k) {

        sink.log("\n####################################################");
        sink.log("   RUNNING PIPELINE WITH CLASSIFIER: " + classifierName);
        sink.log("####################################################");

        FitnessFunction fitnessFunction = new FitnessFunction(k, classifierName);

        // ================= GA =================
        sink.log("\nSTARTING GENETIC ALGORITHM (GA) — " + classifierName);

        int gaPopSize = 15;
        int gaGenerations = 20;
        GeneticAlgorithm ga = new GeneticAlgorithm(gaPopSize, gaGenerations, 0.8, 0.05);

        List<List<Integer>> population = ga.initializePopulation(numGroups, groups);
        double bestGaFitnessEver = -1.0;
        int gaPatience = 8;
        int gaGensWithoutImprovement = 0;
        List<Integer> bestGaFeatures = null;

        long gaStart = System.currentTimeMillis();

        for (int gen = 0; gen < ga.getGenerations(); gen++) {
            List<Double> fitnessScores = new ArrayList<>();
            boolean improved = false;
            double genBestFitness = -1.0;
            List<Integer> genBestChromosome = null;

            for (int c = 0; c < population.size(); c++) {
                List<Integer> chromosome = population.get(c);
                List<Integer> selectedFeatures = ga.decodeChromosome(chromosome, groups);

                double fitness = fitnessFunction.calculateFitnessCV(
    innerTrainData, innerTrainLabels,
    selectedFeatures, 3);

                fitnessScores.add(fitness);

                if (fitness > genBestFitness) {
                    genBestFitness = fitness;
                    genBestChromosome = chromosome;
                }

                if (fitness > bestGaFitnessEver) {
                    bestGaFitnessEver = fitness;
                    bestGaFeatures = selectedFeatures;
                    improved = true;
                }
            }

            List<Integer> decoded = ga.decodeChromosome(genBestChromosome, groups);
            sink.log(String.format("[%s] GA Gen %3d | Best Fitness: %.6f | Selected Features (%d)",
                    classifierName, gen, genBestFitness, decoded.size()));

            gaGensWithoutImprovement = improved ? 0 : gaGensWithoutImprovement + 1;
            if (gaGensWithoutImprovement >= gaPatience) {
                sink.log("[" + classifierName + "] GA stopping early at generation " + gen + ".");
                break;
            }

            ga.evolve(population, groups, fitnessScores);
        }

        long gaDuration = System.currentTimeMillis() - gaStart;

        // ================= PSO =================
        sink.log("\nSTARTING PARTICLE SWARM OPTIMIZATION (PSO) — " + classifierName);

        int swarmSize = 15;
        int psoIterations = 20;
        PSO pso = new PSO(swarmSize, psoIterations, 0.5, 1.5, 1.5);

        List<PSO.Particle> swarm = pso.initializeSwarm(numGroups, groups);
        double[] gBestPosition = null;
        double bestPsoFitnessEver = -1.0;
        int psoPatience = 8;
        int psoItersWithoutImprovement = 0;
        List<Integer> bestPsoFeatures = null;

        long psoStart = System.currentTimeMillis();

        for (int iter = 0; iter < pso.getIterations(); iter++) {
            boolean improved = false;
            double iterBestFitness = -1.0;
            double[] iterBestPosition = null;

            for (int p = 0; p < swarm.size(); p++) {
                PSO.Particle particle = swarm.get(p);
                List<Integer> selectedFeatures = pso.decodeParticle(particle.position, groups);

                double fitness = fitnessFunction.calculateFitnessCV(
    innerTrainData, innerTrainLabels,
    selectedFeatures, 3);

                if (fitness > particle.pBestFitness) {
                    particle.pBestFitness = fitness;
                    particle.pBestPosition = particle.position.clone();
                }

                if (fitness > iterBestFitness) {
                    iterBestFitness = fitness;
                    iterBestPosition = particle.position.clone();
                }

                if (fitness > bestPsoFitnessEver) {
                    bestPsoFitnessEver = fitness;
                    gBestPosition = particle.position.clone();
                    bestPsoFeatures = selectedFeatures;
                    improved = true;
                }
            }

            List<Integer> decoded = pso.decodeParticle(iterBestPosition, groups);
            sink.log(String.format("[%s] PSO Iter %3d | Best Fitness: %.6f | Selected Features (%d)",
                    classifierName, iter, iterBestFitness, decoded.size()));

            psoItersWithoutImprovement = improved ? 0 : psoItersWithoutImprovement + 1;
            if (psoItersWithoutImprovement >= psoPatience) {
                sink.log("[" + classifierName + "] PSO stopping early at iteration " + iter + ".");
                break;
            }

            if (gBestPosition != null) {
                pso.updateSwarm(swarm, groups, gBestPosition);
            }
        }

        long psoDuration = System.currentTimeMillis() - psoStart;

        // ================= FINAL TEST =================
        sink.log("\n[" + classifierName + "] EVALUATING FINAL TEST SET...");

        Evaluation gaEval;
        Evaluation psoEval;

        if (classifierName.equals("TREE")) {
            DecisionTree clf = new DecisionTree();
            gaEval = clf.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatures);
            psoEval = clf.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatures);
        } else if (classifierName.equals("LR")) {
            LogisticRegression clf = new LogisticRegression();
            gaEval = clf.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatures);
            psoEval = clf.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatures);
        } else {
            KNN clf = new KNN(k);
            gaEval = clf.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatures);
            psoEval = clf.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatures);
        }

        sink.log("[" + classifierName + "] GA  → F1=" + String.format("%.4f", gaEval.calculateF1Score())
                + "  GMean=" + String.format("%.4f", gaEval.calculateGMean()));
        sink.log("[" + classifierName + "] PSO → F1=" + String.format("%.4f", psoEval.calculateF1Score())
                + "  GMean=" + String.format("%.4f", psoEval.calculateGMean()));

        // ---- store result ----
        Result r = new Result();
        r.classifierName = classifierName;
        r.gaEval = gaEval;
        r.psoEval = psoEval;
        r.gaDurationMs = gaDuration;
        r.psoDurationMs = psoDuration;
        r.bestGaFeatures = bestGaFeatures;
        r.bestPsoFeatures = bestPsoFeatures;
        results.add(r);
    }

    // ================================================================
    //  UTILITY
    // ================================================================
    private List<String[]> moveColumnToEnd(List<String[]> data, int targetCol) {
        List<String[]> result = new ArrayList<>();
        for (String[] row : data) {
            String[] newRow = new String[row.length];
            String label = row[targetCol];
            int idx = 0;
            for (int i = 0; i < row.length; i++) {
                if (i != targetCol) newRow[idx++] = row[i];
            }
            newRow[row.length - 1] = label;
            result.add(newRow);
        }
        return result;
    }
}