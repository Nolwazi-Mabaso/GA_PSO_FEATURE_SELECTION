import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;

public class Runner extends SwingWorker<Void, String> {

    private final String filePath;
    private final MainGUI gui;

    private Evaluation finalGaEval;
    private long gaDurationMs;
    private List<Integer> bestGaFeatures;

    private Evaluation finalPsoEval;
    private long psoDurationMs;
    private List<Integer> bestPsoFeatures;

    public Runner(String filePath, MainGUI gui) {
        this.filePath = filePath;
        this.gui = gui;
    }

    @Override
    protected Void doInBackground() throws Exception {
        publish("             DATA LOADING & PREPROCESSING         ");
        publish("___________________________________________________");

        LoadData loader = new LoadData(filePath);
        if (!loader.load()) {
            publish("Could not load dataset. Closing system.");
            return null;
        }

        List<String[]> rawData = loader.getData();
        publish("Data loaded successfully.");
        publish("Rows loaded: " + rawData.size());

        int labelColumn = 0;
        rawData = moveColumnToEnd(rawData, labelColumn);

        Preprocessor preprocessor = new Preprocessor(rawData);
        List<String[]> processedData = preprocessor.preprocess();
        publish("Missing values replaced using mean imputation.");

        List<List<Integer>> groups = preprocessor.getGroups();
        publish("Formed " + groups.size() + " feature groups.\n");

        DataSplitter dataSplitter = new DataSplitter();
        DataSplit dataSplit = dataSplitter.splitData(processedData, 0.7);

        ValidationSplitter validationSplitter = new ValidationSplitter();
        ValidationSplit validationSplit = validationSplitter.splitValidation(dataSplit.getTrainSet(), 0.8);

        double[][] innerTrainData = DataConverter.toFeatureArray(validationSplit.getTrainingSet());
        int[] innerTrainLabels = DataConverter.toLabelArray(validationSplit.getTrainingSet());

        double[][] validationData = DataConverter.toFeatureArray(validationSplit.getValidationSet());
        int[] validationLabels = DataConverter.toLabelArray(validationSplit.getValidationSet());

        int k = 5;
        FitnessFunction fitnessFunction = new FitnessFunction(k);
        int numGroups = groups.size();

        publish("STARTING GENETIC ALGORITHM (GA)...");
        publish("________________________________________________");

        int gaPopSize = 50;
        int gaGenerations = 100;
        GeneticAlgorithm ga = new GeneticAlgorithm(gaPopSize, gaGenerations, 0.8, 0.05);

        List<List<Integer>> population = ga.initializePopulation(numGroups, groups);
        List<Integer> bestGaChromosome = null;
        double bestGaFitnessEver = -1.0;
        int gaPatience = 15;
        int gaGensWithoutImprovement = 0;

        long gaStart = System.currentTimeMillis();

        for (int gen = 0; gen < ga.getGenerations(); gen++) {
            List<Double> fitnessScores = new ArrayList<>();
            boolean improved = false;
            double genBestFitness = -1.0;
            List<Integer> genBestChromosome = null;

            for (int c = 0; c < population.size(); c++) {
                List<Integer> chromosome = population.get(c);
                List<Integer> selectedFeatures = ga.decodeChromosome(chromosome, groups);

                double fitness = fitnessFunction.calculateFitness(
                        innerTrainData, innerTrainLabels, validationData, validationLabels, selectedFeatures, numGroups);

                fitnessScores.add(fitness);

                if (fitness > genBestFitness) {
                    genBestFitness = fitness;
                    genBestChromosome = chromosome;
                }

                if (fitness > bestGaFitnessEver) {
                    bestGaFitnessEver = fitness;
                    bestGaChromosome = new ArrayList<>(chromosome);
                    bestGaFeatures = selectedFeatures;
                    improved = true;
                }
            }

            List<Integer> decodedGens = ga.decodeChromosome(genBestChromosome, groups);
            publish(String.format("GA Gen %3d | Best Fitness: %.6f | Selected Features (%d): %s",
                    gen, genBestFitness, decodedGens.size(), decodedGens));

            gaGensWithoutImprovement = improved ? 0 : gaGensWithoutImprovement + 1;
            if (gaGensWithoutImprovement >= gaPatience) {
                publish("\nGA Stopping early: no improvement for " + gaPatience + " generations (at generation " + gen + ").");
                break;
            }

            ga.evolve(population, groups, fitnessScores);
            setProgress((int) (((gen + 1.0) / gaGenerations) * 50));
        }

        gaDurationMs = System.currentTimeMillis() - gaStart;


        publish("\nSTARTING PARTICLE SWARM OPTIMIZATION (PSO)...");
        publish("________________________________________________");

        int swarmSize = 50;
        int psoIterations = 100;
        PSO pso = new PSO(swarmSize, psoIterations, 0.5, 1.5, 1.5);

        List<PSO.Particle> swarm = pso.initializeSwarm(numGroups, groups);
        double[] gBestPosition = null;
        double bestPsoFitnessEver = -1.0;
        int psoPatience = 15;
        int psoItersWithoutImprovement = 0;

        long psoStart = System.currentTimeMillis();

        for (int iter = 0; iter < pso.getIterations(); iter++) {
            boolean improved = false;
            double iterBestFitness = -1.0;
            double[] iterBestPosition = null;

            for (int p = 0; p < swarm.size(); p++) {
                PSO.Particle particle = swarm.get(p);
                List<Integer> selectedFeatures = pso.decodeParticle(particle.position, groups);

                double fitness = fitnessFunction.calculateFitness(
                        innerTrainData, innerTrainLabels, validationData, validationLabels, selectedFeatures, numGroups);

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

            List<Integer> decodedIter = pso.decodeParticle(iterBestPosition, groups);
            publish(String.format("PSO Iter %3d | Best Fitness: %.6f | Selected Features (%d): %s",
                    iter, iterBestFitness, decodedIter.size(), decodedIter));

            psoItersWithoutImprovement = improved ? 0 : psoItersWithoutImprovement + 1;
            if (psoItersWithoutImprovement >= psoPatience) {
                publish("\nPSO Stopping early: no improvement for " + psoPatience + " iterations (at iteration " + iter + ").");
                break;
            }

            if (gBestPosition != null) {
                pso.updateSwarm(swarm, groups, gBestPosition);
            }
            setProgress(50 + (int) (((iter + 1.0) / psoIterations) * 50));
        }

        psoDurationMs = System.currentTimeMillis() - psoStart;


        publish("\n");
        publish("             EVALUATING FINAL TEST SET            ");
        publish("_________________________________________________");

        double[][] finalTrainData = DataConverter.toFeatureArray(dataSplit.getTrainSet());
        int[] finalTrainLabels = DataConverter.toLabelArray(dataSplit.getTrainSet());

        double[][] testData = DataConverter.toFeatureArray(dataSplit.getTestSet());
        int[] testLabels = DataConverter.toLabelArray(dataSplit.getTestSet());

        KNN finalKnn = new KNN(k);
        finalGaEval = finalKnn.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatures);
        finalPsoEval = finalKnn.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatures);

        publish("\nFramework Execution Completed Successfully!");
        return null;
    }

    @Override
    protected void process(List<String> chunks) {
        for (String chunk : chunks) {
            gui.appendLog(chunk);
        }
    }

    @Override
    protected void done() {
        if (finalGaEval != null && finalPsoEval != null) {
            gui.updateGaResults(finalGaEval, gaDurationMs, bestGaFeatures);
            gui.updatePsoResults(finalPsoEval, psoDurationMs, bestPsoFeatures);
        }
        gui.setProgressValue(100);
        gui.setProgressStatus("Completed");
        gui.setRunButtonEnabled(true);
    }

    private List<String[]> moveColumnToEnd(List<String[]> data, int targetCol) {
        List<String[]> result = new ArrayList<>();
        for (String[] row : data) {
            String[] newRow = new String[row.length];
            String label = row[targetCol];
            int idx = 0;
            for (int i = 0; i < row.length; i++) {
                if (i != targetCol) {
                    newRow[idx++] = row[i];
                }
            }
            newRow[row.length - 1] = label;
            result.add(newRow);
        }
        return result;
    }
}