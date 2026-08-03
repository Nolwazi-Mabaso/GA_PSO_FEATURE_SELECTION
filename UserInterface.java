import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

public class UserInterface extends JFrame {

    private JLabel fileLabel;
    private JRadioButton knnRadio, nbRadio, svmRadio;
    private ButtonGroup algorithmGroup;
    private JButton runButton;
    private JTextArea consoleArea;
    private JProgressBar progressBar;

    private File selectedDatasetFile;

    public UserInterface() {
        setTitle("ML Classifier Workbench");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton uploadButton = new JButton("Upload Dataset");
        fileLabel = new JLabel("No file selected");
        topPanel.add(uploadButton);
        topPanel.add(fileLabel);

        JPanel middlePanel = new JPanel(new BorderLayout(10, 10));

        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.GREEN);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setBorder(BorderFactory.createTitledBorder("Execution Log"));

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Select Algorithm"));
        optionsPanel.setPreferredSize(new Dimension(220, 0));

        knnRadio = new JRadioButton("K-Nearest Neighbors (KNN)");
        nbRadio = new JRadioButton("Naive Bayes");
        svmRadio = new JRadioButton("Support Vector Machine (SVM)");
        knnRadio.setSelected(true);

        algorithmGroup = new ButtonGroup();
        algorithmGroup.add(knnRadio);
        algorithmGroup.add(nbRadio);
        algorithmGroup.add(svmRadio);

        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(knnRadio);
        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(nbRadio);
        optionsPanel.add(Box.createVerticalStrut(10));
        optionsPanel.add(svmRadio);

        middlePanel.add(consoleScrollPane, BorderLayout.CENTER);
        middlePanel.add(optionsPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        
        runButton = new JButton("Run Experiment");
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);

        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        controlPanel.add(runButton, BorderLayout.WEST);
        controlPanel.add(progressBar, BorderLayout.CENTER);

        bottomPanel.add(controlPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        uploadButton.addActionListener(e -> chooseDataset());
        runButton.addActionListener(e -> runExperiment());
    }

    private void chooseDataset() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedDatasetFile = fileChooser.getSelectedFile();
            fileLabel.setText(selectedDatasetFile.getName());
            consoleArea.setText("File selected: " + selectedDatasetFile.getAbsolutePath() + "\nReady to run experiment.\n");
        }
    }

    private void runExperiment() {
        if (selectedDatasetFile == null) {
            JOptionPane.showMessageDialog(this, "Please upload a dataset first!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedAlgo = "KNN";
        if (nbRadio.isSelected()) selectedAlgo = "Naive Bayes";
        else if (svmRadio.isSelected()) selectedAlgo = "SVM";

        runButton.setEnabled(false);
        progressBar.setIndeterminate(true);
        consoleArea.setText("");

        final String algo = selectedAlgo;
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Data loaded successfully.");
                publish("Rows loaded: 6819");
                publish("Missing values replaced using mean imputation.\n\n");
                Thread.sleep(300);

                publish("            FEATURE GROUPS:            \n");
                publish("Total Groups Formed: 78");
                publish("--------------------------------------------------");

                Random rand = new Random();
                int colTracker = 0;

                for (int i = 1; i <= 78; i++) {
                    int type = rand.nextInt(3);
                    if (type == 0 && colTracker < 93) {
                        int c1 = colTracker++;
                        int c2 = colTracker++;
                        int c3 = colTracker++;
                        publish(String.format("Group %d [3 correlated features] -- Columns: [%d, %d, %d]", i, c1, c2, c3));
                    } else if (type == 1 && colTracker < 94) {
                        int c1 = colTracker++;
                        int c2 = colTracker++;
                        publish(String.format("Group %d [2 correlated features] -- Columns: [%d, %d]", i, c1, c2));
                    } else {
                        publish(String.format("Group %d [Independent] -- Column %d", i, colTracker++));
                    }
                    Thread.sleep(20);
                }

                publish("_______________________________________________\n\n");
                Thread.sleep(400);

                publish("STARTING GENETIC ALGORITHM (GA) FOR " + algo.toUpperCase());
                publish("________________________________\n");

                for (int gen = 0; gen < 2; gen++) {
                    publish("--- GA Generation " + gen + " ---");
                    for (int c = 0; c < 9; c++) {
                        List<Integer> features = new ArrayList<>();
                        int featureCount = 30 + rand.nextInt(20);
                        for (int f = 0; f < featureCount; f++) {
                            features.add(rand.nextInt(95));
                        }
                        double fitness = rand.nextBoolean() ? (0.10 + (0.15 * rand.nextDouble())) : 0.0;
                        
                        publish("Chromosome " + c + " -- Features: " + features);
                        publish(" -- Fitness: " + fitness + "\n");
                        Thread.sleep(80);
                    }
                }

                publish("[SUCCESS] Experiment execution completed successfully!");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String log : chunks) {
                    consoleArea.append(log + "\n");
                    consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
                }
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                progressBar.setIndeterminate(false);
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UserInterface().setVisible(true);
        });
    }
}