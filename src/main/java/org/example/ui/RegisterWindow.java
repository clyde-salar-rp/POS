package org.example.ui;

import org.example.ProductDatabase;
import org.example.VirtualJournal;
import org.example.input.ScanGunListener;
import org.example.model.Product;
import org.example.model.Transaction;
import org.example.model.TransactionManager;
import org.example.ui.components.*;
import org.example.ui.dialogs.SuspendedTransactionsDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class RegisterWindow extends JFrame {
    private final ProductDatabase database;
    private final VirtualJournal journal;
    private final TransactionManager transactionManager;
    private Transaction transaction;

    private ScanPanel scanPanel;
    private ItemsPanel itemsPanel;
    private TotalPanel totalPanel;
    private ActionPanel actionPanel;
    private SuspendPanel suspendPanel;
    private QuickKeysPanel quickKeysPanel;
    private ScanGunListener scanGunListener;

    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color ACCENT_COLOR = new Color(245, 245, 250);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);

    public RegisterWindow() {
        this.database = new ProductDatabase();
        this.journal = new VirtualJournal();
        this.transactionManager = new TransactionManager();
        this.transaction = new Transaction();

        loadPricebook();
        setupUI();
        setupScanGun();
        setupShutdownHook();

        journal.logSystem("Register initialized with " + database.getProductCount() + " products");
    }

    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            journal.logSystem("Shutting down - closing database connection");
            database.close();
        }));
    }

    private void loadPricebook() {
        String[] paths = {"pricebook.tsv", "src/main/resources/pricebook.tsv"};

        for (String path : paths) {
            if (new File(path).exists()) {
                try {
                    database.loadFromTSV(path);
                    journal.logSystem("Loaded pricebook from: " + path);
                    return;
                } catch (Exception e) {
                    journal.logSystem("Error loading pricebook: " + e.getMessage());
                }
            }
        }

        JOptionPane.showMessageDialog(this,
                "Could not find pricebook.tsv",
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void setupUI() {
        setTitle("Modern POS Register");
        setSize(1400, 850);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(ACCENT_COLOR);

        // Menu bar with modern styling
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(PRIMARY_COLOR);
        menuBar.setBorderPainted(false);

        JMenu toolsMenu = new JMenu("Tools");
        toolsMenu.setForeground(Color.WHITE);
        toolsMenu.setFont(new Font("SansSerif", Font.BOLD, 13));

        JMenuItem dbInspectorItem = new JMenuItem("Database Inspector");
        dbInspectorItem.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dbInspectorItem.addActionListener(e -> openDatabaseInspector());
        toolsMenu.add(dbInspectorItem);

        menuBar.add(toolsMenu);
        setJMenuBar(menuBar);

        // Create main layout
        JPanel mainContainer = new JPanel(new BorderLayout(20, 20));
        mainContainer.setBackground(ACCENT_COLOR);
        mainContainer.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Left side - Transaction area
        JPanel leftPanel = new JPanel(new BorderLayout(15, 15));
        leftPanel.setBackground(ACCENT_COLOR);
        leftPanel.setPreferredSize(new Dimension(850, 0));

        scanPanel = new ScanPanel(this::processUPC);
        itemsPanel = new ItemsPanel();
        totalPanel = new TotalPanel();

        leftPanel.add(scanPanel, BorderLayout.NORTH);
        leftPanel.add(itemsPanel, BorderLayout.CENTER);
        leftPanel.add(totalPanel, BorderLayout.SOUTH);

        // Right side - Controls
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(ACCENT_COLOR);
        rightPanel.setPreferredSize(new Dimension(500, 0));

        suspendPanel = new SuspendPanel(
                this::suspendTransaction,
                this::resumeTransaction
        );
        actionPanel = new ActionPanel(
                this::voidItem,
                this::changeQuantity,
                this::tenderExactDollar,
                this::tenderNextDollar,
                this::tenderCash,
                this::tenderCredit,
                this::voidTransaction
        );
        quickKeysPanel = new QuickKeysPanel(this::processQuickKey);

        // Add spacing between panels
        suspendPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        quickKeysPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        rightPanel.add(suspendPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        rightPanel.add(actionPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        rightPanel.add(quickKeysPanel);
        rightPanel.add(Box.createVerticalGlue());

        mainContainer.add(leftPanel, BorderLayout.CENTER);
        mainContainer.add(rightPanel, BorderLayout.EAST);

        add(mainContainer);
        setVisible(true);
    }

    private void openDatabaseInspector() {
        new org.example.DatabaseInspector();
    }

    private void setupScanGun() {
        scanGunListener = new ScanGunListener(
                this::processUPC,
                scanPanel.getScanField()
        );

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(scanGunListener);
    }

    private void processUPC(String upc, String source) {
        upc = upc.trim();
        if (upc.isEmpty()) return;

        Product product = database.findByUPC(upc);
        journal.logScan(source, upc, product);

        if (product != null) {
            transaction.addItem(product);
            updateDisplay();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Item not found: " + upc,
                    "Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void processQuickKey(Product product) {
        journal.logQuickKey(product.getDescription(), product.getPrice());
        transaction.addItem(product);
        updateDisplay();
    }

    private void voidItem() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items to void");
            return;
        }

        int selectedRow = itemsPanel.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an item to void",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Product product = transaction.getItem(selectedRow);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "Invalid item selection");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("Void this item?\n\n%s\nQty: %d\nPrice: $%.2f",
                        product.getDescription(),
                        product.getQuantity(),
                        product.getLineTotal()),
                "Confirm Void",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            journal.logVoidItem(product);
            transaction.voidItem(selectedRow);
            updateDisplay();
        }
    }

    private void changeQuantity() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in transaction");
            return;
        }

        int selectedRow = itemsPanel.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select an item");
            return;
        }

        Product product = transaction.getItem(selectedRow);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "Invalid item selection");
            return;
        }

        JTextField qtyField = new JTextField(10);
        qtyField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        qtyField.setText(String.valueOf(product.getQuantity()));
        qtyField.selectAll();

        ((javax.swing.text.AbstractDocument) qtyField.getDocument()).setDocumentFilter(
                new javax.swing.text.DocumentFilter() {
                    @Override
                    public void insertString(FilterBypass fb, int offset, String string,
                                             javax.swing.text.AttributeSet attr)
                            throws javax.swing.text.BadLocationException {
                        if (string == null) return;

                        String newStr = string.replaceAll("[^0-9]", "");
                        if (newStr.isEmpty() || wouldExceedLimit(fb.getDocument(), offset, newStr, 0)) {
                            Toolkit.getDefaultToolkit().beep();
                            return;
                        }
                        super.insertString(fb, offset, newStr, attr);
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String text,
                                        javax.swing.text.AttributeSet attrs)
                            throws javax.swing.text.BadLocationException {
                        if (text == null) return;

                        String newStr = text.replaceAll("[^0-9]", "");
                        if (newStr.isEmpty() && text.length() > 0) {
                            Toolkit.getDefaultToolkit().beep();
                            return;
                        }
                        if (wouldExceedLimit(fb.getDocument(), offset, newStr, length)) {
                            Toolkit.getDefaultToolkit().beep();
                            return;
                        }
                        super.replace(fb, offset, length, newStr, attrs);
                    }

                    private boolean wouldExceedLimit(javax.swing.text.Document doc, int offset,
                                                     String newText, int replaceLength) {
                        try {
                            String current = doc.getText(0, doc.getLength());
                            String before = current.substring(0, offset);
                            String after = current.substring(offset + replaceLength);
                            String result = before + newText + after;
                            return result.length() > 7;
                        } catch (javax.swing.text.BadLocationException e) {
                            return false;
                        }
                    }
                });

        Object[] message = {
                "Item: " + product.getDescription(),
                "Enter new quantity (max 7 digits):",
                qtyField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Change Quantity",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String input = qtyField.getText().trim();

            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Quantity cannot be empty");
                return;
            }

            try {
                int newQty = Integer.parseInt(input);
                if (newQty > 0) {
                    int oldQty = product.getQuantity();
                    transaction.changeQuantity(selectedRow, newQty);
                    journal.logQuantityChange(product, oldQty, newQty);
                    updateDisplay();
                } else {
                    JOptionPane.showMessageDialog(this, "Quantity must be positive");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid quantity");
            }
        }
    }

    private void tenderExactDollar() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in transaction");
            return;
        }
        completeTender("CASH", transaction.getTotal(), 0.0);
    }

    private void tenderNextDollar() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in transaction");
            return;
        }
        double total = transaction.getTotal();
        double nextDollar = Math.ceil(total);
        completeTender("CASH", nextDollar, nextDollar - total);
    }

    private void tenderCash() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in transaction");
            return;
        }

        JTextField cashField = new JTextField(10);
        cashField.setFont(new Font("Monospaced", Font.PLAIN, 16));

        ((javax.swing.text.AbstractDocument) cashField.getDocument()).setDocumentFilter(
                new javax.swing.text.DocumentFilter() {
                    @Override
                    public void insertString(FilterBypass fb, int offset, String string,
                                             javax.swing.text.AttributeSet attr)
                            throws javax.swing.text.BadLocationException {
                        if (string == null) return;

                        String newStr = string.replaceAll("[^0-9.]", "");
                        if (wouldExceedLimit(fb.getDocument(), offset, newStr, 0)) {
                            Toolkit.getDefaultToolkit().beep();
                            return;
                        }
                        super.insertString(fb, offset, newStr, attr);
                    }

                    @Override
                    public void replace(FilterBypass fb, int offset, int length, String text,
                                        javax.swing.text.AttributeSet attrs)
                            throws javax.swing.text.BadLocationException {
                        if (text == null) return;

                        String newStr = text.replaceAll("[^0-9.]", "");
                        if (wouldExceedLimit(fb.getDocument(), offset, newStr, length)) {
                            Toolkit.getDefaultToolkit().beep();
                            return;
                        }
                        super.replace(fb, offset, length, newStr, attrs);
                    }

                    private boolean wouldExceedLimit(javax.swing.text.Document doc, int offset,
                                                     String newText, int replaceLength) {
                        try {
                            String current = doc.getText(0, doc.getLength());
                            String before = current.substring(0, offset);
                            String after = current.substring(offset + replaceLength);
                            String result = before + newText + after;
                            String digitsOnly = result.replace(".", "");
                            return digitsOnly.length() > 7;
                        } catch (javax.swing.text.BadLocationException e) {
                            return false;
                        }
                    }
                });

        Object[] message = {
                String.format("Total: $%.2f", transaction.getTotal()),
                "Enter cash tendered (max 7 digits):",
                cashField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Cash Payment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String input = cashField.getText().trim();

            if (input.isEmpty()) {
                return;
            }

            try {
                double tendered = Double.parseDouble(input);
                double total = transaction.getTotal();

                if (tendered >= total) {
                    completeTender("CASH", tendered, tendered - total);
                } else {
                    JOptionPane.showMessageDialog(this, "Insufficient payment");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid amount");
            }
        }
    }

    private void tenderCredit() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in transaction");
            return;
        }
        completeTender("CREDIT", transaction.getTotal(), 0.0);
    }

    private void completeTender(String paymentType, double tendered, double change) {
        double subtotal = transaction.getSubtotal();
        double tax = transaction.getTax();
        double total = transaction.getTotal();

        journal.logTender(paymentType, subtotal, tax, total, tendered, change);

        // Print receipt to virtual journal and get receipt text
        String receiptText = journal.printReceipt(transaction, paymentType, tendered, change);

        // Show change dialog if applicable
        if (change > 0) {
            JOptionPane.showMessageDialog(this,
                    String.format("Change due: $%.2f", change),
                    "Transaction Complete", JOptionPane.INFORMATION_MESSAGE);
        }

        // Show receipt dialog
        org.example.ui.dialogs.ReceiptDialog.showReceipt(this, receiptText);

        completeTransaction();
    }

    private void voidTransaction() {
        if (transaction.getItemCount() > 0) {
            journal.logTransaction("VOIDED", transaction.getTotal());
        }
        transaction.clear();
        updateDisplay();
        scanGunListener.reset();
    }

    private void completeTransaction() {
        if(transaction.getItemCount() > 0) {
            journal.logTransaction("COMPLETED", transaction.getTotal());
        }
        transaction.clear();
        updateDisplay();
        scanGunListener.reset();
    }

    private void suspendTransaction() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No items to suspend",
                    "Empty Transaction",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int transactionId = transactionManager.suspendTransaction(transaction);

        if (transactionId > 0) {
            journal.logTransaction("SUSPENDED (ID: " + transactionId + ")", transaction.getTotal());

            JOptionPane.showMessageDialog(this,
                    String.format("Transaction #%d suspended\n\nItems: %d\nTotal: $%.2f",
                            transactionId,
                            transaction.getItemCount(),
                            transaction.getTotal()),
                    "Transaction Suspended",
                    JOptionPane.INFORMATION_MESSAGE);

            transaction.clear();
            updateDisplay();
            scanGunListener.reset();
        }
    }

    private void resumeTransaction() {
        if (transaction.getItemCount() > 0) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Current transaction will be lost. Continue?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        TransactionManager.SuspendedTransaction suspended =
                SuspendedTransactionsDialog.showDialog(this, transactionManager);

        if (suspended != null) {
            Transaction resumedTransaction = transactionManager.resumeTransaction(suspended.getId());

            if (resumedTransaction != null) {
                transaction = resumedTransaction;
                updateDisplay();

                journal.logTransaction("RESUMED (ID: " + suspended.getId() + ")", transaction.getTotal());

                JOptionPane.showMessageDialog(this,
                        String.format("Transaction #%d resumed\n\nItems: %d\nTotal: $%.2f",
                                suspended.getId(),
                                transaction.getItemCount(),
                                transaction.getTotal()),
                        "Transaction Resumed",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void updateDisplay() {
        itemsPanel.updateItems(transaction.getItems());
        totalPanel.updateTotals(
                transaction.getSubtotal(),
                transaction.getTax(),
                transaction.getTotal()
        );
    }
}