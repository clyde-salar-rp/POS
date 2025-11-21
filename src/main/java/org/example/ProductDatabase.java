package org.example;

import org.example.database.DatabaseManager;
import org.example.model.Product;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class ProductDatabase {
    private final DatabaseManager dbManager;

    public ProductDatabase() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void loadFromTSV(String filePath) throws IOException {
        String mergeSQL = "MERGE INTO products (upc, description, price) KEY(upc) VALUES (?, ?, ?)";

        int count = 0;
        Connection conn = dbManager.getConnection();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement pstmt = conn.prepareStatement(mergeSQL)) {

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 3) {
                    String upc = parts[0].trim();
                    String description = parts[1].trim();
                    double price = Double.parseDouble(parts[2].trim());

                    pstmt.setString(1, upc);
                    pstmt.setString(2, description);
                    pstmt.setDouble(3, price);
                    pstmt.addBatch();
                    count++;

                    // Execute batch every 1000 records for performance
                    if (count % 1000 == 0) {
                        pstmt.executeBatch();
                    }
                }
            }

            // Execute remaining batch
            if (count % 1000 != 0) {
                pstmt.executeBatch();
            }

        } catch (SQLException e) {
            throw new IOException("Failed to load products into database", e);
        }
    }

    public Product findByUPC(String upc) {
        String sql = "SELECT upc, description, price FROM products WHERE upc = ?";
        Connection conn = dbManager.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, upc);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Product(
                            rs.getString("upc"),
                            rs.getString("description"),
                            rs.getDouble("price")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        return null;
    }

    public int getProductCount() {
        String sql = "SELECT COUNT(*) FROM products";
        Connection conn = dbManager.getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }

        return 0;
    }

    public void close() {
        dbManager.close();
    }

    // Utility method for debugging
    public void printAllProducts() {
        String sql = "SELECT upc, description, price FROM products ORDER BY upc LIMIT 10";
        Connection conn = dbManager.getConnection();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("\n=== Sample Products (First 10) ===");
            while (rs.next()) {
                System.out.printf("UPC: %s | %s | $%.2f%n",
                        rs.getString("upc"),
                        rs.getString("description"),
                        rs.getDouble("price"));
            }
            System.out.println("==================================\n");

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }
}