package org.example;

import org.example.model.Product;
import org.example.model.Transaction;
import org.example.service.DiscountService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VirtualJournal {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private final ReceiptPrinter receiptPrinter;

    public VirtualJournal() {
        this.receiptPrinter = new ReceiptPrinter();
    }

    public void logScan(String source, String upc, Product product) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);

        if (product != null) {
            System.out.printf("[%s] %s | UPC: %s | Desc: %s | Price: $%.2f%n",
                    timestamp, source, upc, product.getDescription(), product.getPrice());
        } else {
            System.out.printf("[%s] %s | UPC: %s | NOT FOUND%n",
                    timestamp, source, upc);
        }
    }

    public void logQuickKey(String description, double price) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] QUICK_KEY | Desc: %s | Price: $%.2f%n",
                timestamp, description, price);
    }

    public void logVoidItem(Product product) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] VOID_ITEM | UPC: %s | Qty: %d | Desc: %s | Price: $%.2f%n",
                timestamp, product.getUpc(), product.getQuantity(),
                product.getDescription(), product.getLineTotal());
    }

    public void logQuantityChange(Product product, int oldQty, int newQty) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] QTY_CHANGE | UPC: %s | Desc: %s | Old Qty: %d | New Qty: %d%n",
                timestamp, product.getUpc(),
                product.getDescription(), oldQty, newQty);
    }

    public void logTender(String paymentType, double subtotal, double tax, double total,
                          double tendered, double change) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] TENDER | Type: %s | Subtotal: $%.2f | Tax: $%.2f | Total: $%.2f | Tendered: $%.2f | Change: $%.2f%n",
                timestamp, paymentType, subtotal, tax, total, tendered, change);
    }

    public void logTransaction(String event, double total) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] TRANSACTION | %s | Total: $%.2f%n",
                timestamp, event, total);
    }

    public void logSystem(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] SYSTEM | %s%n", timestamp, message);
    }

    // Original method for backward compatibility
    public String printReceipt(Transaction transaction, String paymentType,
                               double tendered, double change) {
        return printReceipt(transaction, paymentType, tendered, change, null);
    }

    // New method with discount support
    public String printReceipt(Transaction transaction, String paymentType,
                               double tendered, double change,
                               DiscountService.DiscountResponse discountInfo) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] RECEIPT PRINTING%n", timestamp);
        System.out.println("\n" + "=".repeat(50));
        System.out.println("RECEIPT OUTPUT:");
        System.out.println("=".repeat(50));

        String receipt = receiptPrinter.generateReceipt(
                transaction, paymentType, tendered, change, discountInfo);
        System.out.println(receipt);

        System.out.println("=".repeat(50));
        System.out.println("END OF RECEIPT");
        System.out.println("=".repeat(50) + "\n");

        return receipt;
    }
}