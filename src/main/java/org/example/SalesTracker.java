package org.example;

import org.example.model.Product;
import org.example.model.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SalesTracker {
    private final List<CompletedSale> sales;
    private final Map<String, ProductSales> productSales;
    private final Map<String, Integer> paymentTypeTotals;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public SalesTracker() {
        this.sales = new ArrayList<>();
        this.productSales = new HashMap<>();
        this.paymentTypeTotals = new HashMap<>();
        paymentTypeTotals.put("CASH", 0);
        paymentTypeTotals.put("CREDIT", 0);
    }

    public void recordSale(Transaction transaction, String paymentType,
                           double tendered, double change) {
        CompletedSale sale = new CompletedSale(
                LocalDateTime.now(),
                transaction.getItems(),
                transaction.getSubtotal(),
                transaction.getTax(),
                transaction.getTotal(),
                paymentType,
                tendered,
                change
        );

        sales.add(sale);

        // Update payment type counter
        paymentTypeTotals.put(paymentType,
                paymentTypeTotals.getOrDefault(paymentType, 0) + 1);

        // Update product sales tracking
        for (Product product : transaction.getItems()) {
            String upc = product.getUpc();
            ProductSales ps = productSales.computeIfAbsent(upc,
                    k -> new ProductSales(product.getDescription()));

            ps.quantitySold += product.getQuantity();
            ps.totalRevenue += product.getLineTotal();
            ps.timesSold++;
        }
    }

    public String generateDailySalesReport() {
        if (sales.isEmpty()) {
            return "No sales recorded today.";
        }

        StringBuilder report = new StringBuilder();
        LocalDate today = LocalDate.now();

        // Header
        report.append("=".repeat(70)).append("\n");
        report.append(centerText("DAILY SALES REPORT")).append("\n");
        report.append(centerText("CLYDE'S STORE")).append("\n");
        report.append(centerText(today.format(DATE_FORMAT))).append("\n");
        report.append("=".repeat(70)).append("\n\n");

        // Summary Statistics
        int totalTransactions = sales.size();
        double totalGrossSales = sales.stream()
                .mapToDouble(s -> s.total).sum();
        double totalSubtotal = sales.stream()
                .mapToDouble(s -> s.subtotal).sum();
        double totalTax = sales.stream()
                .mapToDouble(s -> s.tax).sum();
        int totalItemsSold = sales.stream()
                .mapToInt(s -> s.items.size()).sum();

        report.append("SUMMARY\n");
        report.append("-".repeat(70)).append("\n");
        report.append(String.format("Total Transactions:        %,d\n", totalTransactions));
        report.append(String.format("Total Items Sold:          %,d\n", totalItemsSold));
        report.append(String.format("Gross Sales:               $%,.2f\n", totalGrossSales));
        report.append(String.format("Subtotal:                  $%,.2f\n", totalSubtotal));
        report.append(String.format("Tax Collected:             $%,.2f\n", totalTax));

        // Average transaction
        double avgTransaction = totalGrossSales / totalTransactions;
        report.append(String.format("Average Transaction:       $%,.2f\n", avgTransaction));
        report.append("\n");

        // Payment Method Breakdown
        report.append("PAYMENT METHODS\n");
        report.append("-".repeat(70)).append("\n");

        int cashCount = paymentTypeTotals.getOrDefault("CASH", 0);
        int creditCount = paymentTypeTotals.getOrDefault("CREDIT", 0);

        double cashTotal = sales.stream()
                .filter(s -> "CASH".equals(s.paymentType))
                .mapToDouble(s -> s.total).sum();

        double creditTotal = sales.stream()
                .filter(s -> "CREDIT".equals(s.paymentType))
                .mapToDouble(s -> s.total).sum();

        report.append(String.format("Cash:     %4d transactions    $%,.2f    (%.1f%%)\n",
                cashCount, cashTotal, (cashCount * 100.0 / totalTransactions)));
        report.append(String.format("Credit:   %4d transactions    $%,.2f    (%.1f%%)\n",
                creditCount, creditTotal, (creditCount * 100.0 / totalTransactions)));
        report.append("\n");

        // Top Selling Products
        report.append("TOP 10 SELLING PRODUCTS\n");
        report.append("-".repeat(70)).append("\n");
        report.append(String.format("%-40s %8s %10s %10s\n",
                "DESCRIPTION", "QTY SOLD", "REVENUE", "TIMES"));
        report.append("-".repeat(70)).append("\n");

        List<Map.Entry<String, ProductSales>> sortedProducts =
                new ArrayList<>(productSales.entrySet());
        sortedProducts.sort((a, b) ->
                Double.compare(b.getValue().totalRevenue, a.getValue().totalRevenue));

        int displayCount = Math.min(10, sortedProducts.size());
        for (int i = 0; i < displayCount; i++) {
            Map.Entry<String, ProductSales> entry = sortedProducts.get(i);
            ProductSales ps = entry.getValue();
            String desc = truncate(ps.description);
            report.append(String.format("%-40s %8d $%9.2f %10d\n",
                    desc, ps.quantitySold, ps.totalRevenue, ps.timesSold));
        }
        report.append("\n");

        // Hourly Sales Breakdown
        report.append("HOURLY SALES\n");
        report.append("-".repeat(70)).append("\n");

        Map<Integer, HourlySales> hourlySales = new HashMap<>();
        for (CompletedSale sale : sales) {
            int hour = sale.timestamp.getHour();
            HourlySales hs = hourlySales.computeIfAbsent(hour,
                    k -> new HourlySales());
            hs.transactionCount++;
            hs.totalSales += sale.total;
        }

        List<Integer> hours = new ArrayList<>(hourlySales.keySet());
        Collections.sort(hours);

        report.append(String.format("%-10s %15s %15s %15s\n",
                "HOUR", "TRANSACTIONS", "SALES", "AVG/TRANS"));
        report.append("-".repeat(70)).append("\n");

        for (Integer hour : hours) {
            HourlySales hs = hourlySales.get(hour);
            double avg = hs.totalSales / hs.transactionCount;
            report.append(String.format("%02d:00      %15d $%14.2f $%14.2f\n",
                    hour, hs.transactionCount, hs.totalSales, avg));
        }
        report.append("\n");

        // Recent Transactions
        report.append("RECENT TRANSACTIONS (Last 5)\n");
        report.append("-".repeat(70)).append("\n");
        report.append(String.format("%-10s %-8s %12s %8s %15s\n",
                "TIME", "PAYMENT", "ITEMS", "TOTAL", "CHANGE"));
        report.append("-".repeat(70)).append("\n");

        int startIdx = Math.max(0, sales.size() - 5);
        for (int i = startIdx; i < sales.size(); i++) {
            CompletedSale sale = sales.get(i);
            report.append(String.format("%-10s %-8s %12d $%7.2f $%14.2f\n",
                    sale.timestamp.format(TIME_FORMAT),
                    sale.paymentType,
                    sale.items.size(),
                    sale.total,
                    sale.change));
        }

        report.append("\n");
        report.append("=".repeat(70)).append("\n");
        report.append(centerText("END OF REPORT")).append("\n");
        report.append("=".repeat(70)).append("\n");

        return report.toString();
    }

    private String centerText(String text) {
        int padding = (70 - text.length()) / 2;
        if (padding < 0) padding = 0;
        return " ".repeat(padding) + text;
    }

    private String truncate(String text) {
        if (text.length() <= 40) {
            return text;
        }
        return text.substring(0, 40 - 3) + "...";
    }

    // Inner classes
    private static class CompletedSale {
        final LocalDateTime timestamp;
        final List<Product> items;
        final double subtotal;
        final double tax;
        final double total;
        final String paymentType;
        final double tendered;
        final double change;

        CompletedSale(LocalDateTime timestamp, List<Product> items,
                      double subtotal, double tax, double total,
                      String paymentType, double tendered, double change) {
            this.timestamp = timestamp;
            this.items = new ArrayList<>(items);
            this.subtotal = subtotal;
            this.tax = tax;
            this.total = total;
            this.paymentType = paymentType;
            this.tendered = tendered;
            this.change = change;
        }
    }

    private static class ProductSales {
        final String description;
        int quantitySold;
        double totalRevenue;
        int timesSold;

        ProductSales(String description) {
            this.description = description;
            this.quantitySold = 0;
            this.totalRevenue = 0.0;
            this.timesSold = 0;
        }
    }

    private static class HourlySales {
        int transactionCount;
        double totalSales;

        HourlySales() {
            this.transactionCount = 0;
            this.totalSales = 0.0;
        }
    }

    public int getTotalTransactions() {
        return sales.size();
    }

    public void resetDailySales() {
        sales.clear();
        productSales.clear();
        paymentTypeTotals.clear();
        paymentTypeTotals.put("CASH", 0);
        paymentTypeTotals.put("CREDIT", 0);
    }
}