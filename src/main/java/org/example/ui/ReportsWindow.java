package org.example.ui;

import org.example.TransactionDatabase;
import org.example.TransactionDatabase.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ReportsWindow extends JFrame {
    private final TransactionDatabase database;
    private final JTextArea reportArea;
    private final JComboBox<String> reportTypeCombo;
    private final JSpinner dateSpinner;
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // Store current report data for CSV export
    private LocalDate currentReportDate;
    private String currentReportType;

    public ReportsWindow(TransactionDatabase database) {
        this.database = database;

        setTitle("Sales Reports");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        // Top control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        controlPanel.setBackground(Color.WHITE);

        JLabel typeLabel = new JLabel("Report Type:");
        typeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        reportTypeCombo = new JComboBox<>(new String[]{
                "Daily Sales Summary",
                "Category Sales",
                "Top Selling Items",
                "Payment Methods",
                "Weekly Summary"
        });
        reportTypeCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));

        JLabel dateLabel = new JLabel("Date:");
        dateLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        SpinnerDateModel dateModel = new SpinnerDateModel();
        dateSpinner = new JSpinner(dateModel);
        JSpinner.DateEditor dateEditor = new JSpinner.DateEditor(dateSpinner, "MM/dd/yyyy");
        dateSpinner.setEditor(dateEditor);
        dateSpinner.setPreferredSize(new Dimension(120, 30));

        JButton generateButton = new JButton("Generate Report");
        styleButton(generateButton, PRIMARY_COLOR);
        generateButton.addActionListener(e -> generateReport());

        JButton exportButton = new JButton("Export CSV");
        styleButton(exportButton, new Color(76, 175, 80));
        exportButton.addActionListener(e -> exportReport());

        controlPanel.add(typeLabel);
        controlPanel.add(reportTypeCombo);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(dateLabel);
        controlPanel.add(dateSpinner);
        controlPanel.add(Box.createHorizontalStrut(20));
        controlPanel.add(generateButton);
        controlPanel.add(exportButton);

        // Report display area
        reportArea = new JTextArea();
        reportArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        reportArea.setEditable(false);
        reportArea.setMargin(new Insets(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(reportArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
        setVisible(true);

        // Generate today's report by default
        generateReport();
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 35));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    private void generateReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        java.util.Date spinnerDate = (java.util.Date) dateSpinner.getValue();
        LocalDate selectedDate = spinnerDate.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        // Store current report info for export
        currentReportType = reportType;
        currentReportDate = selectedDate;

        try {
            switch (reportType) {
                case "Daily Sales Summary" -> generateDailySalesReport(selectedDate);
                case "Category Sales" -> generateCategoryReport(selectedDate);
                case "Top Selling Items" -> generateTopSellingReport(selectedDate);
                case "Payment Methods" -> generatePaymentMethodReport(selectedDate);
                case "Weekly Summary" -> generateWeeklySummary(selectedDate);
            }
        } catch (SQLException e) {
            reportArea.setText("Error generating report:\n\n" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateDailySalesReport(LocalDate date) throws SQLException {
        LocalDateTime startOfDay = date.atStartOfDay();
        DailySalesReport report = database.getDailySalesReport(startOfDay);

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(70)).append("\n");
        sb.append(centerText("DAILY SALES REPORT", 70)).append("\n");
        sb.append(centerText(date.format(DATE_FORMAT), 70)).append("\n");
        sb.append("=".repeat(70)).append("\n\n");

        sb.append(String.format("%-30s %10d\n", "Total Transactions:", report.transactionCount()));
        sb.append("-".repeat(70)).append("\n");
        sb.append(String.format("%-30s $%,10.2f\n", "Gross Sales:", report.totalSales() + report.totalDiscounts()));
        sb.append(String.format("%-30s -$%,9.2f\n", "Total Discounts:", report.totalDiscounts()));
        sb.append(String.format("%-30s $%,10.2f\n", "Net Sales (before tax):", report.totalSales() - report.totalTax()));
        sb.append(String.format("%-30s $%,10.2f\n", "Total Tax:", report.totalTax()));
        sb.append("=".repeat(70)).append("\n");
        sb.append(String.format("%-30s $%,10.2f\n", "TOTAL REVENUE:", report.totalSales()));
        sb.append("=".repeat(70)).append("\n\n");

        sb.append(String.format("%-30s $%,10.2f\n", "Average Transaction:", report.avgTransaction()));

        if (report.totalDiscounts() > 0) {
            double discountPercent = (report.totalDiscounts() / (report.totalSales() + report.totalDiscounts())) * 100;
            sb.append(String.format("%-30s %.2f%%\n", "Discount Rate:", discountPercent));
        }

        reportArea.setText(sb.toString());
    }

    private void generateCategoryReport(LocalDate date) throws SQLException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        java.util.List<CategorySalesReport> categories = database.getCategorySales(start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(80)).append("\n");
        sb.append(centerText("CATEGORY SALES REPORT", 80)).append("\n");
        sb.append(centerText(date.format(DATE_FORMAT), 80)).append("\n");
        sb.append("=".repeat(80)).append("\n\n");

        sb.append(String.format("%-15s %12s %12s %15s\n",
                "CATEGORY", "TRANSACTIONS", "QTY SOLD", "TOTAL SALES"));
        sb.append("-".repeat(80)).append("\n");

        double grandTotal = 0;
        int totalQty = 0;
        int totalTransactions = 0;

        for (CategorySalesReport cat : categories) {
            sb.append(String.format("%-15s %12d %12d $%,14.2f\n",
                    cat.category(),
                    cat.transactionCount(),
                    cat.totalQuantity(),
                    cat.totalSales()));

            grandTotal += cat.totalSales();
            totalQty += cat.totalQuantity();
            totalTransactions += cat.transactionCount();
        }

        sb.append("=".repeat(80)).append("\n");
        sb.append(String.format("%-15s %12d %12d $%,14.2f\n",
                "TOTAL", totalTransactions, totalQty, grandTotal));
        sb.append("=".repeat(80)).append("\n");

        reportArea.setText(sb.toString());
    }

    private void generateTopSellingReport(LocalDate date) throws SQLException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        java.util.List<TopSellingItem> items = database.getTopSellingItems(20, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(90)).append("\n");
        sb.append(centerText("TOP 20 SELLING ITEMS", 90)).append("\n");
        sb.append(centerText(date.format(DATE_FORMAT), 90)).append("\n");
        sb.append("=".repeat(90)).append("\n\n");

        sb.append(String.format("%-4s %-40s %10s %12s %12s\n",
                "RANK", "DESCRIPTION", "QTY SOLD", "# TRANS", "REVENUE"));
        sb.append("-".repeat(90)).append("\n");

        int rank = 1;
        for (TopSellingItem item : items) {
            String desc = item.description().length() > 40
                    ? item.description().substring(0, 37) + "..."
                    : item.description();

            sb.append(String.format("%-4d %-40s %10d %12d $%,11.2f\n",
                    rank++,
                    desc,
                    item.totalQuantity(),
                    item.transactionCount(),
                    item.totalSales()));
        }

        sb.append("=".repeat(90)).append("\n");

        reportArea.setText(sb.toString());
    }

    private void generatePaymentMethodReport(LocalDate date) throws SQLException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        PaymentMethodReport report = database.getPaymentMethodReport(start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(70)).append("\n");
        sb.append(centerText("PAYMENT METHOD BREAKDOWN", 70)).append("\n");
        sb.append(centerText(date.format(DATE_FORMAT), 70)).append("\n");
        sb.append("=".repeat(70)).append("\n\n");

        sb.append(String.format("%-20s %15s %20s\n", "METHOD", "TRANSACTIONS", "TOTAL AMOUNT"));
        sb.append("-".repeat(70)).append("\n");

        sb.append(String.format("%-20s %15d $%,19.2f\n",
                "CASH", report.cashCount(), report.cashTotal()));

        double cashPercent = report.totalSales() > 0
                ? (report.cashTotal() / report.totalSales()) * 100 : 0;
        sb.append(String.format("%-20s %15s %19.2f%%\n\n", "", "", cashPercent));

        sb.append(String.format("%-20s %15d $%,19.2f\n",
                "CREDIT", report.creditCount(), report.creditTotal()));

        double creditPercent = report.totalSales() > 0
                ? (report.creditTotal() / report.totalSales()) * 100 : 0;
        sb.append(String.format("%-20s %15s %19.2f%%\n\n", "", "", creditPercent));

        sb.append("=".repeat(70)).append("\n");
        sb.append(String.format("%-20s %15d $%,19.2f\n",
                "TOTAL", report.cashCount() + report.creditCount(), report.totalSales()));
        sb.append("=".repeat(70)).append("\n");

        reportArea.setText(sb.toString());
    }

    private void generateWeeklySummary(LocalDate date) throws SQLException {
        LocalDate startOfWeek = date.minusDays(6);

        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(70)).append("\n");
        sb.append(centerText("WEEKLY SALES SUMMARY", 70)).append("\n");
        sb.append(centerText(startOfWeek.format(DATE_FORMAT) + " - " + date.format(DATE_FORMAT), 70)).append("\n");
        sb.append("=".repeat(70)).append("\n\n");

        sb.append(String.format("%-12s %12s %15s %15s\n",
                "DATE", "TRANSACTIONS", "TOTAL SALES", "AVG TRANS"));
        sb.append("-".repeat(70)).append("\n");

        double weekTotal = 0;
        int weekTransactions = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startOfWeek.plusDays(i);
            LocalDateTime start = currentDate.atStartOfDay();
            DailySalesReport dayReport = database.getDailySalesReport(start);

            double dayTotal = dayReport.totalSales();
            int dayTransactions = dayReport.transactionCount();
            double dayAvg = dayTransactions > 0 ? dayTotal / dayTransactions : 0;

            weekTotal += dayTotal;
            weekTransactions += dayTransactions;

            sb.append(String.format("%-12s %12d $%,14.2f $%,14.2f\n",
                    currentDate.format(DATE_FORMAT),
                    dayTransactions,
                    dayTotal,
                    dayAvg));
        }

        sb.append("=".repeat(70)).append("\n");
        double weekAvg = weekTransactions > 0 ? weekTotal / weekTransactions : 0;
        sb.append(String.format("%-12s %12d $%,14.2f $%,14.2f\n",
                "TOTAL", weekTransactions, weekTotal, weekAvg));
        sb.append("=".repeat(70)).append("\n");

        reportArea.setText(sb.toString());
    }

    private void exportReport() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Report as CSV");

        // Set default filename based on report type and date
        String filename = String.format("%s_%s.csv",
                currentReportType.replace(" ", "_"),
                currentReportDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        fileChooser.setSelectedFile(new java.io.File(filename));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                // Ensure file has .csv extension
                if (!file.getName().toLowerCase().endsWith(".csv")) {
                    file = new java.io.File(file.getAbsolutePath() + ".csv");
                }

                exportReportAsCSV(file);

                JOptionPane.showMessageDialog(this,
                        "Report exported successfully as CSV!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting report: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void exportReportAsCSV(java.io.File file) throws SQLException, IOException {
        try (FileWriter writer = new FileWriter(file)) {
            switch (currentReportType) {
                case "Daily Sales Summary" -> exportDailySalesCSV(writer, currentReportDate);
                case "Category Sales" -> exportCategoryCSV(writer, currentReportDate);
                case "Top Selling Items" -> exportTopSellingCSV(writer, currentReportDate);
                case "Payment Methods" -> exportPaymentMethodCSV(writer, currentReportDate);
                case "Weekly Summary" -> exportWeeklySummaryCSV(writer, currentReportDate);
            }
        }
    }

    private void exportDailySalesCSV(FileWriter writer, LocalDate date) throws SQLException, IOException {
        LocalDateTime startOfDay = date.atStartOfDay();
        DailySalesReport report = database.getDailySalesReport(startOfDay);

        // Write header
        writer.write("Daily Sales Summary\n");
        writer.write("Date," + date.format(DATE_FORMAT) + "\n\n");

        // Write data
        writer.write("Metric,Value\n");
        writer.write(String.format("Total Transactions,%d\n", report.transactionCount()));
        writer.write(String.format("Gross Sales,%.2f\n", report.totalSales() + report.totalDiscounts()));
        writer.write(String.format("Total Discounts,%.2f\n", report.totalDiscounts()));
        writer.write(String.format("Net Sales (before tax),%.2f\n", report.totalSales() - report.totalTax()));
        writer.write(String.format("Total Tax,%.2f\n", report.totalTax()));
        writer.write(String.format("Total Revenue,%.2f\n", report.totalSales()));
        writer.write(String.format("Average Transaction,%.2f\n", report.avgTransaction()));

        if (report.totalDiscounts() > 0) {
            double discountPercent = (report.totalDiscounts() / (report.totalSales() + report.totalDiscounts())) * 100;
            writer.write(String.format("Discount Rate,%.2f%%\n", discountPercent));
        }
    }

    private void exportCategoryCSV(FileWriter writer, LocalDate date) throws SQLException, IOException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        java.util.List<CategorySalesReport> categories = database.getCategorySales(start, end);

        // Write header
        writer.write("Category Sales Report\n");
        writer.write("Date," + date.format(DATE_FORMAT) + "\n\n");
        writer.write("Category,Transactions,Quantity Sold,Total Sales\n");

        // Write data
        double grandTotal = 0;
        int totalQty = 0;
        int totalTransactions = 0;

        for (CategorySalesReport cat : categories) {
            writer.write(String.format("%s,%d,%d,%.2f\n",
                    escapeCsv(cat.category()),
                    cat.transactionCount(),
                    cat.totalQuantity(),
                    cat.totalSales()));

            grandTotal += cat.totalSales();
            totalQty += cat.totalQuantity();
            totalTransactions += cat.transactionCount();
        }

        // Write totals
        writer.write(String.format("\nTOTAL,%d,%d,%.2f\n",
                totalTransactions, totalQty, grandTotal));
    }

    private void exportTopSellingCSV(FileWriter writer, LocalDate date) throws SQLException, IOException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        java.util.List<TopSellingItem> items = database.getTopSellingItems(20, start, end);

        // Write header
        writer.write("Top 20 Selling Items\n");
        writer.write("Date," + date.format(DATE_FORMAT) + "\n\n");
        writer.write("Rank,Description,Quantity Sold,Transactions,Revenue\n");

        // Write data
        int rank = 1;
        for (TopSellingItem item : items) {
            writer.write(String.format("%d,\"%s\",%d,%d,%.2f\n",
                    rank++,
                    escapeCsv(item.description()),
                    item.totalQuantity(),
                    item.transactionCount(),
                    item.totalSales()));
        }
    }

    private void exportPaymentMethodCSV(FileWriter writer, LocalDate date) throws SQLException, IOException {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        PaymentMethodReport report = database.getPaymentMethodReport(start, end);

        // Write header
        writer.write("Payment Method Breakdown\n");
        writer.write("Date," + date.format(DATE_FORMAT) + "\n\n");
        writer.write("Payment Method,Transactions,Total Amount,Percentage\n");

        // Calculate percentages
        double cashPercent = report.totalSales() > 0
                ? (report.cashTotal() / report.totalSales()) * 100 : 0;
        double creditPercent = report.totalSales() > 0
                ? (report.creditTotal() / report.totalSales()) * 100 : 0;

        // Write data
        writer.write(String.format("CASH,%d,%.2f,%.2f%%\n",
                report.cashCount(), report.cashTotal(), cashPercent));
        writer.write(String.format("CREDIT,%d,%.2f,%.2f%%\n",
                report.creditCount(), report.creditTotal(), creditPercent));

        // Write totals
        writer.write(String.format("\nTOTAL,%d,%.2f,100.00%%\n",
                report.cashCount() + report.creditCount(), report.totalSales()));
    }

    private void exportWeeklySummaryCSV(FileWriter writer, LocalDate date) throws SQLException, IOException {
        LocalDate startOfWeek = date.minusDays(6);

        // Write header
        writer.write("Weekly Sales Summary\n");
        writer.write(String.format("Period,%s - %s\n\n",
                startOfWeek.format(DATE_FORMAT), date.format(DATE_FORMAT)));
        writer.write("Date,Transactions,Total Sales,Average Transaction\n");

        // Write daily data
        double weekTotal = 0;
        int weekTransactions = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startOfWeek.plusDays(i);
            LocalDateTime start = currentDate.atStartOfDay();
            DailySalesReport dayReport = database.getDailySalesReport(start);

            double dayTotal = dayReport.totalSales();
            int dayTransactions = dayReport.transactionCount();
            double dayAvg = dayTransactions > 0 ? dayTotal / dayTransactions : 0;

            weekTotal += dayTotal;
            weekTransactions += dayTransactions;

            writer.write(String.format("%s,%d,%.2f,%.2f\n",
                    currentDate.format(DATE_FORMAT),
                    dayTransactions,
                    dayTotal,
                    dayAvg));
        }

        // Write totals
        double weekAvg = weekTransactions > 0 ? weekTotal / weekTransactions : 0;
        writer.write(String.format("\nTOTAL,%d,%.2f,%.2f\n",
                weekTransactions, weekTotal, weekAvg));
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
}