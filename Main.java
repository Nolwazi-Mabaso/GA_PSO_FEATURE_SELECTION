import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame {

    private File datasetFile = null;
    private JTextField txtFilePath;
    private JCheckBox chkEnablePearson;
    private JComboBox<String> comboClassifier;
    private JButton btnRun;
    private JTextArea txtConsole;

    public Main() {
        setTitle("Feature Selection Pipeline");
        setSize(750, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Top Controls Panel
        JPanel controlsPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. File Upload
        JPanel pnlFile = new JPanel(new BorderLayout(5, 5));
        txtFilePath = new JTextField("No file selected");
        txtFilePath.setEditable(false);
        JButton btnBrowse = new JButton("Upload CSV");
        btnBrowse.addActionListener(e -> selectFile());
        pnlFile.add(txtFilePath, BorderLayout.CENTER);
        pnlFile.add(btnBrowse, BorderLayout.EAST);

        // 2. Pearson Filtering Options
        chkEnablePearson = new JCheckBox("Enable Pearson Filtering", true);

        // 3. Classifier Selection
        JPanel pnlClassifier = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboClassifier = new JComboBox<>(new String[] { "KNN", "Naive Bayes", "SVM" });
        pnlClassifier.add(new JLabel("Classifier: "));
        pnlClassifier.add(comboClassifier);

        // 4. Run Button
        btnRun = new JButton("Run Pipeline");
        btnRun.setEnabled(false);
        btnRun.addActionListener(e -> startPipeline());

        controlsPanel.add(pnlFile);
        controlsPanel.add(chkEnablePearson);
        controlsPanel.add(pnlClassifier);
        controlsPanel.add(btnRun);

        add(controlsPanel, BorderLayout.NORTH);

        // Right/Center Log Area
        txtConsole = new JTextArea();
        txtConsole.setEditable(false);
        txtConsole.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(txtConsole), BorderLayout.CENTER);

        // Redirect System.out to text area
        redirectSystemStreams();

        setVisible(true);
    }

    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            datasetFile = chooser.getSelectedFile();
            txtFilePath.setText(datasetFile.getAbsolutePath());
            btnRun.setEnabled(true);
            System.out.println("Dataset selected: " + datasetFile.getName());
        }
    }

    private void startPipeline() {
        btnRun.setEnabled(false);
        txtConsole.setText(""); // Clear console

        String selectedClassifier = (String) comboClassifier.getSelectedItem();
        boolean usePearson = chkEnablePearson.isSelected();

        // Run heavy pipeline execution on a background thread
        new Thread(() -> {
            try {
                System.out.println("======================================");
                System.out.println("   STARTING FEATURE SELECTION PIPELINE");
                System.out.println("======================================");
                System.out.println("Classifier: " + selectedClassifier);
                System.out.println("Pearson Enabled: " + usePearson + "\n");

                // 1. LOAD RAW DATA
                LoadData loader = new LoadData(datasetFile.getAbsolutePath());
                if (!loader.load()) {
                    System.out.println("Error loading CSV file.");
                    return;
                }
                List<String[]> rawData = loader.getData();

                // 1b. MOVE LABEL COLUMN TO THE END
                rawData = moveColumnToEnd(rawData, 0);

                // 2. PREPROCESS + PEARSON FEATURE GROUPS
                Preprocessor preprocessor = new Preprocessor(rawData);
                List<String[]> processedData = preprocessor.preprocess();
                List<List<Integer>> groups;

                if (usePearson) {
                    groups = preprocessor.getGroups();
                } else {
                    int totalFeatures = processedData.get(0).length - 1;
                    groups = new ArrayList<>();
                    for (int i = 0; i < totalFeatures; i++) {
                        groups.add(java.util.Collections.singletonList(i));
                    }
                }

                PearsonCorrelation.printFeatureGroups(groups);

                // 3. TRAIN / TEST SPLIT
                DataSplitter dataSplitter = new DataSplitter();
                DataSplit dataSplit = dataSplitter.splitData(processedData, 0.7);

                // 4. INNER TRAINING / VALIDATION SPLIT
                ValidationSplitter validationSplitter = new ValidationSplitter();
                ValidationSplit validationSplit = validationSplitter.splitValidation(dataSplit.getTrainSet(), 0.8);

                // 5. CONVERT TO ARRAYS
                double[][] innerTrainData = DataConverter.toFeatureArray(validationSplit.getTrainingSet());
                int[] innerTrainLabels = DataConverter.toLabelArray(validationSplit.getTrainingSet());
                double[][] validationData = DataConverter.toFeatureArray(validationSplit.getValidationSet());
                int[] validationLabels = DataConverter.toLabelArray(validationSplit.getValidationSet());

                int k = 5;
                FitnessFunction fitnessFunction = new FitnessFunction(k);
                int numGroups = groups.size();

                // 6. RUN GENETIC ALGORITHM
                System.out.println("\n======================================");
                System.out.println("STARTING GENETIC ALGORITHM (GA)");
                System.out.println("======================================");

                GeneticAlgorithm ga = new GeneticAlgorithm(50, 100, 0.8, 0.01);
                List<List<Integer>> population = ga.initializePopulation(numGroups, groups);

                List<Integer> bestGaChromosome = null;
                List<Integer> bestGaFeatureSubset = null;
                double bestGaFitnessEver = -1;

                int gaPatience = 15;
                int gaGensWithoutImprovement = 0;

                for (int gen = 0; gen < ga.getGenerations(); gen++) {
                    List<Double> fitnessScores = new ArrayList<>();
                    double bestFitnessThisGen = -1;

                    for (int c = 0; c < population.size(); c++) {
                        List<Integer> chromosome = population.get(c);
                        List<Integer> selectedFeatures = ga.decodeChromosome(chromosome, groups);

                        double fitness = fitnessFunction.calculateFitness(
                                innerTrainData, innerTrainLabels, validationData, validationLabels, selectedFeatures);

                        fitnessScores.add(fitness);

                        if (fitness > bestFitnessThisGen) {
                            bestFitnessThisGen = fitness;
                        }

                        if (fitness > bestGaFitnessEver) {
                            bestGaFitnessEver = fitness;
                            bestGaChromosome = new ArrayList<>(chromosome);
                            bestGaFeatureSubset = selectedFeatures;
                        }
                    }

                    System.out.printf("GA Gen %02d | Best Fitness So Far: %.4f%n", gen, bestGaFitnessEver);

                    if (bestFitnessThisGen >= bestGaFitnessEver) {
                        gaGensWithoutImprovement = 0;
                    } else {
                        gaGensWithoutImprovement++;
                    }

                    if (gaGensWithoutImprovement >= gaPatience) {
                        System.out.println("GA Early stopping at generation " + gen);
                        break;
                    }

                    ga.evolve(population, groups, fitnessScores);
                }

                ga.printResults(bestGaChromosome, bestGaFeatureSubset);

                // 7. RUN PARTICLE SWARM OPTIMIZATION (PSO)
                System.out.println("\n======================================");
                System.out.println("STARTING PARTICLE SWARM OPTIMIZATION (PSO)");
                System.out.println("======================================");

                PSO pso = new PSO(50, 100, 0.5, 1.5, 1.5);
                List<PSO.Particle> swarm = pso.initializeSwarm(numGroups, groups);

                double[] gBestPosition = null;
                List<Integer> bestPsoFeatureSubset = null;
                double bestPsoFitnessEver = -1;

                int psoPatience = 15;
                int psoItersWithoutImprovement = 0;

                for (int iter = 0; iter < pso.getIterations(); iter++) {
                    double bestFitnessThisIter = -1;

                    for (int p = 0; p < swarm.size(); p++) {
                        PSO.Particle particle = swarm.get(p);
                        List<Integer> selectedFeatures = pso.decodeParticle(particle.position, groups);

                        double fitness = fitnessFunction.calculateFitness(
                                innerTrainData, innerTrainLabels, validationData, validationLabels, selectedFeatures);

                        if (fitness > particle.pBestFitness) {
                            particle.pBestFitness = fitness;
                            particle.pBestPosition = particle.position.clone();
                        }

                        if (fitness > bestPsoFitnessEver) {
                            bestPsoFitnessEver = fitness;
                            gBestPosition = particle.position.clone();
                            bestPsoFeatureSubset = selectedFeatures;
                        }

                        if (fitness > bestFitnessThisIter) {
                            bestFitnessThisIter = fitness;
                        }
                    }

                    System.out.printf("PSO Iter %02d | Best Fitness So Far: %.4f%n", iter, bestPsoFitnessEver);

                    if (bestFitnessThisIter >= bestPsoFitnessEver) {
                        psoItersWithoutImprovement = 0;
                    } else {
                        psoItersWithoutImprovement++;
                    }

                    if (psoItersWithoutImprovement >= psoPatience) {
                        System.out.println("PSO Early stopping at iteration " + iter);
                        break;
                    }

                    if (gBestPosition != null) {
                        pso.updateSwarm(swarm, groups, gBestPosition);
                    }
                }

                pso.printResults(gBestPosition, bestPsoFeatureSubset);

                // 8. FINAL EVALUATION ON UNTOUCHED TEST SET
                double[][] finalTrainData = DataConverter.toFeatureArray(dataSplit.getTrainSet());
                int[] finalTrainLabels = DataConverter.toLabelArray(dataSplit.getTrainSet());
                double[][] testData = DataConverter.toFeatureArray(dataSplit.getTestSet());
                int[] testLabels = DataConverter.toLabelArray(dataSplit.getTestSet());

                KNN finalKnn = new KNN(k);

                Evaluation gaEvaluation = finalKnn.evaluate(
                        finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatureSubset);

                Evaluation psoEvaluation = finalKnn.evaluate(
                        finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatureSubset);

                System.out.println("\n======================================");
                System.out.println("FINAL TEST SET COMPARISON");
                System.out.println("======================================");
                System.out.println("--- Genetic Algorithm (GA) ---");
                System.out.printf("F1 Score: %.4f | G-Mean: %.4f%n", gaEvaluation.calculateF1Score(), gaEvaluation.calculateGMean());

                System.out.println("\n--- Particle Swarm Optimization (PSO) ---");
                System.out.printf("F1 Score: %.4f | G-Mean: %.4f%n", psoEvaluation.calculateF1Score(), psoEvaluation.calculateGMean());
                System.out.println("======================================");

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(() -> btnRun.setEnabled(true));
            }
        }).start();
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

    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                txtConsole.append(String.valueOf((char) b));
                txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
            }

            @Override
            public void write(byte[] b, int off, int len) {
                txtConsole.append(new String(b, off, len));
                txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
            }
        };
        System.setOut(new PrintStream(out, true));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }
}