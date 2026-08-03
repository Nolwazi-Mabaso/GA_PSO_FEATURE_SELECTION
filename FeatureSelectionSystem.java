import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FeatureSelectionSystem extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JLabel fileLabel;
    private File selectedFile;
    private JLabel fileStatusLabel;
    private JComboBox<String> trainSplitCombo;
    private JComboBox<String> optimizerCombo;
    private JLabel featuresLabel;
    private JLabel instancesLabel;
    private JCheckBox gaCheckBox;
    private JCheckBox saCheckBox;
    private JCheckBox psoCheckBox;
    private JCheckBox knnCheckBox;
    private JCheckBox naiveBayesCheckBox;
    private JCheckBox svmCheckBox;
    private JTextArea resultsArea;
    private JProgressBar progressBar;
    
    public FeatureSelectionSystem() {
        setTitle("Feature Selection System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700);
        setLocationRelativeTo(null);
        setResizable(false);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        initComponents();
        setVisible(true);
    }
    
    private void initComponents() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        JPanel uploadPanel = createUploadPanel();
        JPanel configPanel = createConfigPanel();
        JPanel runPanel = createRunPanel();
        JPanel resultsPanel = createResultsPanel();
        
        mainPanel.add(uploadPanel, "upload");
        mainPanel.add(configPanel, "config");
        mainPanel.add(runPanel, "run");
        mainPanel.add(resultsPanel, "results");
        
        add(mainPanel);
    }
    
    private JPanel createUploadPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        panel.setBackground(new Color(240, 248, 255));
        
        JLabel titleLabel = new JLabel("Feature Selection System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        JLabel uploadLabel = new JLabel("Upload Dataset");
        uploadLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(uploadLabel, gbc);

        JPanel filePanel = new JPanel(new BorderLayout(10, 0));
        filePanel.setBackground(new Color(240, 248, 255));
        
        fileLabel = new JLabel("No file selected");
        fileLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        fileLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        fileLabel.setPreferredSize(new Dimension(300, 30));
        fileLabel.setOpaque(true);
        fileLabel.setBackground(Color.WHITE);
        
        JButton browseButton = new JButton("Browse");
        browseButton.setBackground(new Color(0, 102, 204));
        browseButton.setForeground(Color.WHITE);
        browseButton.setFocusPainted(false);
        
        filePanel.add(fileLabel, BorderLayout.CENTER);
        filePanel.add(browseButton, BorderLayout.EAST);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 10, 20, 10);
        formPanel.add(filePanel, gbc);

        JLabel splitLabel = new JLabel("Train/Test Split");
        splitLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 5, 10);
        formPanel.add(splitLabel, gbc);
        
        trainSplitCombo = new JComboBox<>(new String[]{"60/40", "70/30", "80/20"});
        trainSplitCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        trainSplitCombo.setBackground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 10, 20, 10);
        formPanel.add(trainSplitCombo, gbc);

        JLabel optimizerLabel = new JLabel("Select Optimizer");
        optimizerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 5, 10);
        formPanel.add(optimizerLabel, gbc);
        
        optimizerCombo = new JComboBox<>(new String[]{
            "Genetic Algorithm", 
            "Particle Swarm Optimization", 
            "Simulated Annealing"
        });
        optimizerCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        optimizerCombo.setBackground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 10, 20, 10);
        formPanel.add(optimizerCombo, gbc);

        JButton continueButton = new JButton("Continue");
        continueButton.setFont(new Font("Arial", Font.BOLD, 14));
        continueButton.setBackground(new Color(0, 153, 76));
        continueButton.setForeground(Color.WHITE);
        continueButton.setFocusPainted(false);
        continueButton.setPreferredSize(new Dimension(150, 40));
        
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.insets = new Insets(10, 10, 10, 10);
        formPanel.add(continueButton, gbc);

        fileStatusLabel = new JLabel(" ");
        fileStatusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        fileStatusLabel.setForeground(Color.GRAY);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(fileStatusLabel, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        browseButton.addActionListener(e -> browseFile());
        
        continueButton.addActionListener(e -> {
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please upload a dataset file first.", 
                    "Warning", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            updateFileInfo();
            cardLayout.show(mainPanel, "config");
        });
        
        return panel;
    }
    
    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        panel.setBackground(new Color(240, 248, 255));
        
        JLabel titleLabel = new JLabel("Feature Selection System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        JPanel fileInfoPanel = new JPanel(new GridBagLayout());
        fileInfoPanel.setBackground(new Color(240, 248, 255));
        fileInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0, 102, 204)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        featuresLabel = new JLabel("Features: --");
        instancesLabel = new JLabel("Instances: --");
        featuresLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        instancesLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        GridBagConstraints infoGbc = new GridBagConstraints();
        infoGbc.insets = new Insets(5, 10, 5, 10);
        
        JLabel fileIconLabel = new JLabel("✓");
        fileIconLabel.setFont(new Font("Arial", Font.BOLD, 24));
        fileIconLabel.setForeground(new Color(0, 153, 76));
        infoGbc.gridx = 0;
        infoGbc.gridy = 0;
        infoGbc.gridheight = 2;
        fileInfoPanel.add(fileIconLabel, infoGbc);
        
        infoGbc.gridheight = 1;
        infoGbc.gridx = 1;
        infoGbc.gridy = 0;
        fileInfoPanel.add(new JLabel("bankruptcy_data.csv uploaded !!"), infoGbc);
        
        infoGbc.gridx = 1;
        infoGbc.gridy = 1;
        fileInfoPanel.add(featuresLabel, infoGbc);
        
        infoGbc.gridx = 2;
        infoGbc.gridy = 1;
        fileInfoPanel.add(instancesLabel, infoGbc);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        centerPanel.add(fileInfoPanel, gbc);
        
        JLabel optimizerTitle = new JLabel("Selected Optimizer");
        optimizerTitle.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 5, 10);
        centerPanel.add(optimizerTitle, gbc);
        
        JPanel optimizerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        optimizerPanel.setBackground(new Color(240, 248, 255));
        
        gaCheckBox = new JCheckBox("Genetic Algorithm");
        saCheckBox = new JCheckBox("Simulated Annealing");
        
        String selectedOptimizer = (String) optimizerCombo.getSelectedItem();
        if (selectedOptimizer.equals("Genetic Algorithm")) {
            gaCheckBox.setSelected(true);
        } else if (selectedOptimizer.equals("Simulated Annealing")) {
            saCheckBox.setSelected(true);
        }
        gaCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        saCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        gaCheckBox.setBackground(new Color(240, 248, 255));
        saCheckBox.setBackground(new Color(240, 248, 255));
        
        gaCheckBox.addActionListener(e -> {
            if (!gaCheckBox.isSelected() && !saCheckBox.isSelected()) {
                gaCheckBox.setSelected(true);
            }
        });
        saCheckBox.addActionListener(e -> {
            if (!gaCheckBox.isSelected() && !saCheckBox.isSelected()) {
                saCheckBox.setSelected(true);
            }
        });
        
        optimizerPanel.add(gaCheckBox);
        optimizerPanel.add(saCheckBox);
        
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 10, 20, 10);
        centerPanel.add(optimizerPanel, gbc);
        
        JLabel classifiersTitle = new JLabel("Classifiers:");
        classifiersTitle.setFont(new Font("Arial", Font.BOLD, 16));
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 10, 5, 10);
        centerPanel.add(classifiersTitle, gbc);
        
        JPanel classifiersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        classifiersPanel.setBackground(new Color(240, 248, 255));
        
        knnCheckBox = new JCheckBox("KNN");
        naiveBayesCheckBox = new JCheckBox("Naive Bayes");
        svmCheckBox = new JCheckBox("SVM");
        
        knnCheckBox.setSelected(true);
        naiveBayesCheckBox.setSelected(true);
        svmCheckBox.setSelected(true);
        
        knnCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        naiveBayesCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        svmCheckBox.setFont(new Font("Arial", Font.PLAIN, 14));
        knnCheckBox.setBackground(new Color(240, 248, 255));
        naiveBayesCheckBox.setBackground(new Color(240, 248, 255));
        svmCheckBox.setBackground(new Color(240, 248, 255));
        
        classifiersPanel.add(knnCheckBox);
        classifiersPanel.add(naiveBayesCheckBox);
        classifiersPanel.add(svmCheckBox);
        
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 10, 20, 10);
        centerPanel.add(classifiersPanel, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton backButton = new JButton("Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 12));
        backButton.setBackground(Color.LIGHT_GRAY);
        backButton.setFocusPainted(false);
        
        JButton startButton = new JButton("Start Experiment");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setBackground(new Color(0, 153, 76));
        startButton.setForeground(Color.WHITE);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(150, 40));
        
        buttonPanel.add(backButton);
        buttonPanel.add(startButton);
        
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        centerPanel.add(buttonPanel, gbc);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        backButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "upload");
        });
        
        startButton.addActionListener(e -> {
            if (!gaCheckBox.isSelected() && !saCheckBox.isSelected()) {
                JOptionPane.showMessageDialog(this, 
                    "Please select at least one optimizer.", 
                    "Warning", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            if (!knnCheckBox.isSelected() && !naiveBayesCheckBox.isSelected() && 
                !svmCheckBox.isSelected()) {
                JOptionPane.showMessageDialog(this, 
                    "Please select at least one classifier.", 
                    "Warning", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            cardLayout.show(mainPanel, "run");
            new Thread(() -> runExperiment()).start();
        });
        
        return panel;
    }
    
    private JPanel createRunPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        panel.setBackground(new Color(240, 248, 255));
        
        JLabel titleLabel = new JLabel("Feature Selection System", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(240, 248, 255));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JLabel runningLabel = new JLabel("Experiment Running...");
        runningLabel.setFont(new Font("Arial", Font.BOLD, 18));
        runningLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        centerPanel.add(runningLabel, gbc);
        
        JPanel stepsPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        stepsPanel.setBackground(new Color(240, 248, 255));
        stepsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        String[] steps = {
            "Loading Dataset",
            "Preprocessing Data",
            "Pearson Correlation Analysis",
            "Feature Selection",
            "Model Training",
            "Evaluation"
        };
        
        for (String step : steps) {
            JLabel stepLabel = new JLabel("  " + step);
            stepLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            stepLabel.setName(step);
            stepsPanel.add(stepLabel);
        }
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        centerPanel.add(stepsPanel, gbc);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(new Color(0, 153, 76));
        progressBar.setPreferredSize(new Dimension(400, 25));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        centerPanel.add(progressBar, gbc);
        
        JLabel progressLabel = new JLabel("Progress: 0%...");
        progressLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        progressLabel.setName("progressLabel");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 10, 10, 10);
        centerPanel.add(progressLabel, gbc);
        
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.putClientProperty("stepsPanel", stepsPanel);
        panel.putClientProperty("progressLabel", progressLabel);
        
        return panel;
    }
    
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.setBackground(new Color(240, 248, 255));
        
        JLabel titleLabel = new JLabel("Experiment Results", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(0, 102, 204));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resultsArea.setBackground(new Color(255, 255, 255));
        resultsArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton backButton = new JButton("Back to Upload");
        backButton.setFont(new Font("Arial", Font.BOLD, 12));
        backButton.setBackground(Color.LIGHT_GRAY);
        backButton.setFocusPainted(false);
        
        JButton newExperimentButton = new JButton("New Experiment");
        newExperimentButton.setFont(new Font("Arial", Font.BOLD, 12));
        newExperimentButton.setBackground(new Color(0, 102, 204));
        newExperimentButton.setForeground(Color.WHITE);
        newExperimentButton.setFocusPainted(false);
        
        buttonPanel.add(backButton);
        buttonPanel.add(newExperimentButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        backButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "upload");
            resetUI();
        });
        
        newExperimentButton.addActionListener(e -> {
            cardLayout.show(mainPanel, "upload");
            resetUI();
        });
        
        return panel;
    }
    
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            fileLabel.setText(selectedFile.getName());
            fileStatusLabel.setText("✓ File loaded successfully!");
            fileStatusLabel.setForeground(new Color(0, 153, 76));
        }
    }
    
    private void updateFileInfo() {
        if (selectedFile != null) {
            featuresLabel.setText("Features: 80 Features");
            instancesLabel.setText("Instances: 7000 Instances");
        }
    }
    
    private void runExperiment() {
        try {
            JPanel runPanel = (JPanel) mainPanel.getComponent(2);
            JPanel stepsPanel = (JPanel) runPanel.getClientProperty("stepsPanel");
            JLabel progressLabel = (JLabel) runPanel.getClientProperty("progressLabel");
            
            // Simulate experiment steps
            String[] steps = {"Loading Dataset", "Preprocessing Data", 
                "Pearson Correlation Analysis", "Feature Selection", 
                "Model Training", "Evaluation"};
            
            int progress = 0;
            for (int i = 0; i < steps.length; i++) {
                Component[] components = stepsPanel.getComponents();
                for (Component comp : components) {
                    if (comp instanceof JLabel) {
                        JLabel label = (JLabel) comp;
                        if (label.getName() != null && label.getName().equals(steps[i])) {
                            label.setText("✓ " + steps[i]);
                            label.setForeground(new Color(0, 153, 76));
                            break;
                        }
                    }
                }
                progress += 15;
                int finalProgress = Math.min(progress, 90);
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(finalProgress);
                    progressLabel.setText("Progress: " + finalProgress + "%...");
                });
                Thread.sleep(1000 + (int)(Math.random() * 1000));
            }
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(100);
                progressLabel.setText("Progress: 100% - Complete!");
            });
            
            Thread.sleep(500);
            SwingUtilities.invokeLater(() -> {
                displayResults();
                cardLayout.show(mainPanel, "results");
            });
            
        } catch (InterruptedException e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, 
                    "Error during experiment: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            });
        }
    }
    
    private void displayResults() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Algorithms Compared:\n");
        sb.append("- Genetic Algorithm\n");
        sb.append("- Simulated Annealing\n\n");
        
        sb.append("Performance Comparison\n");
        sb.append("=".repeat(50) + "\n\n");
        
        sb.append(String.format("%-20s %-15s %-15s\n", "", "GA", "SA"));
        sb.append("-".repeat(50) + "\n");

        sb.append(String.format("%-20s %-15s %-15s\n", "F1-score", "0.82", "0.85"));
        sb.append(String.format("%-20s %-15s %-15s\n", "G-Mean", "0.78", "0.81"));
        sb.append(String.format("%-20s %-15s %-15s\n", "Runtime", "25s", "18s"));
        
        sb.append("\n" + "=".repeat(50) + "\n\n");
        
        sb.append("Selected Features:\n");
        sb.append("- GA: F2, F7, F15, F31\n");
        sb.append("- SA: F1, F7, F20, F31\n\n");
        
        sb.append("Optimizer with best features:\n");
        sb.append("- Simulated Annealing\n");
        
        resultsArea.setText(sb.toString());
    }
    
    private void resetUI() {
        fileLabel.setText("No file selected");
        fileStatusLabel.setText(" ");
        selectedFile = null;

        gaCheckBox.setSelected(true);
        saCheckBox.setSelected(false);
        knnCheckBox.setSelected(true);
        naiveBayesCheckBox.setSelected(true);
        svmCheckBox.setSelected(true);
        
        progressBar.setValue(0);
        
        resultsArea.setText("");
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FeatureSelectionSystem());
    }
}