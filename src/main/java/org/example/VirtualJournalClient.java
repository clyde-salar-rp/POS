package org.example;

import lombok.Getter;
import org.example.config.VJConfig;
import org.example.model.Product;
import org.example.model.Transaction;
import org.example.service.DiscountService;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Virtual Journal Client - Connects to VJ Server via sockets
 * Now sends PRE-FORMATTED log messages for universal compatibility
 */
public class VirtualJournalClient {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Socket socket;
    private PrintWriter writer;
    @Getter
    private final VJConfig config;
    private final ReceiptPrinter receiptPrinter;
    private boolean connected = false;

    public VirtualJournalClient(ReceiptPrinter receiptPrinter) {
        this.config = new VJConfig();
        this.receiptPrinter = receiptPrinter;
        config.displayConfig();
    }

    public void connect() {
        int maxAttempts = config.isRetryEnabled() ? config.getMaxRetryAttempts() : 1;
        int delaySeconds = config.getRetryDelaySeconds();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                System.out.println("Connecting to VJ Server at " +
                        config.getServerHost() + ":" + config.getServerPort() +
                        " (Attempt " + attempt + "/" + maxAttempts + ")");

                socket = new Socket(config.getServerHost(), config.getServerPort());
                writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()),
                        true);
                connected = true;

                System.out.println("✓ Successfully connected to Virtual Journal Server");
                logSystem("Register connected to Virtual Journal Server");
                return;

            } catch (IOException e) {
                System.err.println("✗ Connection attempt " + attempt + " failed: " + e.getMessage());

                if (attempt < maxAttempts) {
                    System.out.println("Retrying in " + delaySeconds + " seconds...");
                    try {
                        Thread.sleep(delaySeconds * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        System.err.println("Failed to connect to VJ Server after " + maxAttempts + " attempts");
        System.err.println("Register will continue WITHOUT virtual journal logging");
        connected = false;
    }

    public void disconnect() {
        try {
            if (writer != null) {
                logSystem("Register disconnecting from Virtual Journal Server");
                writer.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            connected = false;
            System.out.println("Disconnected from Virtual Journal Server");
        } catch (IOException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
        }
    }

    /**
     * Format log entry with timestamp - ALL formatting happens here
     */
    private String formatLogEntry(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        return String.format("[%s] %s", timestamp, message);
    }

    /**
     * Send pre-formatted message to VJ Server
     */
    private void send(String formattedMessage) {
        // Also log to local console
        System.out.println(formattedMessage);

        if (!connected || writer == null) {
            return;
        }

        try {
            writer.println(formattedMessage);

            if (writer.checkError()) {
                System.err.println("Connection lost to VJ Server");
                connected = false;

                if (config.isRetryEnabled()) {
                    reconnect();
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending to VJ: " + e.getMessage());
            connected = false;
        }
    }

    private void reconnect() {
        System.out.println("Attempting to reconnect to VJ Server...");
        disconnect();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        connect();
    }

    // ========== LOG METHODS - Pre-formatted for universal compatibility ==========

    public void logScan(String source, String upc, Product product) {
        String logEntry;
        if (product != null) {
            logEntry = formatLogEntry(
                    String.format("%s | UPC: %s | Desc: %s | Price: $%.2f",
                            source, upc, product.getDescription(), product.getPrice())
            );
        } else {
            logEntry = formatLogEntry(
                    String.format("%s | UPC: %s | NOT FOUND", source, upc)
            );
        }
        send(logEntry);
    }

    public void logQuickKey(String description, double price) {
        String logEntry = formatLogEntry(
                String.format("QUICK_KEY | Desc: %s | Price: $%.2f", description, price)
        );
        send(logEntry);
    }

    public void logVoidItem(Product product) {
        String logEntry = formatLogEntry(
                String.format("VOID_ITEM | UPC: %s | Qty: %d | Desc: %s | Total: $%.2f",
                        product.getUpc(), product.getQuantity(),
                        product.getDescription(), product.getLineTotal())
        );
        send(logEntry);
    }

    public void logQuantityChange(Product product, int oldQty, int newQty) {
        String logEntry = formatLogEntry(
                String.format("QTY_CHANGE | UPC: %s | Desc: %s | Old Qty: %d | New Qty: %d",
                        product.getUpc(), product.getDescription(), oldQty, newQty)
        );
        send(logEntry);
    }

    public void logTender(String paymentType, double subtotal, double tax,
                          double total, double tendered, double change) {
        String logEntry = formatLogEntry(
                String.format("TENDER | Type: %s | Subtotal: $%.2f | Tax: $%.2f | Total: $%.2f | Tendered: $%.2f | Change: $%.2f",
                        paymentType, subtotal, tax, total, tendered, change)
        );
        send(logEntry);
    }

    public void logTransaction(String event, double total) {
        String logEntry = formatLogEntry(
                String.format("TRANSACTION | %s | Total: $%.2f", event, total)
        );
        send(logEntry);
    }

    public void logSystem(String message) {
        String logEntry = formatLogEntry(
                String.format("SYSTEM | %s", message)
        );
        send(logEntry);
    }

    // ========== RECEIPT METHODS ==========

    public String printReceipt(Transaction transaction, String paymentType,
                               double tendered, double change) {
        return printReceipt(transaction, paymentType, tendered, change, null);
    }

    public String printReceipt(Transaction transaction, String paymentType,
                               double tendered, double change,
                               DiscountService.DiscountResponse discountInfo) {

        // Log receipt printing event
        send(formatLogEntry("RECEIPT PRINTING"));

        // Generate receipt
        String receipt = receiptPrinter.generateReceipt(
                transaction, paymentType, tendered, change, discountInfo);

        // Send receipt to VJ server (and console)
        System.out.println("\n" + "=".repeat(50));
        System.out.println("RECEIPT OUTPUT:");
        System.out.println("=".repeat(50));
        System.out.println(receipt);
        System.out.println("=".repeat(50));
        System.out.println("END OF RECEIPT");
        System.out.println("=".repeat(50) + "\n");

        // Send formatted receipt to VJ server
        if (connected && writer != null) {
            writer.println("\n" + "=".repeat(50));
            writer.println("RECEIPT OUTPUT:");
            writer.println("=".repeat(50));
            writer.println(receipt);
            writer.println("=".repeat(50));
            writer.println("END OF RECEIPT");
            writer.println("=".repeat(50) + "\n");
        }

        return receipt;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
}