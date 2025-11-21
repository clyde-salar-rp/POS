package org.example;

import org.example.model.Product;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class ProductDatabase {
    private Connection connection;
    private static final String DB_URL = "jdbc:h2:mem:pos;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private org.h2.tools.Server webServer;

    public ProductDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTable();
            startWebConsole();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void startWebConsole() {
        try {
            // Start H2 web console on port 8082
            webServer = org.h2.tools.Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082");
            webServer.start();

            System.out.println("=".repeat(60));
            System.out.println("H2 Database Console Started!");
            System.out.println("URL: http://localhost:8082");
            System.out.println("=".repeat(60));
            System.out.println("IntelliJ Connection Settings:");
            System.out.println("  JDBC URL: " + DB_URL);
            System.out.println("  User: " + DB_USER);
            System.out.println("  Password: (leave blank)");
            System.out.println("  ⚠️  Check 'Keep connection alive' in IntelliJ!");
            System.out.println("=".repeat(60));
        } catch (SQLException e) {
            System.err.println("Could not start H2 web console: " + e.getMessage());
        }
    }

    private void createTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS products (
                upc VARCHAR(255) PRIMARY KEY,
                description VARCHAR(500),
                price DECIMAL(10,2)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void loadFromTSV(String filePath) throws IOException {
        String insertSQL = "INSERT INTO products (upc, description, price) VALUES (?, ?, ?)";

        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {

            String line;
            int batchCount = 0;

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

                    batchCount++;

                    // Execute batch every 100 records
                    if (batchCount % 100 == 0) {
                        pstmt.executeBatch();
                    }
                }
            }

            // Execute remaining records
            if (batchCount % 100 != 0) {
                pstmt.executeBatch();
            }

        } catch (SQLException e) {
            throw new IOException("Failed to load data into database", e);
        }
    }

    public Product findByUPC(String upc) {
        String sql = "SELECT upc, description, price FROM products WHERE upc = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
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
            System.err.println("Error querying database: " + e.getMessage());
        }

        return null;
    }

    public int getProductCount() {
        String sql = "SELECT COUNT(*) as count FROM products";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("Error counting products: " + e.getMessage());
        }

        return 0;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            if (webServer != null) {
                webServer.stop();
                System.out.println("H2 Web Console stopped");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    // Optional: Method to search by description
    public Product[] searchByDescription(String keyword) {
        String sql = "SELECT upc, description, price FROM products WHERE description LIKE ? LIMIT 10";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + keyword + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                java.util.List<Product> results = new java.util.ArrayList<>();

                while (rs.next()) {
                    results.add(new Product(
                            rs.getString("upc"),
                            rs.getString("description"),
                            rs.getDouble("price")
                    ));
                }

                return results.toArray(new Product[0]);
            }
        } catch (SQLException e) {
            System.err.println("Error searching database: " + e.getMessage());
        }

        return new Product[0];
    }
}