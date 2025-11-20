package org.example;

import javax.swing.*;
import javax.swing.border.*;
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

    // Modern color palette
    private static final Color PRIMARY_BG = new Color(248, 249, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color ACCENT_COLOR = new Color(79, 70, 229);
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);
    private static final Color ERROR_COLOR = new Color(239, 68, 68);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color TEXT_SECONDARY = new Color(107, 114, 128);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);

    public RegisterGUI() {
        parser = new PricebookParser();
        scannedItems = new ArrayList<>();
        runningTotal = 0.0;

        loadPricebook();

        setTitle("POS Register");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(PRIMARY_BG);
        setLayout(new BorderLayout(0, 0));

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
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(CARD_BG);
        outerPanel.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(20, 25, 20, 25)
        ));

        JPanel scanPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        scanPanel.setBackground(CARD_BG);

        JLabel scanLabel = new JLabel("UPC:");
        scanLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        scanLabel.setForeground(TEXT_PRIMARY);

        scanField = new JTextField(25);
        scanField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scanField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        scanField.setBackground(PRIMARY_BG);

        JButton scanButton = new JButton("Manual Scan");
        scanButton.setFont(new Font("SansSerif", Font.PLAIN, 13));
        scanButton.setBackground(ACCENT_COLOR);
        scanButton.setForeground(Color.WHITE);
        scanButton.setFocusPainted(false);
        scanButton.setBorderPainted(false);
        scanButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        scanButton.setBorder(new EmptyBorder(8, 20, 8, 20));

        scanButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                scanButton.setBackground(new Color(67, 56, 202));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                scanButton.setBackground(ACCENT_COLOR);
            }
        });

        // Manual entry with Enter key
        scanField.addActionListener(e -> processScannedItem(scanField.getText(), "MANUAL_ENTRY"));
        scanButton.addActionListener(e -> processScannedItem(scanField.getText(), "MANUAL_ENTRY"));

        scanPanel.add(scanLabel);
        scanPanel.add(scanField);
        scanPanel.add(scanButton);

        outerPanel.add(scanPanel, BorderLayout.CENTER);
        add(outerPanel, BorderLayout.NORTH);
    }

    private void createItemDisplay() {
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.setBackground(PRIMARY_BG);
        outerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBackground(CARD_BG);
        displayPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel("Cashier Display");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));

        itemDisplay = new JTextArea(20, 60);
        itemDisplay.setEditable(false);
        itemDisplay.setFont(new Font("Monospaced", Font.PLAIN, 13));
        itemDisplay.setBackground(new Color(249, 250, 251));
        itemDisplay.setForeground(TEXT_PRIMARY);
        itemDisplay.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(itemDisplay);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(249, 250, 251));

        displayPanel.add(titleLabel, BorderLayout.NORTH);
        displayPanel.add(scrollPane, BorderLayout.CENTER);

        outerPanel.add(displayPanel, BorderLayout.CENTER);
        add(outerPanel, BorderLayout.CENTER);
    }

    private void createTotalDisplay() {
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(CARD_BG);
        totalPanel.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(25, 25, 25, 25)
        ));

        JButton clearButton = new JButton("Clear Transaction");
        clearButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        clearButton.setBackground(PRIMARY_BG);
        clearButton.setForeground(TEXT_PRIMARY);
        clearButton.setFocusPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        clearButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                clearButton.setBackground(new Color(243, 244, 246));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                clearButton.setBackground(PRIMARY_BG);
            }
        });

        clearButton.addActionListener(e -> clearTransaction());

        totalLabel = new JLabel("TOTAL: $0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        totalLabel.setForeground(TEXT_PRIMARY);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

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
        Color flashColor = success ? new Color(220, 252, 231) : new Color(254, 226, 226);

        scanField.setBackground(flashColor);
        Timer timer = new Timer(300, e -> scanField.setBackground(originalColor));
        timer.setRepeats(false);
        timer.start();
    }

    private void updateDisplay() {
        StringBuilder display = new StringBuilder();
        display.append(String.format("%-18s  %-45s  %12s\n", "UPC", "DESCRIPTION", "PRICE"));
        display.append("─".repeat(80)).append("\n\n");

        for (Product item : scannedItems) {
            display.append(String.format("%-18s  %-45s  $%10.2f\n",
                    item.getUpc(),
                    truncate(item.getDescription(), 45),
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