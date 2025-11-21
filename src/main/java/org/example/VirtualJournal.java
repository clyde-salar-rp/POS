package org.example;

import org.example.model.Product;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class VirtualJournal {
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

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

    public void logTransaction(String event, double total) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] TRANSACTION | %s | Total: $%.2f%n",
                timestamp, event, total);
    }

    public void logSystem(String message) {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        System.out.printf("[%s] SYSTEM | %s%n", timestamp, message);
    }
}
