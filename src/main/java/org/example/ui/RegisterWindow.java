package org.example.ui;

import org.example.ProductDatabase;
import org.example.VirtualJournal;
import org.example.input.ScanGunListener;
import org.example.model.Product;
import org.example.model.Transaction;
import org.example.model.TransactionItem;
import org.example.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class RegisterWindow extends JFrame {
    private final ProductDatabase database;
    private final VirtualJournal journal;
    private final Transaction transaction;

    private ScanPanel scanPanel;
    private ItemsPanel itemsPanel;
    private TotalPanel totalPanel;
    private ActionPanel actionPanel;
    private QuickKeysPanel quickKeysPanel;
    private ScanGunListener scanGunListener;

    public RegisterWindow() {
        this.database = new ProductDatabase();
        this.journal = new VirtualJournal();
        this.transaction = new Transaction();

        loadPricebook();
        setupUI();
        setupScanGun();

        journal.logSystem("Register initialized with " + database.getProductCount() + " products");
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
        setTitle("POS Register");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        scanPanel = new ScanPanel(this::processUPC);
        itemsPanel = new ItemsPanel();
        totalPanel = new TotalPanel(this::voidTransaction);
        actionPanel = new ActionPanel(
                this::voidItem,
                this::changeQuantity,
                this::tenderExactDollar,
                this::tenderNextDollar,
                this::tenderCash,
                this::tenderCredit
        );
        quickKeysPanel = new QuickKeysPanel(this::processQuickKey);

        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        rightPanel.add(actionPanel);
        rightPanel.add(quickKeysPanel);

        add(scanPanel, BorderLayout.NORTH);
        add(itemsPanel, BorderLayout.CENTER);
        add(totalPanel, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);

        setVisible(true);
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
        if (selectedRow >= 0) {
            TransactionItem item = transaction.getItems().get(selectedRow);
            journal.logVoidItem(item);
            transaction.getItems().remove(selectedRow);
        } else {
            TransactionItem item = transaction.getLastItem();
            journal.logVoidItem(item);
            transaction.voidLastItem();
        }
        updateDisplay();
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

        TransactionItem item = transaction.getItems().get(selectedRow);
        String input = JOptionPane.showInputDialog(this,
                "Enter new quantity for " + item.getProduct().getDescription() + ":",
                item.getQuantity());

        if (input != null) {
            try {
                int newQty = Integer.parseInt(input.trim());
                if (newQty > 0) {
                    int oldQty = item.getQuantity();
                    transaction.changeQuantity(selectedRow, newQty);
                    journal.logQuantityChange(item, oldQty, newQty);
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

        String input = JOptionPane.showInputDialog(this,
                String.format("Total: $%.2f\nEnter cash tendered:", transaction.getTotal()));

        if (input != null) {
            try {
                double tendered = Double.parseDouble(input.trim());
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

        if (change > 0) {
            JOptionPane.showMessageDialog(this,
                    String.format("Change due: $%.2f", change),
                    "Transaction Complete", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Transaction Complete",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }

        voidTransaction();
    }

    private void voidTransaction() {
        if (transaction.getItemCount() > 0) {
            journal.logTransaction("VOIDED", transaction.getTotal());
        }
        transaction.clear();
        updateDisplay();
        scanGunListener.reset();
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