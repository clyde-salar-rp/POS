package org.example.ui;

import org.example.TransactionDatabase;
import org.example.TransactionDatabase.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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

        JButton exportButton = new JButton("Export to CSV");
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
        sb.append(String.format("%-20s %15s %19.2f%%\n", "", "", creditPercent));

        sb.append("=".repeat(70)).append("\n");
        sb.append(String.format("%-20s %15d $%,19.2f\n",
                "TOTAL", report.totalCount(), report.totalSales()));
        sb.append("=".repeat(70)).append("\n");

        reportArea.setText(sb.toString());
    }

    private void generateWeeklySummary(LocalDate endDate) throws SQLException {
        LocalDate startDate = endDate.minusDays(6);
        StringBuilder sb = new StringBuilder();

        sb.append("=".repeat(70)).append("\n");
        sb.append(centerText("WEEKLY SALES SUMMARY", 70)).append("\n");
        sb.append(centerText(startDate.format(DATE_FORMAT) + " - " + endDate.format(DATE_FORMAT), 70)).append("\n");
        sb.append("=".repeat(70)).append("\n\n");

        sb.append(String.format("%-12s %12s %15s %15s\n",
                "DATE", "TRANSACTIONS", "REVENUE", "AVG TRANS"));
        sb.append("-".repeat(70)).append("\n");

        double weekTotal = 0;
        int weekTransactions = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            DailySalesReport dayReport = database.getDailySalesReport(date.atStartOfDay());

            sb.append(String.format("%-12s %12d $%,14.2f $%,14.2f\n",
                    date.format(DateTimeFormatter.ofPattern("MM/dd")),
                    dayReport.transactionCount(),
                    dayReport.totalSales(),
                    dayReport.avgTransaction()));

            weekTotal += dayReport.totalSales();
            weekTransactions += dayReport.transactionCount();
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
        fileChooser.setDialogTitle("Export Report");
        fileChooser.setSelectedFile(new java.io.File("report.txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                java.nio.file.Files.writeString(file.toPath(), reportArea.getText());
                JOptionPane.showMessageDialog(this,
                        "Report exported successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting report: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }
}