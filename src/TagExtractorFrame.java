import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class TagExtractorFrame extends JFrame {
    private JTextArea tagTextArea;
    private JLabel documentLabel, stopWordLabel;
    private JButton loadDocButton, loadStopButton, extractButton, saveButton, quitButton;

    private Path documentPath, stopWordPath;
    private Set<String> stopWords = new HashSet<>();
    private Map<String, Integer> tagMap = new HashMap<>();

    public TagExtractorFrame() {
        super("Tag Extractor");

        JPanel topPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        documentLabel = new JLabel("Document: (none selected)");
        stopWordLabel = new JLabel("Stop words file: (none selected)");

        JPanel buttonPanel = new JPanel();
        loadDocButton = new JButton("Load Document");
        loadStopButton = new JButton("Load Stop Words");
        extractButton = new JButton("Extract Tags");
        saveButton = new JButton("Save Tags");
        quitButton = new JButton("Quit");

        buttonPanel.add(loadDocButton);
        buttonPanel.add(loadStopButton);
        buttonPanel.add(extractButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(quitButton);

        topPanel.add(documentLabel);
        topPanel.add(stopWordLabel);
        topPanel.add(buttonPanel);
        add(topPanel, BorderLayout.NORTH);

        tagTextArea = new JTextArea();
        tagTextArea.setEditable(false);
        tagTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(tagTextArea);
        add(scrollPane, BorderLayout.CENTER);


        loadDocButton.addActionListener(e -> chooseFile(true));
        loadStopButton.addActionListener(e -> chooseFile(false));
        extractButton.addActionListener(e -> extractTags());
        saveButton.addActionListener(e -> saveTags());
        quitButton.addActionListener(e -> System.exit(0));

        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void chooseFile(boolean isDoc) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            if (isDoc) {
                documentPath = f.toPath();
                documentLabel.setText("Document: " + f.getName());
            } else {
                stopWordPath = f.toPath();
                stopWordLabel.setText("Stop words: " + f.getName());
                try (Stream<String> lines = Files.lines(stopWordPath)) {
                    stopWords = lines.map(String::trim).map(String::toLowerCase)
                            .filter(s -> !s.isEmpty()).collect(Collectors.toSet());
                } catch (IOException ex) {
                    showError("Error loading stop words: " + ex.getMessage());
                }
            }
        }
    }

    private void extractTags() {
        if (documentPath == null) {
            showError("Select a document first.");
            return;
        }
        tagMap.clear();
        try (Stream<String> lines = Files.lines(documentPath)) {
            lines.flatMap(line -> Arrays.stream(line.split("\\s+")))
                    .map(w -> w.toLowerCase().replaceAll("[^a-z]", ""))
                    .filter(w -> !w.isEmpty() && !stopWords.contains(w))
                    .forEach(w -> tagMap.merge(w, 1, Integer::sum));
        } catch (IOException ex) {
            showError("Error reading document: " + ex.getMessage());
        }
        displayTags();
    }

    private void displayTags() {
        if (tagMap.isEmpty()) {
            tagTextArea.setText("(No tags found.)");
            return;
        }
        StringBuilder sb = new StringBuilder("TAG\tFREQ\n----------------\n");
        tagMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sb.append(String.format("%-15s %d%n", e.getKey(), e.getValue())));
        tagTextArea.setText(sb.toString());
        tagTextArea.setCaretPosition(0);
    }

    private void saveTags() {
        if (tagMap.isEmpty()) {
            showError("No tags to save.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedWriter w = Files.newBufferedWriter(chooser.getSelectedFile().toPath())) {
                w.write("TAG,FREQ\n");
                for (var e : tagMap.entrySet()) {
                    w.write(e.getKey() + "," + e.getValue() + "\n");
                }
            } catch (IOException ex) {
                showError("Error saving: " + ex.getMessage());
            }
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }


}
