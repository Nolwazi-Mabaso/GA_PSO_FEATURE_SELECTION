import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class MainGUI extends JFrame {

    private JTextField filePathField;
    private JButton runButton;
    private JProgressBar progressBar;
    private JTextArea logArea;

    private JRadioButton knnRadio;
    private JRadioButton nbRadio;
    private JRadioButton svmRadio;
    private ButtonGroup classifierGroup;

    private JLabel gaFeaturesLabel, gaPrecisionLabel, gaRecallLabel, gaF1Label, gaGMeanLabel, gaTimeLabel;

    private JLabel psoFeaturesLabel, psoPrecisionLabel, psoRecallLabel, psoF1Label, psoGMeanLabel, psoTimeLabel;

    public MainGUI() {
        setTitle("Feature Selection Framework — GA vs. PSO");
        setSize(1100, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(createCustomTitledBorder("Dataset Configuration"));

        filePathField = new JTextField("data.csv");
        filePathField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        
        runButton = new JButton("Run Optimization");
        runButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        runButton.setFocusPainted(false);
        runButton.addActionListener(e -> onRunClicked());

        topPanel.add(new JLabel(" File Path: "), BorderLayout.WEST);
        topPanel.add(filePathField, BorderLayout.CENTER);
        topPanel.add(runButton, BorderLayout.EAST);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setPreferredSize(new Dimension(240, 0));

        JPanel classifierPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        classifierPanel.setBorder(createCustomTitledBorder("Classifier Selection"));

        knnRadio = new JRadioButton("K-Nearest Neighbors (KNN)", true);
        nbRadio = new JRadioButton("Naive Bayes");
        svmRadio = new JRadioButton("Support Vector Machine (SVM)");

        classifierGroup = new ButtonGroup();
        classifierGroup.add(knnRadio);
        classifierGroup.add(nbRadio);
        classifierGroup.add(svmRadio);

        classifierPanel.add(knnRadio);
        classifierPanel.add(nbRadio);
        classifierPanel.add(svmRadio);

        JPanel paramsPanel = new JPanel(new GridLayout(6, 2, 5, 8));
        paramsPanel.setBorder(createCustomTitledBorder("System Parameters"));

        paramsPanel.add(new JLabel("Population Size:"));
        paramsPanel.add(new JLabel("50"));
        paramsPanel.add(new JLabel("Generations:"));
        paramsPanel.add(new JLabel("100"));
        paramsPanel.add(new JLabel("Mutation Rate:"));
        paramsPanel.add(new JLabel("0.05"));
        paramsPanel.add(new JLabel("Crossover Rate:"));
        paramsPanel.add(new JLabel("0.80"));
        paramsPanel.add(new JLabel("Inertia (w):"));
        paramsPanel.add(new JLabel("0.729"));
        paramsPanel.add(new JLabel("Cognitive (c1/c2):"));
        paramsPanel.add(new JLabel("1.494"));

        leftPanel.add(classifierPanel);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(paramsPanel);
        leftPanel.add(Box.createVerticalGlue());

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(new Color(250, 250, 250));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(createCustomTitledBorder("Execution Console Log"));

        JTabbedPane resultsTabbedPane = new JTabbedPane();
        resultsTabbedPane.setFont(new Font("SansSerif", Font.BOLD, 12));
        resultsTabbedPane.addTab("GA Results", createStyledMetricsPanel(true));
        resultsTabbedPane.addTab("PSO Results", createStyledMetricsPanel(false));

        JSplitPane centerSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logScrollPane, resultsTabbedPane);
        centerSplitPane.setResizeWeight(0.55);
        centerSplitPane.setBorder(null);

        JPanel mainContentPanel = new JPanel(new BorderLayout(10, 0));
        mainContentPanel.add(leftPanel, BorderLayout.WEST);
        mainContentPanel.add(centerSplitPane, BorderLayout.CENTER);

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Ready");
        progressBar.setPreferredSize(new Dimension(progressBar.getPreferredSize().width, 22));

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(mainContentPanel, BorderLayout.CENTER);
        mainPanel.add(progressBar, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createStyledMetricsPanel(boolean isGA) {
        JPanel panel = new JPanel(new GridLayout(2, 3, 12, 12));
        panel.setBorder(new EmptyBorder(12, 10, 10, 10));
        panel.setBackground(new Color(245, 246, 248));

        JLabel featuresVal = createValueLabel();
        JLabel precisionVal = createValueLabel();
        JLabel recallVal = createValueLabel();
        JLabel f1Val = createValueLabel();
        JLabel gmeanVal = createValueLabel();
        JLabel timeVal = createValueLabel();

        if (isGA) {
            gaFeaturesLabel = featuresVal;
            gaPrecisionLabel = precisionVal;
            gaRecallLabel = recallVal;
            gaF1Label = f1Val;
            gaGMeanLabel = gmeanVal;
            gaTimeLabel = timeVal;
        } else {
            psoFeaturesLabel = featuresVal;
            psoPrecisionLabel = precisionVal;
            psoRecallLabel = recallVal;
            psoF1Label = f1Val;
            psoGMeanLabel = gmeanVal;
            psoTimeLabel = timeVal;
        }

        panel.add(createMetricCard("Selected Features", featuresVal, new Color(220, 235, 252)));
        panel.add(createMetricCard("Precision", precisionVal, new Color(230, 245, 230)));
        panel.add(createMetricCard("Recall", recallVal, new Color(255, 243, 224)));
        panel.add(createMetricCard("F1 Score", f1Val, new Color(237, 231, 246)));
        panel.add(createMetricCard("G-Mean", gmeanVal, new Color(224, 247, 250)));
        panel.add(createMetricCard("Execution Runtime", timeVal, new Color(245, 245, 245)));

        return panel;
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color bgColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(bgColor);
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel titleLabel = new JLabel(title.toUpperCase());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        titleLabel.setForeground(new Color(90, 95, 105));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JLabel createValueLabel() {
        JLabel label = new JLabel("-", SwingConstants.LEFT);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setForeground(new Color(30, 35, 45));
        return label;
    }

    private TitledBorder createCustomTitledBorder(String title) {
        Font font = new Font("SansSerif", Font.BOLD, 12);
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), title,
                TitledBorder.LEFT, TitledBorder.TOP, font, new Color(60, 60, 60)
        );
    }

    public String getSelectedClassifier() {
        if (knnRadio.isSelected()) return "KNN";
        if (nbRadio.isSelected()) return "Naive Bayes";
        if (svmRadio.isSelected()) return "SVM";
        return "KNN";
    }

    private void onRunClicked() {
        String filePath = filePathField.getText().trim();
        File file = new File(filePath);

        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File '" + filePath + "' not found!", "Dataset Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        resetUIState();
        appendLog("Selected Classifier: " + getSelectedClassifier());

        Runner runner = new Runner(filePath, this);
        runner.execute();
    }

    private void resetUIState() {
        runButton.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);
        progressBar.setString("Executing algorithms...");

        resetMetricLabels(gaFeaturesLabel, gaPrecisionLabel, gaRecallLabel, gaF1Label, gaGMeanLabel, gaTimeLabel);
        resetMetricLabels(psoFeaturesLabel, psoPrecisionLabel, psoRecallLabel, psoF1Label, psoGMeanLabel, psoTimeLabel);
    }

    private void resetMetricLabels(JLabel... labels) {
        for (JLabel label : labels) {
            label.setText("-");
        }
    }
    public void appendLog(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void setProgressValue(int progress) {
        progressBar.setValue(progress);
    }

    public void setProgressStatus(String status) {
        progressBar.setString(status);
    }

    public void setRunButtonEnabled(boolean enabled) {
        runButton.setEnabled(enabled);
    }

    public void updateGaResults(Evaluation eval, long durationMs, List<Integer> selectedFeatures) {
        updateLabels(gaFeaturesLabel, gaPrecisionLabel, gaRecallLabel, gaF1Label, gaGMeanLabel, gaTimeLabel, eval, durationMs, selectedFeatures);
    }

    public void updatePsoResults(Evaluation eval, long durationMs, List<Integer> selectedFeatures) {
        updateLabels(psoFeaturesLabel, psoPrecisionLabel, psoRecallLabel, psoF1Label, psoGMeanLabel, psoTimeLabel, eval, durationMs, selectedFeatures);
    }

    private void updateLabels(JLabel features, JLabel precision, JLabel recall, JLabel f1, JLabel gmean, JLabel time,
                              Evaluation eval, long durationMs, List<Integer> selectedFeatures) {
        features.setText(String.valueOf(selectedFeatures != null ? selectedFeatures.size() : 0));
        precision.setText(String.format("%.4f", eval.calculatePrecision()));
        recall.setText(String.format("%.4f", eval.calculateRecall()));
        f1.setText(String.format("%.4f", eval.calculateF1Score()));
        gmean.setText(String.format("%.4f", eval.calculateGMean()));
        time.setText(String.format("%d ms", durationMs));
    }
}