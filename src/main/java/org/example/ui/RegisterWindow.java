package org.example.ui;

import org.example.TransactionDatabase;
import org.example.VirtualJournal;
import org.example.ReceiptPrinter;
import org.example.VirtualJournalClient;
import org.example.input.ScanGunListener;
import org.example.model.Product;
import org.example.model.Transaction;
import org.example.model.TransactionManager;
import org.example.service.DiscountService;
import org.example.ui.components.*;
import org.example.ui.dialogs.SuspendedTransactionsDialog;
import org.example.ui.dialogs.VJConfigDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;

public class RegisterWindow extends JFrame {
    private final TransactionDatabase database;
    private final VirtualJournalClient journal;

    private final ReceiptPrinter receiptPrinter;
    private final TransactionManager transactionManager;
    private final DiscountService discountService;
    private Transaction transaction;

    private enum RegisterMode {
        TRANSACTION,
        TENDERING
    }

    private RegisterMode currentMode = RegisterMode.TRANSACTION;
    private CardLayout cardLayout;
    private JPanel cardPanel;

    private JPanel transactionView;
    private JPanel tenderingView;
    private TenderPanel tenderPanel;
    private ItemsPanel readOnlyItemsPanel;
    private TotalPanel readOnlyTotalPanel;

    private ScanPanel scanPanel;
    private ItemsPanel itemsPanel;
    private TotalPanel totalPanel;
    private SuspendPanel suspendPanel;
    private QuickKeysPanel quickKeysPanel;
    private ScanGunListener scanGunListener;

    private DiscountService.DiscountResponse currentDiscount;

    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color ACCENT_COLOR = new Color(245, 245, 250);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);

    public RegisterWindow(VirtualJournalClient vjClient) {
        this.database = new TransactionDatabase();
        this.receiptPrinter = new ReceiptPrinter();
        this.journal = vjClient;
        this.transactionManager = new TransactionManager(database); // CHANGED: Pass database
        this.discountService = new DiscountService();
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
            journal.disconnect();
        }));
    }

    private void loadPricebook() {
        String[] paths = {"pricebook.tsv", "src/main/resources/pricebook.tsv"};

        for (String path : paths) {
            if (new File(path).exists()) {
                try {
                    database.loadProductsFromTSV(path);
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

        JMenuItem reportsItem = new JMenuItem("Sales Reports");
        reportsItem.setFont(new Font("SansSerif", Font.PLAIN, 12));
        reportsItem.addActionListener(e -> openReportsWindow());
        toolsMenu.add(reportsItem);

        JMenuItem vjConfigItem = new JMenuItem("VJ Server Settings");
        vjConfigItem.setFont(new Font("SansSerif", Font.PLAIN, 12));
        vjConfigItem.addActionListener(e -> openVJConfig());
        toolsMenu.add(vjConfigItem);

        menuBar.add(toolsMenu);
        setJMenuBar(menuBar);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(ACCENT_COLOR);

        transactionView = buildTransactionView();
        tenderingView = buildTenderingView();

        cardPanel.add(transactionView, "TRANSACTION");
        cardPanel.add(tenderingView, "TENDERING");

        add(cardPanel);
        setVisible(true);
    }

    private void openVJConfig() {
        VJConfigDialog.showDialog(this, journal.getConfig());
    }

    private void openReportsWindow() {
        new org.example.ui.ReportsWindow(database);
    }

    private void openDatabaseInspector() {
        new org.example.DatabaseInspector(database);
    }

    private JPanel buildTransactionView() {
        JPanel view = new JPanel(new BorderLayout(20, 20));
        view.setBackground(ACCENT_COLOR);
        view.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel leftPanel = new JPanel(new BorderLayout(15, 15));
        leftPanel.setBackground(ACCENT_COLOR);
        leftPanel.setPreferredSize(new Dimension(850, 0));

        scanPanel = new ScanPanel(this::processUPC);
        itemsPanel = new ItemsPanel();
        totalPanel = new TotalPanel();

        leftPanel.add(scanPanel, BorderLayout.NORTH);
        leftPanel.add(itemsPanel, BorderLayout.CENTER);
        leftPanel.add(totalPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(ACCENT_COLOR);
        rightPanel.setPreferredSize(new Dimension(500, 0));

        suspendPanel = new SuspendPanel(
                this::suspendTransaction,
                this::resumeTransaction
        );

        TransactionActionPanel transActionPanel = new TransactionActionPanel(
                this::voidItem,
                this::changeQuantity,
                this::voidTransaction
        );

        quickKeysPanel = new QuickKeysPanel(this::processQuickKey);

        JButton paymentButton = createLargeButton("PROCEED TO PAYMENT", SUCCESS_COLOR, this::enterTenderingMode);

        suspendPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        transActionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        quickKeysPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        paymentButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        rightPanel.add(suspendPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        rightPanel.add(transActionPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        rightPanel.add(quickKeysPanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        rightPanel.add(paymentButton);
        rightPanel.add(Box.createVerticalGlue());

        view.add(leftPanel, BorderLayout.CENTER);
        view.add(rightPanel, BorderLayout.EAST);

        return view;
    }

    private JPanel buildTenderingView() {
        JPanel view = new JPanel(new BorderLayout(20, 20));
        view.setBackground(ACCENT_COLOR);
        view.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel leftPanel = new JPanel(new BorderLayout(15, 15));
        leftPanel.setBackground(ACCENT_COLOR);
        leftPanel.setPreferredSize(new Dimension(850, 0));

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(WARNING_COLOR);
        JLabel headerLabel = new JLabel("PAYMENT IN PROGRESS - SCANNING DISABLED");
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);

        readOnlyItemsPanel = new ItemsPanel();
        readOnlyTotalPanel = new TotalPanel();

        leftPanel.add(headerPanel, BorderLayout.NORTH);
        leftPanel.add(readOnlyItemsPanel, BorderLayout.CENTER);
        leftPanel.add(readOnlyTotalPanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(ACCENT_COLOR);
        rightPanel.setPreferredSize(new Dimension(500, 0));

        tenderPanel = new TenderPanel(
                this::tenderExactDollar,
                this::tenderNextDollar,
                this::tenderCash,
                this::tenderCredit,
                this::cancelTendering
        );

        tenderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        rightPanel.add(tenderPanel);
        rightPanel.add(Box.createVerticalGlue());

        view.add(leftPanel, BorderLayout.CENTER);
        view.add(rightPanel, BorderLayout.EAST);

        return view;
    }

    private JButton createLargeButton(String text, Color bgColor, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 70));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        button.addActionListener(e -> action.run());
        return button;
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
        if (currentMode == RegisterMode.TENDERING) {
            journal.logSystem("SCAN BLOCKED - Currently in tendering mode");
            return;
        }

        upc = upc.trim();
        if (upc.isEmpty()) return;

        Product product = database.findProductByUPC(upc);
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
        if (currentMode == RegisterMode.TENDERING) {
            return;
        }

        journal.logQuickKey(product.getDescription(), product.getPrice());
        transaction.addItem(product);
        updateDisplay();
    }

    private void enterTenderingMode() {
        if (transaction.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No items in transaction");
            return;
        }

        currentMode = RegisterMode.TENDERING;
        journal.logSystem("Entered TENDERING mode - scan gun DISABLED");

        scanGunListener.setEnabled(false);

        if (!calculateDiscount()) {
            return;
        }

        updateTenderingView();
        cardLayout.show(cardPanel, "TENDERING");
    }

    private void cancelTendering() {
        currentMode = RegisterMode.TRANSACTION;
        currentDiscount = null;

        scanGunListener.setEnabled(true);
        journal.logSystem("Cancelled tendering - scan gun RE-ENABLED");

        updateDisplay();
        cardLayout.show(cardPanel, "TRANSACTION");
    }

    private void updateTenderingView() {
        if (currentDiscount != null) {
            readOnlyItemsPanel.updateItems(transaction.getItems());
            readOnlyTotalPanel.updateTotals(
                    currentDiscount.subtotal,
                    currentDiscount.tax,
                    currentDiscount.total,
                    currentDiscount.totalDiscount
            );

            if (tenderPanel != null) {
                tenderPanel.updateTotal(currentDiscount.total);
            }
        } else {
            readOnlyItemsPanel.updateItems(transaction.getItems());
            readOnlyTotalPanel.updateTotals(
                    transaction.getSubtotal(),
                    transaction.getTax(),
                    transaction.getTotal(),
                    0.0
            );

            if (tenderPanel != null) {
                tenderPanel.updateTotal(transaction.getTotal());
            }
        }
    }

    private boolean calculateDiscount() {
        try {
            journal.logSystem("Calculating discounts...");
            currentDiscount = discountService.calculateDiscount(transaction);

            if (currentDiscount.totalDiscount > 0) {
                journal.logSystem(String.format("Discount applied: $%.2f", currentDiscount.totalDiscount));
                for (DiscountService.DiscountResponse.AppliedDiscount discount : currentDiscount.appliedDiscounts) {
                    journal.logSystem(String.format("  - %s: $%.2f (%s)",
                            discount.ruleName, discount.amount, discount.description));
                }
            } else {
                journal.logSystem("No discounts applied");
            }

            return true;

        } catch (Exception e) {
            journal.logSystem("Error calculating discount: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Could not calculate discounts. Proceeding without discounts.\n\n" +
                            "Error: " + e.getMessage(),
                    "Discount Error",
                    JOptionPane.WARNING_MESSAGE);

            currentDiscount = new DiscountService.DiscountResponse();
            currentDiscount.subtotal = transaction.getSubtotal();
            currentDiscount.tax = transaction.getTax();
            currentDiscount.total = transaction.getTotal();
            currentDiscount.totalDiscount = 0.0;

            return true;
        }
    }

    private void completeTender(String paymentType, double tendered, double change) {
        if (currentDiscount == null) return;

        double subtotal = currentDiscount.subtotal;
        double tax = currentDiscount.tax;
        double total = currentDiscount.total;
        double discount = currentDiscount.totalDiscount;

        journal.logTender(paymentType, subtotal, tax, total, tendered, change);

        if (discount > 0) {
            journal.logSystem(String.format("Total discount: $%.2f", discount));
        }

        String receiptText = journal.printReceipt(
                transaction,
                paymentType,
                tendered,
                change,
                currentDiscount
        );

        // Save transaction to database
        try {
            long txId = database.saveTransaction(
                    transaction,
                    paymentType,
                    tendered,
                    change,
                    "COMPLETED",
                    receiptPrinter.getReceiptNumber(),
                    discount,
                    currentDiscount
            );
            journal.logSystem("Transaction saved to database (ID: " + txId + ")");
        } catch (SQLException e) {
            journal.logSystem("ERROR: Failed to save transaction - " + e.getMessage());
            e.printStackTrace();
        }

        if (change > 0) {
            JOptionPane.showMessageDialog(this,
                    String.format("Change due: $%.2f", change),
                    "Transaction Complete", JOptionPane.INFORMATION_MESSAGE);
        }

        org.example.ui.dialogs.ReceiptDialog.showReceipt(this, receiptText);

        completeTransaction();

        currentMode = RegisterMode.TRANSACTION;
        scanGunListener.setEnabled(true);
        journal.logSystem("Transaction completed - scan gun RE-ENABLED");
        cardLayout.show(cardPanel, "TRANSACTION");
    }

    private void tenderExactDollar() {
        if (currentMode != RegisterMode.TENDERING) return;
        if (currentDiscount == null) return;
        completeTender("CASH", currentDiscount.total, 0.0);
    }

    private void tenderNextDollar() {
        if (currentMode != RegisterMode.TENDERING) return;
        if (currentDiscount == null) return;
        double nextDollar = Math.ceil(currentDiscount.total);
        completeTender("CASH", nextDollar, nextDollar - currentDiscount.total);
    }

    private void tenderCash() {
        if (currentMode != RegisterMode.TENDERING) return;
        if (currentDiscount == null) return;

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
                String.format("Total: $%.2f", currentDiscount.total),
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

                if (tendered >= currentDiscount.total) {
                    completeTender("CASH", tendered, tendered - currentDiscount.total);
                } else {
                    JOptionPane.showMessageDialog(this, "Insufficient payment");
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid amount");
            }
        }
    }

    private void tenderCredit() {
        if (currentMode != RegisterMode.TENDERING) return;
        if (currentDiscount == null) return;
        completeTender("CREDIT", currentDiscount.total, 0.0);
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
                        if (newStr.isEmpty() && !text.isEmpty()) {
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

    private void voidTransaction() {
        if (transaction.getItemCount() > 0) {
            journal.logTransaction("VOIDED", transaction.getTotal());
        }
        currentDiscount = null;
        transaction.clear();
        updateDisplay();
        scanGunListener.reset();
    }

    private void completeTransaction() {
        if(transaction.getItemCount() > 0) {
            double finalTotal = (currentDiscount != null)
                    ? currentDiscount.total : transaction.getTotal();
            journal.logTransaction("COMPLETED", finalTotal);
        }
        currentDiscount = null;
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

            currentDiscount = null;
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

        // CHANGED: Now returns SuspendedTransactionInfo instead of SuspendedTransaction
        org.example.TransactionDatabase.SuspendedTransactionInfo suspended =
                SuspendedTransactionsDialog.showDialog(this, transactionManager);

        if (suspended != null) {
            // CHANGED: Use suspended.id() instead of suspended.getId()
            Transaction resumedTransaction = transactionManager.resumeTransaction(suspended.id());

            if (resumedTransaction != null) {
                transaction = resumedTransaction;
                currentDiscount = null;
                updateDisplay();

                journal.logTransaction("RESUMED (ID: " + suspended.id() + ")", transaction.getTotal());

                JOptionPane.showMessageDialog(this,
                        String.format("Transaction #%d resumed\n\nItems: %d\nTotal: $%.2f",
                                suspended.id(),
                                transaction.getItemCount(),
                                transaction.getTotal()),
                        "Transaction Resumed",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to resume transaction #" + suspended.id(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateDisplay() {
        itemsPanel.updateItems(transaction.getItems());

        if (currentDiscount == null) {
            totalPanel.updateTotals(
                    transaction.getSubtotal(),
                    transaction.getTax(),
                    transaction.getTotal(),
                    0.0
            );
        } else {
            totalPanel.updateTotals(
                    currentDiscount.subtotal,
                    currentDiscount.tax,
                    currentDiscount.total,
                    currentDiscount.totalDiscount
            );
        }
    }
}