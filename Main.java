import java.util.*;

public class Main {

    public static void main(String[] args) {

        // ------------------------------------------------------------
        // 1. LOAD RAW DATA
        // ------------------------------------------------------------
        String filePath = "data.csv"; // TODO: point this at your actual CSV

        LoadData loader = new LoadData(filePath);

        if (!loader.load()) {
            System.out.println("Could not load data, stopping.");
            return;
        }

        List<String[]> rawData = loader.getData();

        // ------------------------------------------------------------
        // 1b. MOVE LABEL COLUMN TO THE END
        // ------------------------------------------------------------
        int labelColumn = 0;
        rawData = moveColumnToEnd(rawData, labelColumn);

        // ------------------------------------------------------------
        // 2. PREPROCESS + PEARSON FEATURE GROUPS
        // ------------------------------------------------------------
        Preprocessor preprocessor = new Preprocessor(rawData);
        List<String[]> processedData = preprocessor.preprocess();
        List<List<Integer>> groups = preprocessor.getGroups();

        PearsonCorrelation.printFeatureGroups(groups);

        // ------------------------------------------------------------
        // 3. TRAIN / TEST SPLIT (test set locked away until the end)
        // ------------------------------------------------------------
        DataSplitter dataSplitter = new DataSplitter();
        DataSplit dataSplit = dataSplitter.splitData(processedData, 0.7);

        // ------------------------------------------------------------
        // 4. INNER TRAINING / VALIDATION SPLIT
        // ------------------------------------------------------------
        ValidationSplitter validationSplitter = new ValidationSplitter();
        ValidationSplit validationSplit =
                validationSplitter.splitValidation(dataSplit.getTrainSet(), 0.8);

        // ------------------------------------------------------------
        // 5. CONVERT String[] ROWS INTO NUMERIC ARRAYS FOR KNN
        // ------------------------------------------------------------
        double[][] innerTrainData = DataConverter.toFeatureArray(validationSplit.getTrainingSet());
        int[] innerTrainLabels = DataConverter.toLabelArray(validationSplit.getTrainingSet());

        double[][] validationData = DataConverter.toFeatureArray(validationSplit.getValidationSet());
        int[] validationLabels = DataConverter.toLabelArray(validationSplit.getValidationSet());

        // Shared fitness evaluator for both GA and PSO
        int k = 5;
        FitnessFunction fitnessFunction = new FitnessFunction(k);
        int numGroups = groups.size();

        // ============================================================
        // 6. RUN THE GENETIC ALGORITHM
        // ============================================================
        System.out.println("\n======================================");
        System.out.println("STARTING GENETIC ALGORITHM (GA)");
        System.out.println("======================================");

        int gaPopSize = 50;
        int gaGenerations = 100;
        double crossoverRate = 0.8;
        double mutationRate = 0.01;

        GeneticAlgorithm ga = new GeneticAlgorithm(
                gaPopSize,
                gaGenerations,
                crossoverRate,
                mutationRate
        );

        List<List<Integer>> population = ga.initializePopulation(numGroups, groups);

        List<Integer> bestGaChromosome = null;
        List<Integer> bestGaFeatureSubset = null;
        double bestGaFitnessEver = -1;

        int gaPatience = 15;
        int gaGensWithoutImprovement = 0;

        for (int gen = 0; gen < ga.getGenerations(); gen++) {

            List<Double> fitnessScores = new ArrayList<>();
            double bestFitnessThisGen = -1;

            System.out.println("\n--- GA Generation " + gen + " ---");

            for (int c = 0; c < population.size(); c++) {

                List<Integer> chromosome = population.get(c);
                List<Integer> selectedFeatures = ga.decodeChromosome(chromosome, groups);

                double fitness = fitnessFunction.calculateFitness(
                        innerTrainData,
                        innerTrainLabels,
                        validationData,
                        validationLabels,
                        selectedFeatures
                );

                fitnessScores.add(fitness);

                System.out.println(
                        "Chromosome " + c + " -> Features: " + selectedFeatures + "\n" +
                        " -> Fitness: " + fitness + "\n"
                );

                if (fitness > bestFitnessThisGen) {
                    bestFitnessThisGen = fitness;
                }

                if (fitness > bestGaFitnessEver) {
                    bestGaFitnessEver = fitness;
                    bestGaChromosome = new ArrayList<>(chromosome);
                    bestGaFeatureSubset = selectedFeatures;
                }
            }

            System.out.println("Best GA fitness so far: " + bestGaFitnessEver);

            if (bestFitnessThisGen >= bestGaFitnessEver) {
                gaGensWithoutImprovement = 0;
            } else {
                gaGensWithoutImprovement++;
            }

            if (gaGensWithoutImprovement >= gaPatience) {
                System.out.println(
                        "\nGA Stopping early: no improvement in the last "
                        + gaPatience + " generations (generation " + gen + ")."
                );
                break;
            }

            ga.evolve(population, groups, fitnessScores);
        }

        ga.printResults(bestGaChromosome, bestGaFeatureSubset);

        // ============================================================
        // 7. RUN PARTICLE SWARM OPTIMIZATION (PSO)
        // ============================================================
        System.out.println("\n======================================");
        System.out.println("STARTING PARTICLE SWARM OPTIMIZATION (PSO)");
        System.out.println("======================================");

        int swarmSize = 50;
        int psoIterations = 100;
        double inertiaWeight = 0.5;
        double cognitiveWeight = 1.5;
        double socialWeight = 1.5;

        PSO pso = new PSO(
                swarmSize,
                psoIterations,
                inertiaWeight,
                cognitiveWeight,
                socialWeight
        );

        List<PSO.Particle> swarm = pso.initializeSwarm(numGroups, groups);

        double[] gBestPosition = null;
        List<Integer> bestPsoFeatureSubset = null;
        double bestPsoFitnessEver = -1;

        int psoPatience = 15;
        int psoItersWithoutImprovement = 0;

        for (int iter = 0; iter < pso.getIterations(); iter++) {

            double bestFitnessThisIter = -1;

            System.out.println("\n--- PSO Iteration " + iter + " ---");

            for (int p = 0; p < swarm.size(); p++) {

                PSO.Particle particle = swarm.get(p);
                List<Integer> selectedFeatures = pso.decodeParticle(particle.position, groups);

                double fitness = fitnessFunction.calculateFitness(
                        innerTrainData,
                        innerTrainLabels,
                        validationData,
                        validationLabels,
                        selectedFeatures
                );

                // Update particle's personal best (pBest)
                if (fitness > particle.pBestFitness) {
                    particle.pBestFitness = fitness;
                    particle.pBestPosition = particle.position.clone();
                }

                // Update swarm's global best (gBest)
                if (fitness > bestPsoFitnessEver) {
                    bestPsoFitnessEver = fitness;
                    gBestPosition = particle.position.clone();
                    bestPsoFeatureSubset = selectedFeatures;
                }

                if (fitness > bestFitnessThisIter) {
                    bestFitnessThisIter = fitness;
                }

                System.out.println(
                        "Particle " + p + " -> Features: " + selectedFeatures + "\n" +
                        " -> Fitness: " + fitness + "\n"
                );
            }

            System.out.println("Best PSO fitness so far: " + bestPsoFitnessEver);

            if (bestFitnessThisIter >= bestPsoFitnessEver) {
                psoItersWithoutImprovement = 0;
            } else {
                psoItersWithoutImprovement++;
            }

            if (psoItersWithoutImprovement >= psoPatience) {
                System.out.println(
                        "\nPSO Stopping early: no improvement in the last "
                        + psoPatience + " iterations (iteration " + iter + ")."
                );
                break;
            }

            // Update positions and velocities for next iteration
            if (gBestPosition != null) {
                pso.updateSwarm(swarm, groups, gBestPosition);
            }
        }

        pso.printResults(gBestPosition, bestPsoFeatureSubset);

        // ============================================================
        // 8. FINAL EVALUATION ON UNTOUCHED TEST SET
        // ============================================================
        double[][] finalTrainData = DataConverter.toFeatureArray(dataSplit.getTrainSet());
        int[] finalTrainLabels = DataConverter.toLabelArray(dataSplit.getTrainSet());

        double[][] testData = DataConverter.toFeatureArray(dataSplit.getTestSet());
        int[] testLabels = DataConverter.toLabelArray(dataSplit.getTestSet());

        KNN finalKnn = new KNN(k);

        // Evaluate GA Best Subset
        Evaluation gaEvaluation = finalKnn.evaluate(
                finalTrainData,
                finalTrainLabels,
                testData,
                testLabels,
                bestGaFeatureSubset
        );

        // Evaluate PSO Best Subset
        Evaluation psoEvaluation = finalKnn.evaluate(
                finalTrainData,
                finalTrainLabels,
                testData,
                testLabels,
                bestPsoFeatureSubset
        );

        System.out.println("\n======================================");
        System.out.println("FINAL TEST SET COMPARISON");
        System.out.println("======================================");
        System.out.println("--- Genetic Algorithm (GA) ---");
        System.out.println("F1 Score: " + gaEvaluation.calculateF1Score());
        System.out.println("G-Mean:   " + gaEvaluation.calculateGMean());

        System.out.println("\n--- Particle Swarm Optimization (PSO) ---");
        System.out.println("F1 Score: " + psoEvaluation.calculateF1Score());
        System.out.println("G-Mean:   " + psoEvaluation.calculateGMean());
        System.out.println("======================================");
    }

    private static List<String[]> moveColumnToEnd(List<String[]> data, int targetCol) {
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