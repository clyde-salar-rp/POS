package org.example;

import org.example.ProductDatabase;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DatabaseInspector extends JFrame {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;
    private Connection connection;

    public DatabaseInspector() {
        setTitle("Database Inspector");
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
        String[] columns = {"UPC", "Description", "Price"};
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

        JScrollPane scrollPane = new JScrollPane(table);

        // Bottom panel with stats
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton statsButton = new JButton("Show Statistics");
        statsButton.addActionListener(e -> showStatistics());
        bottomPanel.add(statsButton);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        connectToDatabase();
        loadAllProducts();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection(
                    "jdbc:h2:mem:pos;DB_CLOSE_DELAY=-1", "sa", "");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to database: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadAllProducts() {
        tableModel.setRowCount(0);
        String sql = "SELECT upc, description, price FROM products ORDER BY description LIMIT 1000";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getString("upc"),
                        rs.getString("description"),
                        String.format("$%.2f", rs.getDouble("price"))
                });
                count++;
            }

            statusLabel.setText("Loaded " + count + " products");

        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void searchProducts(String keyword) {
        if (keyword.trim().isEmpty()) {
            loadAllProducts();
            return;
        }

        tableModel.setRowCount(0);
        String sql = "SELECT upc, description, price FROM products " +
                "WHERE description LIKE ? OR upc LIKE ? " +
                "ORDER BY description LIMIT 100";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);

            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                            rs.getString("upc"),
                            rs.getString("description"),
                            String.format("$%.2f", rs.getDouble("price"))
                    });
                    count++;
                }

                statusLabel.setText("Found " + count + " matching products");
            }

        } catch (SQLException e) {
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void showStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Database Statistics:\n\n");

        try (Statement stmt = connection.createStatement()) {
            // Total count
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM products");
            if (rs.next()) {
                stats.append("Total Products: ").append(rs.getInt("count")).append("\n");
            }

            // Price statistics
            rs = stmt.executeQuery(
                    "SELECT MIN(price) as min, MAX(price) as max, AVG(price) as avg FROM products");
            if (rs.next()) {
                stats.append(String.format("Min Price: $%.2f\n", rs.getDouble("min")));
                stats.append(String.format("Max Price: $%.2f\n", rs.getDouble("max")));
                stats.append(String.format("Avg Price: $%.2f\n", rs.getDouble("avg")));
            }

            // Most expensive items
            stats.append("\nTop 5 Most Expensive:\n");
            rs = stmt.executeQuery(
                    "SELECT description, price FROM products ORDER BY price DESC LIMIT 5");
            while (rs.next()) {
                stats.append(String.format("  %s - $%.2f\n",
                        rs.getString("description"), rs.getDouble("price")));
            }

        } catch (SQLException e) {
            stats.append("\nError: ").append(e.getMessage());
        }

        JTextArea textArea = new JTextArea(stats.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JOptionPane.showMessageDialog(this,
                new JScrollPane(textArea),
                "Database Statistics",
                JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.dispose();
    }
}