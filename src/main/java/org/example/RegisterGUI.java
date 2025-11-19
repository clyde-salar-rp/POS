package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

public class RegisterGUI extends JFrame {
    private PricebookParser parser;
    private JTextField scanField;
    private JTextArea itemDisplay;
    private JLabel totalLabel;
    private ArrayList<Product> scannedItems;
    private double runningTotal;
    private BarcodeScannerListener scannerListener;

    public RegisterGUI() {
        parser = new PricebookParser();
        scannedItems = new ArrayList<>();
        runningTotal = 0.0;

        loadPricebook();

        setTitle("POS Register");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        createScanPanel();
        createItemDisplay();
        createTotalDisplay();

        // Initialize barcode scanner listener
        scannerListener = new BarcodeScannerListener();
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(scannerListener);

        setVisible(true);

        logEvent("SYSTEM", "Register initialized with " + parser.getProductCount() + " products");
        logEvent("SYSTEM", "Barcode scanner ready - scan items anywhere");
    }

    private void loadPricebook() {
        String[] paths = {
                "pricebook.tsv",
                "src/main/java/org/example/pricebook.tsv",
                "src/main/resources/pricebook.tsv"
        };

        boolean loaded = false;
        for (String path : paths) {
            File file = new File(path);
            if (file.exists()) {
                parser.parseTSV(path);
                loaded = true;
                System.out.println("Loaded pricebook from: " + path);
                break;
            }
        }

        if (!loaded) {
            JOptionPane.showMessageDialog(this,
                    "Could not find pricebook.tsv. Please ensure it exists in the project root.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createScanPanel() {
        JPanel scanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        scanPanel.setBorder(BorderFactory.createTitledBorder("Manual Entry / Barcode Scanner"));

        JLabel scanLabel = new JLabel("UPC:");
        scanField = new JTextField(20);
        JButton scanButton = new JButton("Manual Scan");

        // Manual entry with Enter key
        scanField.addActionListener(e -> processScannedItem(scanField.getText(), "MANUAL_ENTRY"));
        scanButton.addActionListener(e -> processScannedItem(scanField.getText(), "MANUAL_ENTRY"));

        scanPanel.add(scanLabel);
        scanPanel.add(scanField);
        scanPanel.add(scanButton);

        add(scanPanel, BorderLayout.NORTH);
    }

    private void createItemDisplay() {
        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createTitledBorder("Cashier Display"));

        itemDisplay = new JTextArea(20, 60);
        itemDisplay.setEditable(false);
        itemDisplay.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(itemDisplay);
        displayPanel.add(scrollPane, BorderLayout.CENTER);

        add(displayPanel, BorderLayout.CENTER);
    }

    private void createTotalDisplay() {
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        totalLabel = new JLabel("TOTAL: $0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 24));
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JButton clearButton = new JButton("Clear Transaction");
        clearButton.addActionListener(e -> clearTransaction());

        totalPanel.add(clearButton, BorderLayout.WEST);
        totalPanel.add(totalLabel, BorderLayout.EAST);

        add(totalPanel, BorderLayout.SOUTH);
    }

    private void processScannedItem(String upc, String source) {
        upc = upc.trim();

        if (upc.isEmpty()) {
            return;
        }

        logEvent(source, "Scanned UPC: " + upc);

        // Look up item in HashMap via PricebookParser
        Product product = parser.searchByUPC(upc);

        if (product != null) {
            scannedItems.add(product);
            runningTotal += product.getPrice();

            logEvent(source, "Item found in HashMap");
            logEvent(source, "  Description: " + product.getDescription());
            logEvent(source, "  Price: $" + String.format("%.2f", product.getPrice()));
            logEvent(source, "  UPC: " + product.getUpc());

            updateDisplay();

            // Visual feedback
            flashScanField(true);
        } else {
            logEvent(source, "ERROR - UPC not found in HashMap: " + upc);
            JOptionPane.showMessageDialog(this,
                    "Item not found in pricebook: " + upc,
                    "Not Found",
                    JOptionPane.WARNING_MESSAGE);
            flashScanField(false);
        }

        scanField.setText("");
        scanField.requestFocus();
    }

    private void flashScanField(boolean success) {
        Color originalColor = scanField.getBackground();
        Color flashColor = success ? new Color(200, 255, 200) : new Color(255, 200, 200);

        scanField.setBackground(flashColor);
        Timer timer = new Timer(200, e -> scanField.setBackground(originalColor));
        timer.setRepeats(false);
        timer.start();
    }

    private void updateDisplay() {
        StringBuilder display = new StringBuilder();
        display.append(String.format("%-15s %-40s %10s\n", "UPC", "DESCRIPTION", "PRICE"));
        display.append("-".repeat(70)).append("\n");

        for (Product item : scannedItems) {
            display.append(String.format("%-15s %-40s $%9.2f\n",
                    item.getUpc(),
                    truncate(item.getDescription(), 40),
                    item.getPrice()));
        }

        itemDisplay.setText(display.toString());
        totalLabel.setText("TOTAL: $" + String.format("%.2f", runningTotal));

        itemDisplay.setCaretPosition(itemDisplay.getDocument().getLength());
    }

    private void clearTransaction() {
        scannedItems.clear();
        runningTotal = 0.0;
        itemDisplay.setText("");
        totalLabel.setText("TOTAL: $0.00");
        scanField.setText("");
        logEvent("SYSTEM", "Transaction cleared");
    }

    private void logEvent(String source, String message) {
        System.out.println("[" + source + "] " + message);
    }

    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Barcode Scanner Listener
     * Captures rapid keyboard input from physical barcode scanners
     * Real scanners type the full barcode + Enter in <100ms
     */
    private class BarcodeScannerListener implements KeyEventDispatcher {
        private StringBuilder buffer = new StringBuilder();
        private long lastKeyTime = 0;
        private static final long SCANNER_SPEED_THRESHOLD = 50; // ms between chars
        private static final int MIN_BARCODE_LENGTH = 8; // Minimum UPC length

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            // Only process key typed events, not from the manual entry field
            if (e.getID() != KeyEvent.KEY_TYPED || e.getComponent() == scanField) {
                return false;
            }

            long currentTime = System.currentTimeMillis();
            long timeSinceLastKey = currentTime - lastKeyTime;

            // Reset buffer if typing is too slow (human typing)
            if (timeSinceLastKey > SCANNER_SPEED_THRESHOLD && buffer.length() > 0) {
                buffer.setLength(0);
            }

            lastKeyTime = currentTime;
            char c = e.getKeyChar();

            // Handle Enter/Return - process the scanned barcode
            if (c == '\n' || c == '\r') {
                if (buffer.length() >= MIN_BARCODE_LENGTH) {
                    String scannedCode = buffer.toString();
                    buffer.setLength(0);

                    // Process on EDT
                    SwingUtilities.invokeLater(() -> {
                        processScannedItem(scannedCode, "BARCODE_SCANNER");
                    });

                    return true; // Consume the event
                } else {
                    buffer.setLength(0);
                }
            }
            // Build up the barcode - accept digits and some letters
            else if (Character.isLetterOrDigit(c) || c == '-') {
                buffer.append(c);

                // Prevent buffer overflow
                if (buffer.length() > 50) {
                    buffer.setLength(0);
                }
            }

            return false;
        }
    }

}