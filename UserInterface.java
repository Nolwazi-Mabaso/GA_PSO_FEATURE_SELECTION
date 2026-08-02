import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserInterface extends JFrame {

    // Member variables for simple UI state
    private File datasetFile = null;
    private JTextField txtFilePath;
    private JLabel lblDatasetInfo;
    private JCheckBox chkEnablePearson;
    private JSlider sliderPearson;
    private JComboBox<String> comboClassifier;
    private JProgressBar progressBar;
    private JTextArea txtConsole;
    private JButton btnStartOptimization;
    private JButton btnEvaluate;

    // Execution state preserved between Stage 1 (Optimization) & Stage 2 (Evaluation)
    private DataSplit cachedDataSplit;
    private List<Integer> bestGaFeatureSubset;
    private List<Integer> bestPsoFeatureSubset;
    private double bestGaFitnessEver;
    private double bestPsoFitnessEver;

    public UserInterface() {
        // Basic Window Configuration
        setTitle("Feature Selection System (Alpha Prototype)");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Build Simple Layout Panels
        add(createHeaderPanel(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightConsole());
        splitPane.setDividerLocation(360);
        add(splitPane, BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(20, 30, 55));
        JLabel lblTitle = new JLabel("Feature Selection Pipeline (Pearson + GA / PSO)");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        panel.add(lblTitle);
        return panel;
    }

    // Left Panel: Settings & Control
    private JPanel createLeftPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Dataset Selection
        JPanel pnlData = new JPanel(new GridLayout(3, 1, 5, 5));
        pnlData.setBorder(BorderFactory.createTitledBorder("1. Dataset Ingestion"));

        JPanel pnlBrowse = new JPanel(new BorderLayout(5, 5));
        txtFilePath = new JTextField();
        txtFilePath.setEditable(false);
        JButton btnBrowse = new JButton("Browse");
        btnBrowse.addActionListener(e -> selectFile());
        pnlBrowse.add(txtFilePath, BorderLayout.CENTER);
        pnlBrowse.add(btnBrowse, BorderLayout.EAST);

        lblDatasetInfo = new JLabel("No file selected", SwingConstants.LEFT);
        JLabel lblSplit = new JLabel("Split: 70/30 (With Validation)", SwingConstants.LEFT);

        pnlData.add(pnlBrowse);
        pnlData.add(lblDatasetInfo);
        pnlData.add(lblSplit);

        // 2. Preprocessing & Pearson Filter
        JPanel pnlFilter = new JPanel(new GridLayout(2, 1, 5, 5));
        pnlFilter.setBorder(BorderFactory.createTitledBorder("2. Pearson Pre-Filter"));

        chkEnablePearson = new JCheckBox("Enable Pearson Grouping", true);
        sliderPearson = new JSlider(50, 95, 85); // 0.50 to 0.95 (default 0.85)

        pnlFilter.add(chkEnablePearson);
        pnlFilter.add(sliderPearson);

        // 3. Execution & Controls
        JPanel pnlExec = new JPanel(new GridLayout(4, 1, 5, 5));
        pnlExec.setBorder(BorderFactory.createTitledBorder("3. Execution"));

        comboClassifier = new JComboBox<>(new String[] { "KNN", "Naive Bayes", "SVM" });

        btnStartOptimization = new JButton("1. Run Optimizers");
        btnStartOptimization.setEnabled(false);
        btnStartOptimization.addActionListener(e -> runOptimizationPipeline());

        btnEvaluate = new JButton("2. Run Final Evaluation");
        btnEvaluate.setEnabled(false);
        btnEvaluate.addActionListener(e -> runFinalEvaluation());

        pnlExec.add(new JLabel("Classifier:"));
        pnlExec.add(comboClassifier);
        pnlExec.add(btnStartOptimization);
        pnlExec.add(btnEvaluate);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        panel.add(pnlData);
        panel.add(pnlFilter);
        panel.add(pnlExec);
        panel.add(Box.createVerticalStrut(10));
        panel.add(progressBar);

        return panel;
    }

    // Right Panel: Live Text Console
    private JPanel createRightConsole() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Console Log", TitledBorder.LEFT, TitledBorder.TOP));

        txtConsole = new JTextArea();
        txtConsole.setEditable(false);
        txtConsole.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(txtConsole);
        panel.add(scrollPane, BorderLayout.CENTER);

        log("Alpha GUI Loaded. Upload a CSV file to start.");
        return panel;
    }

    // File Selector Action
    private void selectFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            datasetFile = chooser.getSelectedFile();
            txtFilePath.setText(datasetFile.getName());
            lblDatasetInfo.setText("File: " + datasetFile.getName() + " Loaded");
            btnStartOptimization.setEnabled(true);
            log("Loaded dataset: " + datasetFile.getName());
        }
    }

    private void log(String message) {
        txtConsole.append(message + "\n");
        txtConsole.setCaretPosition(txtConsole.getDocument().getLength());
    }

    // Utility to shift target column to the back of array if needed
    private List<String[]> moveColumnToEnd(List<String[]> data, int targetColIndex) {
        if (data == null || data.isEmpty() || targetColIndex < 0) return data;
        List<String[]> modifiedData = new ArrayList<>();
        for (String[] row : data) {
            if (targetColIndex >= row.length) return data;
            String[] newRow = new String[row.length];
            int pointer = 0;
            for (int i = 0; i < row.length; i++) {
                if (i != targetColIndex) {
                    newRow[pointer++] = row[i];
                }
            }
            newRow[row.length - 1] = row[targetColIndex];
            modifiedData.add(newRow);
        }
        return modifiedData;
    }

    // Phase 1: Sequential Optimization (GA then PSO)
    private void runOptimizationPipeline() {
        btnStartOptimization.setEnabled(false);
        btnEvaluate.setEnabled(false);
        txtConsole.setText("");

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("=== STARTING REAL OPTIMIZATION PIPELINE ===");

                // 1. Ingest Data
                String path = datasetFile.getAbsolutePath();
                publish("Loading CSV file: " + path);
                LoadData loader = new LoadData(path);
                if (!loader.load()) {
                    publish("ERROR: Could not load dataset.");
                    return null;
                }
                List<String[]> rawData = loader.getData();

                // 2. Adjust Label Position (Move target column to last column)
                int labelColumn = 0; 
                rawData = moveColumnToEnd(rawData, labelColumn);

                // 3. Preprocessing & Pearson Grouping
                boolean usePearson = chkEnablePearson.isSelected();
                List<List<Integer>> groups;
                List<String[]> processedData;

                Preprocessor preprocessor = new Preprocessor(rawData);
                processedData = preprocessor.preprocess();

                if (usePearson) {
                    publish("Running Pearson Correlation feature grouping...");
                    groups = preprocessor.getGroups();
                    publish("Pearson Grouping complete: " + groups.size() + " feature groups created.");
                } else {
                    publish("Pearson Filter Disabled: Treating each feature as its own group.");
                    int featureCount = processedData.get(0).length - 1;
                    groups = new ArrayList<>();
                    for (int i = 0; i < featureCount; i++) {
                        groups.add(Collections.singletonList(i));
                    }
                }

                // 4. Data Splits
                DataSplitter dataSplitter = new DataSplitter();
                DataSplit dataSplit = dataSplitter.splitData(processedData, 0.7);

                ValidationSplitter validationSplitter = new ValidationSplitter();
                ValidationSplit validationSplit = validationSplitter.splitValidation(dataSplit.getTrainSet(), 0.8);

                // 5. Array Conversions
                double[][] innerTrainData = DataConverter.toFeatureArray(validationSplit.getTrainingSet());
                int[] innerTrainLabels = DataConverter.toLabelArray(validationSplit.getTrainingSet());
                double[][] validationData = DataConverter.toFeatureArray(validationSplit.getValidationSet());
                int[] validationLabels = DataConverter.toLabelArray(validationSplit.getValidationSet());

                FitnessFunction fitnessFunction = new FitnessFunction(5);
                int numGroups = groups.size();

                // ------------------------------------------------------------
                // 6. RUN REAL GENETIC ALGORITHM
                // ------------------------------------------------------------
                publish("\n--- Executing Genetic Algorithm (GA) ---");
                GeneticAlgorithm ga = new GeneticAlgorithm(50, 100, 0.8, 0.01);
                List<List<Integer>> population = ga.initializePopulation(numGroups, groups);

                bestGaFitnessEver = -1;
                bestGaFeatureSubset = null;

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

                        if (fitness > bestFitnessThisGen) bestFitnessThisGen = fitness;
                        if (fitness > bestGaFitnessEver) {
                            bestGaFitnessEver = fitness;
                            bestGaFeatureSubset = selectedFeatures;
                        }
                    }

                    if (gen % 5 == 0 || gen == ga.getGenerations() - 1) {
                        publish(String.format("[GA] Gen %02d | Best Fitness: %.4f", gen, bestGaFitnessEver));
                    }

                    if (bestFitnessThisGen >= bestGaFitnessEver) {
                        gaGensWithoutImprovement = 0;
                    } else {
                        gaGensWithoutImprovement++;
                    }

                    if (gaGensWithoutImprovement >= gaPatience) {
                        publish("[GA] Early stopping triggered at generation " + gen);
                        break;
                    }

                    ga.evolve(population, groups, fitnessScores);
                    setProgress((int) (((gen + 1) / 200.0) * 100));
                }
                publish("[GA] Optimal Features Selected: " + bestGaFeatureSubset);

                // ------------------------------------------------------------
                // 7. RUN REAL PARTICLE SWARM OPTIMIZATION
                // ------------------------------------------------------------
                publish("\n--- Executing Particle Swarm Optimization (PSO) ---");
                PSO pso = new PSO(50, 100, 0.5, 1.5, 1.5);
                List<PSO.Particle> swarm = pso.initializeSwarm(numGroups, groups);

                double[] gBestPosition = null;
                bestPsoFitnessEver = -1;
                bestPsoFeatureSubset = null;

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

                        if (fitness > bestFitnessThisIter) bestFitnessThisIter = fitness;
                    }

                    if (iter % 5 == 0 || iter == pso.getIterations() - 1) {
                        publish(String.format("[PSO] Iter %02d | Best Fitness: %.4f", iter, bestPsoFitnessEver));
                    }

                    if (bestFitnessThisIter >= bestPsoFitnessEver) {
                        psoItersWithoutImprovement = 0;
                    } else {
                        psoItersWithoutImprovement++;
                    }

                    if (psoItersWithoutImprovement >= psoPatience) {
                        publish("[PSO] Early stopping triggered at iteration " + iter);
                        break;
                    }

                    if (gBestPosition != null) {
                        pso.updateSwarm(swarm, groups, gBestPosition);
                    }
                    setProgress(50 + (int) (((iter + 1) / 200.0) * 100));
                }
                publish("[PSO] Optimal Features Selected: " + bestPsoFeatureSubset);

                // Store split reference for Stage 2 evaluation
                cachedDataSplit = dataSplit;

                publish("\n=== OPTIMIZATION COMPLETE ===");
                publish("Click '2. Run Final Evaluation' to calculate metrics on the holdout test set.");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    log(msg);
                }
            }

            @Override
            protected void done() {
                progressBar.setValue(100);
                btnEvaluate.setEnabled(true);
                btnStartOptimization.setEnabled(true);
            }
        };

        worker.execute();
    }

    private void runFinalEvaluation() {
        if (cachedDataSplit == null) {
            log("No dataset processed yet.");
            return;
        }

        log("\n==================================================");
        log("          FINAL UNTOUCHED TEST SET EVALUATION      ");
        log("==================================================");

        double[][] finalTrainData = DataConverter.toFeatureArray(cachedDataSplit.getTrainSet());
        int[] finalTrainLabels = DataConverter.toLabelArray(cachedDataSplit.getTrainSet());

        double[][] testData = DataConverter.toFeatureArray(cachedDataSplit.getTestSet());
        int[] testLabels = DataConverter.toLabelArray(cachedDataSplit.getTestSet());

        KNN finalKnn = new KNN(5);

        // Evaluate GA
        Evaluation gaEval = finalKnn.evaluate(
                finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatureSubset
        );

        // Evaluate PSO
        Evaluation psoEval = finalKnn.evaluate(
                finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatureSubset
        );

        log(String.format("%-15s | %-10s | %-10s | %-12s", "Algorithm", "F1-Score", "G-Mean", "Feature Count"));
        log("----------------------------------------------------------------");
        log(String.format("%-15s | %-10.4f | %-10.4f | %-12d", 
                "GA Subset", gaEval.calculateF1Score(), gaEval.calculateGMean(), 
                bestGaFeatureSubset != null ? bestGaFeatureSubset.size() : 0));
        log(String.format("%-15s | %-10.4f | %-10.4f | %-12d", 
                "PSO Subset", psoEval.calculateF1Score(), psoEval.calculateGMean(), 
                bestPsoFeatureSubset != null ? bestPsoFeatureSubset.size() : 0));
        log("----------------------------------------------------------------");
        log("GA Selected Indices : " + bestGaFeatureSubset);
        log("PSO Selected Indices: " + bestPsoFeatureSubset);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UserInterface::new);
    }
}