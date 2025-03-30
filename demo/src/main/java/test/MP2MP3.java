package test;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

public class MP2MP3 extends JFrame {

    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private Map<String, Double> attendanceAverages;

    public MP2MP3() {
        setTitle("Grade vs. Attendance Analysis");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        String[] columnNames = {"Student ID", "First Name", "Last Name", "Email", "Gender", "Age", "Department", "Attendance (%)", "Midterm Score", "Final Score", "Assignment Avg", "Quizzes Avg", "Participation Score", "Projects Score", "Total Score", "Grade", "Study Hours", "Extracurricular", "Internet Access", "Parent Education", "Family Income", "Stress Level", "Sleep Hours"};
        tableModel = new DefaultTableModel(columnNames, 0);
        dataTable = new JTable(tableModel);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            dataTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(dataTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton loadButton = new JButton("Load CSV");
        JButton analyzeButton = new JButton("Analyze");
        JButton downloadButton = new JButton("Download CSV");
        JButton summaryButton = new JButton("Summary");
        JButton filterButton = new JButton("Missing Attendance");
        JButton animateButton = new JButton("Animate");

        buttonPanel.add(loadButton);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(summaryButton);
        buttonPanel.add(filterButton);
        buttonPanel.add(animateButton);
        add(buttonPanel, BorderLayout.NORTH);

        chartPanel = new JPanel();
        add(chartPanel, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> loadCSV());
        analyzeButton.addActionListener(e -> analyzeData());
        downloadButton.addActionListener(e -> downloadCSV());
        summaryButton.addActionListener(e -> showSummary());
        filterButton.addActionListener(e -> filterMissingAttendance());
        animateButton.addActionListener(e -> animateChart());
    }

    private void loadCSV() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
        fileChooser.setFileFilter(filter);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                tableModel.setRowCount(0);
                String line;
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }
                    String[] values = line.split(",");
                    tableModel.addRow(values);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading file: " + e.getMessage());
            }
        }
    }

    private void analyzeData() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to analyze. Please load a CSV file first.");
            return;
        }

        // Process the data and create the chart
        attendanceAverages = processCSV();
        createChart(attendanceAverages);

        // Set the window to fullscreen
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private Map<String, Double> processCSV() {
        Map<String, GradeData> grades = new HashMap<>();
        grades.put("A", new GradeData());
        grades.put("B", new GradeData());
        grades.put("C", new GradeData());
        grades.put("D", new GradeData());
        grades.put("F", new GradeData());

        int attendanceIndex = -1;
        int gradeIndex = -1;
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String columnName = tableModel.getColumnName(i);
            if (columnName.equalsIgnoreCase("Attendance (%)")) {
                attendanceIndex = i;
            } else if (columnName.equalsIgnoreCase("Grade")) {
                gradeIndex = i;
            }
        }

        if (attendanceIndex == -1 || gradeIndex == -1) {
            JOptionPane.showMessageDialog(this, "Invalid CSV format. Missing required columns.");
            return new HashMap<>();
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String grade = (String) tableModel.getValueAt(i, gradeIndex);
            try {
                double attendance = Double.parseDouble((String) tableModel.getValueAt(i, attendanceIndex));
                if (grades.containsKey(grade)) {
                    grades.get(grade).sum += attendance;
                    grades.get(grade).count++;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid attendance value on row " + (i + 1));
            }
        }

        Map<String, Double> attendanceAverages = new HashMap<>();
        for (Map.Entry<String, GradeData> entry : grades.entrySet()) {
            GradeData data = entry.getValue();
            attendanceAverages.put(entry.getKey(), data.count > 0 ? data.sum / data.count : 0.0);
        }
        return attendanceAverages;
    }

    private void createChart(Map<String, Double> attendanceAverages) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> entry : attendanceAverages.entrySet()) {
            dataset.addValue(entry.getValue(), entry.getKey(), "Attendance"); // Use grade as the series key
        }
    
        JFreeChart chart = ChartFactory.createBarChart(
                "Average Attendance (%) by Grade",
                "Grade",
                "Average Attendance (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
    
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
    
        // Assign different colors for each grade dynamically
        Map<String, Color> gradeColors = new HashMap<>();
        gradeColors.put("A", Color.BLUE);
        gradeColors.put("B", Color.GREEN);
        gradeColors.put("C", Color.YELLOW);
        gradeColors.put("D", Color.RED);
        gradeColors.put("F", Color.MAGENTA);
    
        for (int i = 0; i < dataset.getRowCount(); i++) {
            String grade = (String) dataset.getRowKey(i);
            renderer.setSeriesPaint(i, gradeColors.getOrDefault(grade, Color.GRAY)); // Default to gray if grade not found
        }
    
        plot.getRangeAxis().setRange(0.0, 100.0);
    
        ChartPanel newChartPanel = new ChartPanel(chart);
        chartPanel.removeAll();
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(newChartPanel, BorderLayout.CENTER);
        chartPanel.validate();
    }

    private void downloadCSV() {
        if (attendanceAverages == null || attendanceAverages.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please analyze the data first.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("GradevsAttendance.csv"));
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(selectedFile)) {
                writer.write("Grades,Average Attendance (%)\n");
                for (Map.Entry<String, Double> entry : attendanceAverages.entrySet()) {
                    writer.write(entry.getKey() + "," + String.format("%.3f", entry.getValue()) + "\n");
                }
                writer.write("\n\n");
                writer.write(getSummary());
                JOptionPane.showMessageDialog(this, "CSV file saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
            }
        }
    }

    private String getSummary() {
        StringBuilder summary = new StringBuilder("Summary:\n");
        for (Map.Entry<String, Double> entry : attendanceAverages.entrySet()) {
            summary.append("Students with grade ").append(entry.getKey())
                    .append(" had an average attendance of ")
                    .append(String.format("%.3f", entry.getValue())).append("%.\n");
        }
        return summary.toString();
    }

    private void showSummary() {
        if (attendanceAverages == null || attendanceAverages.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please analyze the data first.");
            return;
        }
        JOptionPane.showMessageDialog(this, getSummary());
    }

    private void filterMissingAttendance() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to filter. Please load a CSV file first.");
            return;
        }

        int attendanceIndex = -1;
        int studentIdIndex = -1;
        int gradeIndex = -1;

        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            String columnName = tableModel.getColumnName(i);
            if (columnName.equalsIgnoreCase("Attendance (%)")) {
                attendanceIndex = i;
            } else if (columnName.equalsIgnoreCase("Student ID")) {
                studentIdIndex = i;
            } else if (columnName.equalsIgnoreCase("Grade")) {
                gradeIndex = i;
            }
        }

        if (attendanceIndex == -1 || studentIdIndex == -1 || gradeIndex == -1) {
            JOptionPane.showMessageDialog(this, "Required columns (Attendance (%), Student ID, Grade) not found.");
            return;
        }

        DefaultTableModel missingDataModel = new DefaultTableModel(new String[]{"Student ID", "Grade"}, 0);

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object attendanceValueObj = tableModel.getValueAt(i, attendanceIndex);
            String attendanceValue = attendanceValueObj != null ? attendanceValueObj.toString().trim() : "";

            try {
                if (attendanceValue.isEmpty() || Double.parseDouble(attendanceValue) < 0) {
                    Object studentId = tableModel.getValueAt(i, studentIdIndex);
                    Object grade = tableModel.getValueAt(i, gradeIndex);
                    missingDataModel.addRow(new Object[]{studentId, grade});
                }
            } catch (NumberFormatException e) {
                Object studentId = tableModel.getValueAt(i, studentIdIndex);
                Object grade = tableModel.getValueAt(i, gradeIndex);
                missingDataModel.addRow(new Object[]{studentId, grade});
            }
        }

        if (missingDataModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No missing attendance data found.");
            return;
        }

        JTable missingDataTable = new JTable(missingDataModel);
        DefaultTableCellRenderer centerRenderer2 = new DefaultTableCellRenderer();
        centerRenderer2.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < missingDataTable.getColumnCount(); i++) {
            missingDataTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer2);
        }

        JScrollPane scrollPane2 = new JScrollPane(missingDataTable);
        scrollPane2.setPreferredSize(new Dimension(400, 200));
        JOptionPane.showMessageDialog(this, scrollPane2, "Missing Attendance Data", JOptionPane.INFORMATION_MESSAGE);
    }

    private void animateChart() {
        if (attendanceAverages == null || attendanceAverages.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please analyze the data first.");
            return;
        }
    
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (String grade : attendanceAverages.keySet()) {
            dataset.addValue(0, "Attendance", grade); // Start with 0 for animation
        }
    
        JFreeChart chart = ChartFactory.createBarChart(
                "Average Attendance (%) by Grade",
                "Grade",
                "Average Attendance (%)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );
    
        CategoryPlot plot = chart.getCategoryPlot();
    
        // Custom renderer to assign colors dynamically based on the grade
        BarRenderer renderer = new BarRenderer() {
            private final Map<String, Color> gradeColors = new HashMap<String, Color>() {{
                put("A", Color.BLUE);
                put("B", Color.GREEN);
                put("C", Color.YELLOW);
                put("D", Color.RED);
                put("F", Color.MAGENTA);
            }};
    
            @Override
            public Paint getItemPaint(int row, int column) {
                String grade = (String) dataset.getColumnKey(column);
                return gradeColors.getOrDefault(grade, Color.GRAY); // Default to gray if grade not found
            }
        };
    
        plot.setRenderer(renderer);
        plot.getRangeAxis().setRange(0.0, 100.0);
    
        ChartPanel newChartPanel = new ChartPanel(chart);
        chartPanel.removeAll();
        chartPanel.setLayout(new BorderLayout());
        chartPanel.add(newChartPanel, BorderLayout.CENTER);
        chartPanel.validate();
    
        // Use a Timer to animate the bars
        Timer timer = new Timer(50, null);
        final Map<String, Double> currentValues = new HashMap<>();
        for (String grade : attendanceAverages.keySet()) {
            currentValues.put(grade, 0.0); // Start with 0
        }
    
        timer.addActionListener(e -> {
            boolean done = true;
            for (String grade : attendanceAverages.keySet()) {
                double targetValue = attendanceAverages.get(grade);
                double currentValue = currentValues.get(grade);
                if (currentValue < targetValue) {
                    currentValue = Math.min(currentValue + 1, targetValue); // Increment by 1
                    currentValues.put(grade, currentValue);
                    dataset.setValue(currentValue, "Attendance", grade);
                    done = false;
                }
            }
            if (done) {
                timer.stop(); // Stop the timer when all bars reach their target values
            }
        });
    
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MP2MP3 mp2 = new MP2MP3();
            mp2.setVisible(true);
        });
    }

    static class GradeData {
        double sum;
        int count;
    }
}