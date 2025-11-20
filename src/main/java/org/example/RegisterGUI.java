package org.example;

import org.example.model.Product;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;

public class RegisterGUI extends JFrame {
    private PricebookParser parser;
    private JTextField scanField;
    private JTable itemTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;
    private ArrayList<Product> scannedItems;
    private double runningTotal;
    private BarcodeScannerListener scannerListener;

    // Modern color palette
    private static final Color PRIMARY_BG = new Color(248, 249, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color ACCENT_COLOR = new Color(79, 70, 229);
    private static final Color TEXT_PRIMARY = new Color(17, 24, 39);
    private static final Color BORDER_COLOR = new Color(229, 231, 235);

    public RegisterGUI() {
        parser = new PricebookParser();
        scannedItems = new ArrayList<>();
        runningTotal = 0.0;

        loadPricebook();

        setTitle("POS Register");
        setSize(1000, 700);
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

        JButton scanButton = createStyledButton("Manual Scan", ACCENT_COLOR, Color.WHITE);

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

        JLabel titleLabel = new JLabel("Transaction Items");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Create table with columns
        String[] columnNames = {"#", "UPC", "Description", "Price"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };

        itemTable = new JTable(tableModel);
        itemTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        itemTable.setRowHeight(32);
        itemTable.setShowVerticalLines(false);
        itemTable.setGridColor(BORDER_COLOR);
        itemTable.setSelectionBackground(new Color(238, 242, 255));
        itemTable.setSelectionForeground(TEXT_PRIMARY);
        itemTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        itemTable.getTableHeader().setBackground(PRIMARY_BG);
        itemTable.getTableHeader().setForeground(TEXT_PRIMARY);
        itemTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));

        // Set column widths
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(50);  // #
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(150); // UPC
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(400); // Description
        itemTable.getColumnModel().getColumn(3).setPreferredWidth(100); // Price

        // Right-align price column
        var priceRenderer = new javax.swing.table.DefaultTableCellRenderer();
        priceRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        itemTable.getColumnModel().getColumn(3).setCellRenderer(priceRenderer);

        JScrollPane scrollPane = new JScrollPane(itemTable);
        scrollPane.setBorder(null);

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

        // Left side - action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonPanel.setBackground(CARD_BG);

        JButton clearButton = createStyledButton("Clear Transaction", PRIMARY_BG, TEXT_PRIMARY);
        clearButton.addActionListener(e -> clearTransaction());

        buttonPanel.add(clearButton);

        // Right side - total
        totalLabel = new JLabel("TOTAL: $0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        totalLabel.setForeground(TEXT_PRIMARY);
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        totalPanel.add(buttonPanel, BorderLayout.WEST);
        totalPanel.add(totalLabel, BorderLayout.EAST);

        add(totalPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, 14));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (bg.equals(ACCENT_COLOR)) {
            button.setBorderPainted(false);
            button.setBorder(new EmptyBorder(8, 20, 8, 20));
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(new Color(67, 56, 202));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(ACCENT_COLOR);
                }
            });
        } else {
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
            ));
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(new Color(243, 244, 246));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(bg);
                }
            });
        }

        return button;
    }

    private void processScannedItem(String upc, String source) {
        upc = upc.trim();

        if (upc.isEmpty()) {
            return;
        }

        logEvent(source, "Scanned UPC: " + upc);

        Product product = parser.searchByUPC(upc);

        if (product != null) {
            scannedItems.add(product);
            runningTotal += product.getPrice();

            logEvent(source, String.format("Item found - UPC: %s, Desc: %s, Price: $%.2f",
                    product.getUpc(), product.getDescription(), product.getPrice()));

            updateDisplay();
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
        // Clear existing rows
        tableModel.setRowCount(0);

        // Add all scanned items
        for (int i = 0; i < scannedItems.size(); i++) {
            Product item = scannedItems.get(i);
            Object[] row = {
                    i + 1,
                    item.getUpc(),
                    item.getDescription(),
                    String.format("$%.2f", item.getPrice())
            };
            tableModel.addRow(row);
        }

        // Update total
        totalLabel.setText("TOTAL: $" + String.format("%.2f", runningTotal));

        // Scroll to bottom
        if (itemTable.getRowCount() > 0) {
            itemTable.scrollRectToVisible(
                    itemTable.getCellRect(itemTable.getRowCount() - 1, 0, true)
            );
        }
    }

    private void clearTransaction() {
        scannedItems.clear();
        runningTotal = 0.0;
        tableModel.setRowCount(0);
        totalLabel.setText("TOTAL: $0.00");
        scanField.setText("");
        logEvent("SYSTEM", "Transaction cleared");
    }

    private void logEvent(String source, String message) {
        System.out.println("[" + source + "] " + message);
    }

    /**
     * Barcode Scanner Listener
     * Captures rapid keyboard input from physical barcode scanners
     */
    private class BarcodeScannerListener implements KeyEventDispatcher {
        private StringBuilder buffer = new StringBuilder();
        private long lastKeyTime = 0;
        private static final long SCANNER_SPEED_THRESHOLD = 50;
        private static final int MIN_BARCODE_LENGTH = 8;

        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID() != KeyEvent.KEY_TYPED || e.getComponent() == scanField) {
                return false;
            }

            long currentTime = System.currentTimeMillis();
            long timeSinceLastKey = currentTime - lastKeyTime;

            if (timeSinceLastKey > SCANNER_SPEED_THRESHOLD && buffer.length() > 0) {
                buffer.setLength(0);
            }

            lastKeyTime = currentTime;
            char c = e.getKeyChar();

            if (c == '\n' || c == '\r') {
                if (buffer.length() >= MIN_BARCODE_LENGTH) {
                    String scannedCode = buffer.toString();
                    buffer.setLength(0);

                    SwingUtilities.invokeLater(() -> {
                        processScannedItem(scannedCode, "BARCODE_SCANNER");
                    });

                    return true;
                } else {
                    buffer.setLength(0);
                }
            } else if (Character.isLetterOrDigit(c) || c == '-') {
                buffer.append(c);

                if (buffer.length() > 50) {
                    buffer.setLength(0);
                }
            }

            return false;
        }
    }
}