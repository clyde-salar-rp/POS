package org.example.ui;

import org.example.ProductDatabase;
import org.example.VirtualJournal;
import org.example.input.ScanGunListener;
import org.example.model.Product;
import org.example.model.Transaction;
import org.example.ui.components.ItemsPanel;
import org.example.ui.components.ScanPanel;
import org.example.ui.components.TotalPanel;

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
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        scanPanel = new ScanPanel(this::processUPC);
        itemsPanel = new ItemsPanel();
        totalPanel = new TotalPanel(this::clearTransaction);

        add(scanPanel, BorderLayout.NORTH);
        add(itemsPanel, BorderLayout.CENTER);
        add(totalPanel, BorderLayout.SOUTH);

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

    private void updateDisplay() {
        itemsPanel.updateItems(transaction.getItems());
        totalPanel.updateTotal(transaction.getTotal());
    }

    private void clearTransaction() {
        transaction.clear();
        updateDisplay();
        scanGunListener.reset();
        journal.logTransaction("CLEARED", 0.0);
    }
}