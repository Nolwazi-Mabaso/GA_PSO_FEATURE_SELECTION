import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;

public class UserInterface extends JFrame {

    private JTextField fileField;
    private JButton loadButton;
    private JLabel rawFeaturesLabel;
    private JLabel pearsonGroupsLabel;

    private JComboBox<String> classifierBox;
    private JButton runButton;
    private JButton exportButton;
    private JProgressBar progressBar;

    private JTextArea logArea;
    private JTextArea resultsArea;

    private String selectedFilePath = null;

    private List<Integer> bestGaFeatureSubset = null;
    private List<Integer> bestPsoFeatureSubset = null;
    private double[][] lastFinalTrainData = null;
    private int[] lastFinalTrainLabels = null;
    private double[][] lastTestData = null;
    private int[] lastTestLabels = null;

    public UserInterface() {
        super("GA vs PSO Feature Selection");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 750);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        add(buildTopPanel(), BorderLayout.NORTH);

        JPanel centerSplit = new JPanel(new BorderLayout());
        centerSplit.add(buildConfigPanel(), BorderLayout.WEST);
        centerSplit.add(buildConsolePanel(), BorderLayout.CENTER);
        add(centerSplit, BorderLayout.CENTER);
    }

    private JPanel buildTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("Data & Preprocessing"));

        fileField = new JTextField("No file selected", 30);
        fileField.setEditable(false);
        loadButton = new JButton("Load File");
        loadButton.addActionListener(e -> chooseFile());

        rawFeaturesLabel = new JLabel("Raw Features: -");
        pearsonGroupsLabel = new JLabel("Pearson Groups: -");

        topPanel.add(new JLabel("File:"));
        topPanel.add(fileField);
        topPanel.add(loadButton);
        topPanel.add(new JLabel("   |   "));
        topPanel.add(rawFeaturesLabel);
        topPanel.add(new JLabel("   |   "));
        topPanel.add(pearsonGroupsLabel);

        return topPanel;
    }

    private JPanel buildConfigPanel() {
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));
        westPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));
        westPanel.setPreferredSize(new Dimension(260, 0));

        JLabel classifierLabel = new JLabel("Final Evaluation Classifier:");
        classifierLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        classifierBox = new JComboBox<>(new String[]{"KNN", "Naive Bayes", "SVM"});
        classifierBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        classifierBox.setMaximumSize(new Dimension(220, 28));

        runButton = new JButton("RUN EXPERIMENT");
        runButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        runButton.setEnabled(false);
        runButton.addActionListener(e -> runPipeline());

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        exportButton = new JButton("Export Filtered CSVs");
        exportButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportFilteredCsv());

        westPanel.add(classifierLabel);
        westPanel.add(Box.createVerticalStrut(4));
        westPanel.add(classifierBox);
        westPanel.add(Box.createVerticalStrut(20));
        westPanel.add(runButton);
        westPanel.add(Box.createVerticalStrut(10));
        westPanel.add(progressBar);
        westPanel.add(Box.createVerticalStrut(20));
        westPanel.add(new JSeparator());
        westPanel.add(Box.createVerticalStrut(10));
        westPanel.add(exportButton);
        westPanel.add(Box.createVerticalGlue());

        return westPanel;
    }

    private JPanel buildConsolePanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Live Execution Log"));

        resultsArea = new JTextArea(10, 0);
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane resultsScroll = new JScrollPane(resultsArea);
        resultsScroll.setBorder(BorderFactory.createTitledBorder("Results Summary"));
        resultsScroll.setPreferredSize(new Dimension(0, 180));

        rightPanel.add(logScroll, BorderLayout.CENTER);
        rightPanel.add(resultsScroll, BorderLayout.SOUTH);
        return rightPanel;
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFilePath = chooser.getSelectedFile().getAbsolutePath();
            fileField.setText(chooser.getSelectedFile().getName());
            runButton.setEnabled(true);
            log("File selected: " + selectedFilePath);
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void setResults(String text) {
        SwingUtilities.invokeLater(() -> resultsArea.setText(text));
    }

    private void setProgress(int value) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(Math.min(value, 100)));
    }

    private void runPipeline() {
        runButton.setEnabled(false);
        loadButton.setEnabled(false);
        exportButton.setEnabled(false);
        logArea.setText("");
        resultsArea.setText("");
        progressBar.setValue(0);

        String classifierChoice = (String) classifierBox.getSelectedItem();
        new PipelineWorker(selectedFilePath, classifierChoice).execute();
    }

    private class PipelineWorker extends SwingWorker<Void, Void> {
        private final String filePath;
        private final String classifierChoice;

        PipelineWorker(String filePath, String classifierChoice) {
            this.filePath = filePath;
            this.classifierChoice = classifierChoice;
        }

        @Override
        protected Void doInBackground() {
            try {
                runFullPipeline(filePath, classifierChoice);
            } catch (Exception ex) {
                log("ERROR: " + ex);
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(UserInterface.this,
                                "Something went wrong:\n" + ex,
                                "Error", JOptionPane.ERROR_MESSAGE));
            }
            return null;
        }

        @Override
        protected void done() {
            runButton.setEnabled(true);
            loadButton.setEnabled(true);
            exportButton.setEnabled(bestGaFeatureSubset != null && bestPsoFeatureSubset != null);
            progressBar.setValue(100);
            log("\nDone. Upload a new file or press Run again to repeat.");
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(UserInterface.this,
                            "Run complete. See the results summary panel.",
                            "Finished", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    private void runFullPipeline(String filePath, String classifierChoice) throws Exception {

        log("STEP 1: Loading data...");
        LoadData loader = new LoadData(filePath);
        if (!loader.load()) {
            log("Could not load data. Stopping.");
            return;
        }
        List<String[]> rawData = loader.getData();
        log("Data loaded.");

        int labelColumn = 0;
        rawData = moveColumnToEnd(rawData, labelColumn);

        int rowCount = rawData.size();
        int rawFeatureCount = rowCount > 0 ? rawData.get(0).length - 1 : 0;
        SwingUtilities.invokeLater(() -> rawFeaturesLabel.setText("Raw Features: " + rawFeatureCount));
        log("Total rows: " + rowCount + " | Total raw features: " + rawFeatureCount);

        log("\nSTEP 2: Preprocessing started...");
        Preprocessor preprocessor = new Preprocessor(rawData);
        List<String[]> processedData = preprocessor.preprocess();
        List<List<Integer>> groups = preprocessor.getGroups();
        SwingUtilities.invokeLater(() -> pearsonGroupsLabel.setText("Pearson Groups: " + groups.size()));
        log("Preprocessing done.");

        log("\nFEATURE GROUPS:");
        log("");
        log("Total Groups Formed: " + groups.size());
        log("--------------------------------------------------");
        for (int i = 0; i < groups.size(); i++) {
            List<Integer> g = groups.get(i);
            if (g.size() > 1) {
                log("Group " + (i + 1) + " [" + g.size() + " correlated features] -- Columns: " + g);
            } else {
                log("Group " + (i + 1) + " [Independent] -- Column " + g.get(0));
            }
        }
        log("_______________________________________________");
        setProgress(10);

        log("\nSTEP 3: Splitting data (train/test, then train/validation)...");
        DataSplitter dataSplitter = new DataSplitter();
        DataSplit dataSplit = dataSplitter.splitData(processedData, 0.7);

        ValidationSplitter validationSplitter = new ValidationSplitter();
        ValidationSplit validationSplit =
                validationSplitter.splitValidation(dataSplit.getTrainSet(), 0.8);

        double[][] innerTrainData = DataConverter.toFeatureArray(validationSplit.getTrainingSet());
        int[] innerTrainLabels = DataConverter.toLabelArray(validationSplit.getTrainingSet());

        double[][] validationData = DataConverter.toFeatureArray(validationSplit.getValidationSet());
        int[] validationLabels = DataConverter.toLabelArray(validationSplit.getValidationSet());
        log("Split complete.");
        setProgress(15);

        int k = 5;
        FitnessFunction fitnessFunction = new FitnessFunction(k);
        int numGroups = groups.size();

        log("\nSTARTING GENETIC ALGORITHM (GA)");
        log("________________________________");

        int gaPopSize = 50;
        int gaGenerations = 100;
        double crossoverRate = 0.8;
        double mutationRate = 0.01;

        GeneticAlgorithm ga = new GeneticAlgorithm(gaPopSize, gaGenerations, crossoverRate, mutationRate);
        List<List<Integer>> population = ga.initializePopulation(numGroups, groups);

        double bestGaFitnessEver = -1;
        int gaPatience = 15;
        int gaGensWithoutImprovement = 0;

        for (int gen = 0; gen < ga.getGenerations(); gen++) {
            List<Double> fitnessScores = new ArrayList<>();
            double bestFitnessThisGen = -1;

            for (List<Integer> chromosome : population) {
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

            log(String.format("Gen %02d: Best Solution = %s | Fitness = %.4f", gen + 1, bestGaFeatureSubset, bestGaFitnessEver));

            setProgress(15 + (int) (35.0 * (gen + 1) / gaGenerations));

            gaGensWithoutImprovement = (bestFitnessThisGen >= bestGaFitnessEver) ? 0 : gaGensWithoutImprovement + 1;
            if (gaGensWithoutImprovement >= gaPatience) {
                log("GA stopping early: no improvement in the last " + gaPatience + " generations.");
                break;
            }
            ga.evolve(population, groups, fitnessScores);
        }

        log("\nGA finished. Best feature subset: " + bestGaFeatureSubset);
        log("GA best fitness: " + bestGaFitnessEver);
        setProgress(50);

        log("\nSTARTING PARTICLE SWARM OPTIMIZATION (PSO)");
        log("___________________________________________");

        int swarmSize = 50;
        int psoIterations = 100;
        double inertiaWeight = 0.5;
        double cognitiveWeight = 1.5;
        double socialWeight = 1.5;

        PSO pso = new PSO(swarmSize, psoIterations, inertiaWeight, cognitiveWeight, socialWeight);
        List<PSO.Particle> swarm = pso.initializeSwarm(numGroups, groups);

        double[] gBestPosition = null;
        double bestPsoFitnessEver = -1;
        int psoPatience = 15;
        int psoItersWithoutImprovement = 0;

        for (int iter = 0; iter < pso.getIterations(); iter++) {
            double bestFitnessThisIter = -1;

            for (PSO.Particle particle : swarm) {
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

            log(String.format("Iter %02d: Best Solution = %s | Fitness = %.4f", iter + 1, bestPsoFeatureSubset, bestPsoFitnessEver));

            setProgress(50 + (int) (35.0 * (iter + 1) / psoIterations));

            psoItersWithoutImprovement = (bestFitnessThisIter >= bestPsoFitnessEver) ? 0 : psoItersWithoutImprovement + 1;
            if (psoItersWithoutImprovement >= psoPatience) {
                log("PSO stopping early: no improvement in the last " + psoPatience + " iterations.");
                break;
            }
            if (gBestPosition != null) {
                pso.updateSwarm(swarm, groups, gBestPosition);
            }
        }

        log("\nPSO finished. Best feature subset: " + bestPsoFeatureSubset);
        log("PSO best fitness: " + bestPsoFitnessEver);
        setProgress(85);

        log("\nSTEP: Starting final evaluation using " + classifierChoice + " on the held-out test set...");

        double[][] finalTrainData = DataConverter.toFeatureArray(dataSplit.getTrainSet());
        int[] finalTrainLabels = DataConverter.toLabelArray(dataSplit.getTrainSet());
        double[][] testData = DataConverter.toFeatureArray(dataSplit.getTestSet());
        int[] testLabels = DataConverter.toLabelArray(dataSplit.getTestSet());

        lastFinalTrainData = finalTrainData;
        lastFinalTrainLabels = finalTrainLabels;
        lastTestData = testData;
        lastTestLabels = testLabels;

        Evaluation gaEvaluation;
        Evaluation psoEvaluation;

        if ("KNN".equals(classifierChoice)) {
            KNN finalKnn = new KNN(k);
            gaEvaluation = finalKnn.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatureSubset);
            psoEvaluation = finalKnn.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatureSubset);
        } else {
            log(classifierChoice + " is not wired up yet - falling back to KNN for this run.");
            KNN finalKnn = new KNN(k);
            gaEvaluation = finalKnn.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestGaFeatureSubset);
            psoEvaluation = finalKnn.evaluate(finalTrainData, finalTrainLabels, testData, testLabels, bestPsoFeatureSubset);
        }

        setProgress(95);

        log("\n___________________________________________");
        log("FINAL TEST SET COMPARISON");
        log("___________________________________________");
        log("--- Genetic Algorithm (GA) ---");
        log("F1 Score: " + gaEvaluation.calculateF1Score());
        log("G-Mean:   " + gaEvaluation.calculateGMean());
        log("\n--- Particle Swarm Optimization (PSO) ---");
        log("F1 Score: " + psoEvaluation.calculateF1Score());
        log("G-Mean:   " + psoEvaluation.calculateGMean());

        double gaF1 = gaEvaluation.calculateF1Score();
        double gaGMean = gaEvaluation.calculateGMean();
        double psoF1 = psoEvaluation.calculateF1Score();
        double psoGMean = psoEvaluation.calculateGMean();
        int denom = groups.size();
        double gaFitnessFinal = bestGaFitnessEver;
        double psoFitnessFinal = bestPsoFitnessEver;
        int gaCount = bestGaFeatureSubset.size();
        int psoCount = bestPsoFeatureSubset.size();

        String summary =
                "GENETIC ALGORITHM (GA)\n" +
                "  Best Fitness:       " + String.format("%.4f", gaFitnessFinal) + "\n" +
                "  Features Selected:  " + gaCount + " / " + denom + "\n" +
                "  Test F1 Score:      " + String.format("%.4f", gaF1) + "\n" +
                "  Test G-Mean:        " + String.format("%.4f", gaGMean) + "\n\n" +
                "PARTICLE SWARM OPTIMIZATION (PSO)\n" +
                "  Best Fitness:       " + String.format("%.4f", psoFitnessFinal) + "\n" +
                "  Features Selected:  " + psoCount + " / " + denom + "\n" +
                "  Test F1 Score:      " + String.format("%.4f", psoF1) + "\n" +
                "  Test G-Mean:        " + String.format("%.4f", psoGMean) + "\n";
        setResults(summary);
    }

    private void exportFilteredCsv() {
        if (bestGaFeatureSubset == null || bestPsoFeatureSubset == null || lastFinalTrainData == null) {
            JOptionPane.showMessageDialog(this, "Run an experiment first.", "Nothing to export", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a folder to save gaData.csv and psoData.csv");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        String folder = chooser.getSelectedFile().getAbsolutePath();
        writeCsv(folder + "/gaData.csv", bestGaFeatureSubset);
        writeCsv(folder + "/psoData.csv", bestPsoFeatureSubset);
        log("\nExported gaData.csv and psoData.csv to: " + folder);
    }

    private void writeCsv(String path, List<Integer> subset) {
        try (FileWriter writer = new FileWriter(path)) {
            for (int col : subset) {
                writer.write("feature_" + col + ",");
            }
            writer.write("label\n");

            for (int r = 0; r < lastFinalTrainData.length; r++) {
                for (int col : subset) {
                    writer.write(lastFinalTrainData[r][col] + ",");
                }
                writer.write(lastFinalTrainLabels[r] + "\n");
            }
            for (int r = 0; r < lastTestData.length; r++) {
                for (int col : subset) {
                    writer.write(lastTestData[r][col] + ",");
                }
                writer.write(lastTestLabels[r] + "\n");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Export failed:\n" + ex, "Error", JOptionPane.ERROR_MESSAGE);
        }
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserInterface().setVisible(true));
    }
}