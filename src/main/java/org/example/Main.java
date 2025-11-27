package org.example;
import javax.swing.*;

import org.example.ui.RegisterWindow;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default
        }
        // Initialize Receipt Printer
        ReceiptPrinter receiptPrinter = new ReceiptPrinter();

        // Initialize VJ Client
        VirtualJournalClient vjClient = new VirtualJournalClient(receiptPrinter);

        // Try to connect in background (non-blocking)
        new Thread(() -> vjClient.connect()).start();

        // Add shutdown hook to disconnect cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            vjClient.disconnect();
        }));
        SwingUtilities.invokeLater(() -> new RegisterWindow(vjClient));
    }
}
