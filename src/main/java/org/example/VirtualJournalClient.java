package org.example;

import org.example.config.VJConfig;
import org.example.model.Product;
import org.example.model.Transaction;
import org.example.service.DiscountService;

import java.io.*;
import java.net.*;

/**
 * Virtual Journal Client - Connects to VJ Server via sockets
 */
public class VirtualJournalClient {
    private Socket socket;
    private PrintWriter writer;
    private final VJConfig config;
    private final ReceiptPrinter receiptPrinter;
    private boolean connected = false;
    private int connectionAttempts = 0;

    public VirtualJournalClient(ReceiptPrinter receiptPrinter) {
        this.config = new VJConfig();
        this.receiptPrinter = receiptPrinter;
        config.displayConfig();
    }

    public VirtualJournalClient(VJConfig config, ReceiptPrinter receiptPrinter) {
        this.config = config;
        this.receiptPrinter = receiptPrinter;
        config.displayConfig();
    }

    public boolean connect() {
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
                connectionAttempts = attempt;

                System.out.println("✓ Successfully connected to Virtual Journal Server");
                logSystem("Register connected to Virtual Journal Server");
                return true;

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
        return false;
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

    private void send(String message) {
        if (!connected || writer == null) {
            return; // Silently drop if not connected
        }

        try {
            writer.println(message);

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

    // ========== LOG METHODS ==========

    public void logScan(String source, String upc, Product product) {
        if (product != null) {
            send(String.format("SCAN|%s|%s|%s|%.2f|%s",
                    source, upc, product.getDescription(),
                    product.getPrice(), "FOUND"));
        } else {
            send(String.format("SCAN|%s|%s|||%s",
                    source, upc, "NOT_FOUND"));
        }
    }

    public void logQuickKey(String description, double price) {
        send(String.format("QUICK_KEY|%s|%.2f", description, price));
    }

    public void logVoidItem(Product product) {
        send(String.format("VOID_ITEM|%s|%d|%s|%.2f",
                product.getUpc(), product.getQuantity(),
                product.getDescription(), product.getLineTotal()));
    }

    public void logQuantityChange(Product product, int oldQty, int newQty) {
        send(String.format("QTY_CHANGE|%s|%s|%d|%d",
                product.getUpc(), product.getDescription(), oldQty, newQty));
    }

    public void logTender(String paymentType, double subtotal, double tax,
                          double total, double tendered, double change) {
        send(String.format("TENDER|%s|%.2f|%.2f|%.2f|%.2f|%.2f",
                paymentType, subtotal, tax, total, tendered, change));
    }

    public void logTransaction(String event, double total) {
        send(String.format("TRANSACTION|%s|%.2f", event, total));
    }

    public void logSystem(String message) {
        send("SYSTEM|" + message);
    }

    // Receipt printing methods (kept for backward compatibility)
    public String printReceipt(Transaction transaction, String paymentType,
                               double tendered, double change) {
        return printReceipt(transaction, paymentType, tendered, change, null);
    }

    public String printReceipt(Transaction transaction, String paymentType,
                               double tendered, double change,
                               DiscountService.DiscountResponse discountInfo) {
        logSystem("RECEIPT PRINTING");

        String receipt = receiptPrinter.generateReceipt(
                transaction, paymentType, tendered, change, discountInfo);

        // Send receipt to VJ server
        send("RECEIPT_START");
        for (String line : receipt.split("\n")) {
            send("RECEIPT_LINE|" + line);
        }
        send("RECEIPT_END");

        return receipt;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public VJConfig getConfig() {
        return config;
    }
}