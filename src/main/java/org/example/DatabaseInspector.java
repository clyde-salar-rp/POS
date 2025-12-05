package org.example;

import org.example.TransactionDatabase; // CHANGED
import org.example.model.Product;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DatabaseInspector extends JFrame {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private final TransactionDatabase database;

    public DatabaseInspector(TransactionDatabase database) {
        this.database = database;

        setTitle("Product Database Inspector");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create UI
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("Refresh");
        JButton searchButton = new JButton("Search");
        JTextField searchField = new JTextField(20);
        statusLabel = new JLabel("Ready");

        refreshButton.addActionListener(e -> loadAllProducts());
        searchButton.addActionListener(e -> searchProducts(searchField.getText()));
        searchField.addActionListener(e -> searchProducts(searchField.getText()));

        topPanel.add(new JLabel("Search:"));
        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(refreshButton);
        topPanel.add(statusLabel);

        // Create table
        String[] columns = {"UPC", "Description", "Price", "Category"}; // ADDED: Category column
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(400);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100); // ADDED

        JScrollPane scrollPane = new JScrollPane(table);

        // Bottom panel with stats
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton statsButton = new JButton("Show Statistics");
        statsButton.addActionListener(e -> showStatistics());
        bottomPanel.add(statsButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        loadAllProducts();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadAllProducts() {
        tableModel.setRowCount(0);

        List<Product> products = database.searchProducts("");

        for (Product product : products) {
            tableModel.addRow(new Object[]{
                    product.getUpc(),
                    product.getDescription(),
                    String.format("$%.2f", product.getPrice()),
                    determineCategory(product.getDescription()) // ADDED
            });
        }

        statusLabel.setText("Loaded " + products.size() + " products (max 1000 shown)");
    }

    private void searchProducts(String keyword) {
        if (keyword.trim().isEmpty()) {
            loadAllProducts();
            return;
        }

        tableModel.setRowCount(0);
        List<Product> products = database.searchProducts(keyword);

        for (Product product : products) {
            tableModel.addRow(new Object[]{
                    product.getUpc(),
                    product.getDescription(),
                    String.format("$%.2f", product.getPrice()),
                    determineCategory(product.getDescription()) // ADDED
            });
        }

        statusLabel.setText("Found " + products.size() + " matching products");
    }

    private void showStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Database Statistics:\n\n");
        stats.append("Total Products: ").append(database.getProductCount()).append("\n");
        stats.append("\nNote: More detailed statistics available in Sales Reports\n");

        JTextArea textArea = new JTextArea(stats.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JOptionPane.showMessageDialog(this,
                new JScrollPane(textArea),
                "Database Statistics",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private String determineCategory(String description) {
        String desc = description.toUpperCase();

        if (desc.contains("COKE") || desc.contains("PEPSI") || desc.contains("SPRITE") ||
                desc.contains("MONSTER") || desc.contains("RED BULL") || desc.contains("GATORADE") ||
                desc.contains("WATER") || desc.contains("TEA") || desc.contains("COFFEE")) {
            return "BEVERAGE";
        }

        if (desc.contains("PIZZA") || desc.contains("HOT DOG") || desc.contains("BURGER") ||
                desc.contains("SANDWICH") || desc.contains("DONUT") || desc.contains("TAQUITO")) {
            return "FOOD";
        }

        if (desc.contains("MARLBORO") || desc.contains("CAMEL") || desc.contains("NEWPORT") ||
                desc.contains("CIGAR") || desc.contains("VUSE") || desc.contains("JUUL")) {
            return "TOBACCO";
        }

        if (desc.contains("CHIP") || desc.contains("LAYS") || desc.contains("DORITOS") ||
                desc.contains("SNICKERS") || desc.contains("CANDY")) {
            return "SNACK";
        }

        return "OTHER";
    }
}