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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.CategorySeries;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.style.Styler;

public class MP2 extends JFrame {

    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JPanel chartPanel;
    private Map<String, Double> attendanceAverages;

    public MP2() {
        setTitle("Grade vs. Attendance Analysis");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize table
        String[] columnNames = {"Student ID", "First Name", "Last Name", "Email", "Gender", "Age", "Department", "Attendance (%)", "Midterm Score", "Final Score", "Assignment Avg", "Quizzes Avg", "Participation Score", "Projects Score", "Total Score", "Grade", "Study Hours", "Extracurricular", "Internet Access", "Parent Education", "Family Income", "Stress Level", "Sleep Hours"};
        tableModel = new DefaultTableModel(columnNames, 0);
        dataTable = new JTable(tableModel);

        // Center align data in JTable
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
        JButton summaryButton = new JButton("Summary"); // Added Summary button
        JButton filterButton = new JButton("Filter Missing Attendance");

        buttonPanel.add(loadButton);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(summaryButton); // Added Summary button
        buttonPanel.add(filterButton); // Add the button to the panel
        add(buttonPanel, BorderLayout.NORTH);

        chartPanel = new JPanel();
        add(chartPanel, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> loadCSV());
        analyzeButton.addActionListener(e -> analyzeData());
        downloadButton.addActionListener(e -> downloadCSV());
        summaryButton.addActionListener(e -> showSummary()); // Added Summary button action
        filterButton.addActionListener(e -> filterMissingAttendance());
    }

    private void loadCSV() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
        fileChooser.setFileFilter(filter);
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(selectedFile))) {
                tableModel.setRowCount(0); // Clear existing data
                String line;
                boolean isHeader = true; // Flag to skip the header row
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false; // Skip the first line (header)
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
        attendanceAverages = processCSV();
        createChart(attendanceAverages);
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
        CategoryChart chart = new CategoryChartBuilder()
                .width(800)
                .height(400)
                .title("Average Attendance (%) by Grade")
                .xAxisTitle("GRADES")
                .yAxisTitle("AVERAGE ATTENDANCE (%)")
                .build();

        // Customize the chart's style
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart.getStyler().setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Bar);

        // Prepare data for the chart
        List<String> grades = new ArrayList<>(attendanceAverages.keySet());
        List<Double> averages = new ArrayList<>(attendanceAverages.values());

        // Add individual series for each grade with custom colors
        Color[] barColors = new Color[]{Color.BLUE, Color.GREEN, Color.ORANGE, Color.RED, Color.MAGENTA};
        for (int i = 0; i < grades.size(); i++) {
            String grade = grades.get(i);
            Double average = averages.get(i);

            // Add a separate series for each grade
            chart.addSeries(grade, List.of(grade), List.of(average));

            // Set the color for the series
            chart.getStyler().setSeriesColors(barColors);
        }

        // Set Y-axis range to 0-100
        chart.getStyler().setYAxisMax(100.0);
        chart.getStyler().setYAxisMin(0.0);

        // Create and display the chart panel
        XChartPanel<CategoryChart> newChartPanel = new XChartPanel<>(chart);
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
                JOptionPane.showMessageDialog(this, "CSV file saved successfully.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + e.getMessage());
            }
        }
    }

    private void showSummary() {
        if (attendanceAverages == null || attendanceAverages.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please analyze the data first.");
            return;
        }

        StringBuilder summary = new StringBuilder("Summary:\n");
        for (Map.Entry<String, Double> entry : attendanceAverages.entrySet()) {
            summary.append("Students with grade ").append(entry.getKey())
                    .append(" had an average attendance of ")
                    .append(String.format("%.3f", entry.getValue())).append("%.\n");
        }
        JOptionPane.showMessageDialog(this, summary.toString());
    }

    private void filterMissingAttendance() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to filter. Please load a CSV file first.");
            return;
        }
    
        // Find the indices of the "Attendance (%)", "Student ID", and "Grade" columns
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
    
        // Create a new table model for missing attendance data (only Student ID and Grade)
        DefaultTableModel missingDataModel = new DefaultTableModel(new String[]{"Student ID", "Grade"}, 0);
    
        // Collect rows with missing or invalid attendance data
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object attendanceValueObj = tableModel.getValueAt(i, attendanceIndex);
            String attendanceValue = attendanceValueObj != null ? attendanceValueObj.toString().trim() : "";
    
            try {
                if (attendanceValue.isEmpty() || Double.parseDouble(attendanceValue) < 0) {
                    // Add only Student ID and Grade to the missing data model
                    Object studentId = tableModel.getValueAt(i, studentIdIndex);
                    Object grade = tableModel.getValueAt(i, gradeIndex);
                    missingDataModel.addRow(new Object[]{studentId, grade});
                }
            } catch (NumberFormatException e) {
                // Add the row if attendance is invalid
                Object studentId = tableModel.getValueAt(i, studentIdIndex);
                Object grade = tableModel.getValueAt(i, gradeIndex);
                missingDataModel.addRow(new Object[]{studentId, grade});
            }
        }
    
        // Check if there is any missing data
        if (missingDataModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No missing attendance data found.");
            return;
        }
    
        // Create a JTable to display the missing data
        JTable missingDataTable = new JTable(missingDataModel);
    
        // Center-align the data in the JTable
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < missingDataTable.getColumnCount(); i++) {
            missingDataTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    
        JScrollPane scrollPane = new JScrollPane(missingDataTable);
        scrollPane.setPreferredSize(new Dimension(400, 200)); // Set the size of the pop-up
    
        // Show the table in a JOptionPane
        JOptionPane.showMessageDialog(this, scrollPane, "Missing Attendance Data", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MP2 mp2 = new MP2();
            mp2.setVisible(true);
        });
    }

    static class GradeData {
        double sum;
        int count;
    }
}