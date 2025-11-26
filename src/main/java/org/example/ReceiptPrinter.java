package org.example;

import org.example.model.Product;
import org.example.model.Transaction;
import org.example.service.DiscountService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptPrinter {
    private static final int RECEIPT_WIDTH = 42;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private int receiptNumber = 1;

    public String generateReceipt(Transaction transaction, String paymentType,
                                  double tendered, double change) {
        return generateReceipt(transaction, paymentType, tendered, change, null);
    }

    public String generateReceipt(Transaction transaction, String paymentType,
                                  double tendered, double change,
                                  DiscountService.DiscountResponse discountInfo) {
        StringBuilder receipt = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();

        // Header
        receipt.append(centerText("CLYDE'S STORE")).append("\n");
        receipt.append(centerText("123 Main Street")).append("\n");
        receipt.append(centerText("Town, ST 12345")).append("\n");
        receipt.append(centerText("Tel: (555) 123-4567")).append("\n");
        receipt.append(line()).append("\n");

        // Transaction info
        receipt.append(String.format("Date: %-15s Receipt #: %04d\n",
                now.format(DATE_FORMAT), receiptNumber++));
        receipt.append(String.format("Time: %s\n", now.format(TIME_FORMAT)));
        receipt.append(String.format("Cashier: %s\n", "OPERATOR01"));
        receipt.append(String.format("Register: %s\n", "REG-001"));
        receipt.append(line()).append("\n");

        // Items header
        receipt.append(String.format("%-3s %-22s %7s %7s\n",
                "QTY", "DESCRIPTION", "PRICE", "TOTAL"));
        receipt.append(line()).append("\n");

        // Items
        List<Product> items = transaction.getItems();
        for (Product product : items) {
            // Description line
            String desc = truncate(product.getDescription(), 22);
            receipt.append(String.format("%-3d %-22s %7.2f %7.2f\n",
                    product.getQuantity(),
                    desc,
                    product.getPrice(),
                    product.getLineTotal()));

            // UPC line
            receipt.append(String.format("    UPC: %s\n", product.getUpc()));
        }

        receipt.append(line()).append("\n");

        // Calculate values
        double originalSubtotal;
        double finalSubtotal;
        double totalDiscount = 0.0;
        double tax;
        double total;

        if (discountInfo != null && discountInfo.totalDiscount > 0) {
            // With discounts
            originalSubtotal = discountInfo.subtotal;
            totalDiscount = discountInfo.totalDiscount;
            finalSubtotal = originalSubtotal - totalDiscount;
            tax = discountInfo.tax;
            total = discountInfo.total;
        } else {
            // No discounts - use transaction values
            originalSubtotal = transaction.getSubtotal();
            finalSubtotal = originalSubtotal;
            tax = transaction.getTax();
            total = transaction.getTotal();
        }

        // Original subtotal
        receipt.append(String.format("%26s %15.2f\n", "SUBTOTAL:", originalSubtotal));

        // Show discounts if any
        if (discountInfo != null && discountInfo.appliedDiscounts != null
                && !discountInfo.appliedDiscounts.isEmpty()) {

            receipt.append("\n");
            receipt.append(centerText("*** DISCOUNTS APPLIED ***")).append("\n");
            receipt.append(line()).append("\n");

            // Each discount
            for (DiscountService.DiscountResponse.AppliedDiscount discount :
                    discountInfo.appliedDiscounts) {

                String discDesc = truncate(discount.description, 26);
                receipt.append(String.format("%26s -%14.2f\n",
                        discDesc + ":", discount.amount));

                // Show affected items (optional - can remove if too verbose)
                if (discount.affectedItems != null && !discount.affectedItems.isEmpty()) {
                    for (String item : discount.affectedItems) {
                        String itemDesc = truncate(item, 38);
                        receipt.append(String.format("  * %s\n", itemDesc));
                    }
                }
            }

            receipt.append(line()).append("\n");
            receipt.append(String.format("%26s -%14.2f\n",
                    "TOTAL SAVINGS:", totalDiscount));
            receipt.append(line()).append("\n");

            // Discounted subtotal
            receipt.append(String.format("%26s %15.2f\n",
                    "SUBTOTAL AFTER DISC:", finalSubtotal));
        }

        // Tax (calculated on discounted amount)
        receipt.append(String.format("%26s %15.2f\n", "TAX (7%):", tax));
        receipt.append(doubleLine()).append("\n");

        // Final total
        receipt.append(String.format("%26s %15.2f\n", "TOTAL:", total));
        receipt.append(doubleLine()).append("\n");

        // Savings callout (if significant)
        if (totalDiscount > 0) {
            receipt.append("\n");
            receipt.append(centerText(String.format("YOU SAVED $%.2f TODAY!", totalDiscount)))
                    .append("\n");
            receipt.append(line()).append("\n");
        }

        // Payment
        receipt.append(String.format("%26s %15s\n", "PAYMENT TYPE:", paymentType));

        if (paymentType.equals("CASH")) {
            receipt.append(String.format("%26s %15.2f\n", "CASH TENDERED:", tendered));
            if (change > 0) {
                receipt.append(String.format("%26s %15.2f\n", "CHANGE DUE:", change));
            }
        } else {
            receipt.append(String.format("%26s %15.2f\n", "AMOUNT CHARGED:", total));
        }

        receipt.append(line()).append("\n");

        // Item count
        receipt.append(String.format("Total Items: %d\n", items.size()));
        receipt.append(line()).append("\n");

        // Footer
        receipt.append(centerText("THANK YOU FOR YOUR PURCHASE!")).append("\n");
        receipt.append(centerText("Please come again")).append("\n");
        receipt.append(line()).append("\n");

        if (paymentType.equals("CREDIT")) {
            receipt.append(centerText("** CUSTOMER COPY **")).append("\n");
            receipt.append(line()).append("\n");
        }

        receipt.append(centerText("Tax ID: 12-3456789")).append("\n");
        receipt.append(centerText("www.clyde.com")).append("\n");

        return receipt.toString();
    }

    private String centerText(String text) {
        int padding = (RECEIPT_WIDTH - text.length()) / 2;
        if (padding < 0) padding = 0;
        return " ".repeat(padding) + text;
    }

    private String line() {
        return "-".repeat(RECEIPT_WIDTH);
    }

    private String doubleLine() {
        return "=".repeat(RECEIPT_WIDTH);
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public void resetReceiptNumber() {
        receiptNumber = 1;
    }
}