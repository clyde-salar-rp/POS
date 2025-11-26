package org.example;

import org.example.model.Product;
import org.example.model.Transaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionDatabase {
    private final Connection connection;
    private static final String DB_URL = "jdbc:h2:./data/pos_transactions;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    private org.h2.tools.Server server;
    private static final int TCP_PORT = 9093;

    // In-memory product lookup cache (replaces ProductDatabase for products table)
    private final Map<String, Product> productCache = new HashMap<>();

    public TransactionDatabase() {
        try {
            // Start TCP server for external connections
            server = org.h2.tools.Server.createTcpServer(
                    "-tcp", "-tcpAllowOthers", "-tcpPort", String.valueOf(TCP_PORT), "-baseDir", "."
            ).start();

            System.out.println("=".repeat(70));
            System.out.println("H2 TCP Server started on port " + TCP_PORT);
            System.out.println("=".repeat(70));

            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            createTables();

            // Get absolute path for IntelliJ
            String dbPath = new java.io.File("./data/pos_transactions").getAbsolutePath();

            System.out.println("Transaction database initialized");
            System.out.println("File location: " + dbPath + ".mv.db");
            System.out.println("-".repeat(70));
            System.out.println("IntelliJ Connection Settings:");
            System.out.println("  URL: jdbc:h2:tcp://localhost:" + TCP_PORT + "/data/pos_transactions");
            System.out.println("  User: sa");
            System.out.println("  Password: (leave blank)");
            System.out.println("=".repeat(70));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize transaction database", e);
        }
    }

    private void createTables() throws SQLException {
        // Products table (persistent version of pricebook)
        String productsTable = """
            CREATE TABLE IF NOT EXISTS products (
                upc VARCHAR(255) PRIMARY KEY,
                description VARCHAR(500),
                price DECIMAL(10,2),
                category VARCHAR(50),
                last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String transactionsTable = """
            CREATE TABLE IF NOT EXISTS transactions (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                transaction_date TIMESTAMP NOT NULL,
                cashier VARCHAR(50),
                register_id VARCHAR(50),
                subtotal DECIMAL(10,2),
                discount DECIMAL(10,2) DEFAULT 0.00,
                tax DECIMAL(10,2),
                total DECIMAL(10,2),
                payment_type VARCHAR(20),
                tendered DECIMAL(10,2),
                change_amount DECIMAL(10,2),
                status VARCHAR(20),
                receipt_number INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String itemsTable = """
            CREATE TABLE IF NOT EXISTS transaction_items (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                transaction_id BIGINT NOT NULL,
                upc VARCHAR(255),
                description VARCHAR(500),
                price DECIMAL(10,2),
                quantity INTEGER,
                line_total DECIMAL(10,2),
                category VARCHAR(50),
                FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
            )
        """;

        String discountsTable = """
            CREATE TABLE IF NOT EXISTS applied_discounts (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                transaction_id BIGINT NOT NULL,
                rule_name VARCHAR(100),
                description VARCHAR(500),
                amount DECIMAL(10,2),
                FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
            )
        """;

        String indexSql = """
            CREATE INDEX IF NOT EXISTS idx_trans_date ON transactions(transaction_date);
            CREATE INDEX IF NOT EXISTS idx_trans_status ON transactions(status);
            CREATE INDEX IF NOT EXISTS idx_item_upc ON transaction_items(upc);
            CREATE INDEX IF NOT EXISTS idx_item_category ON transaction_items(category);
            CREATE INDEX IF NOT EXISTS idx_product_category ON products(category);
            CREATE INDEX IF NOT EXISTS idx_product_desc ON products(description);
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(productsTable);
            stmt.execute(transactionsTable);
            stmt.execute(itemsTable);
            stmt.execute(discountsTable);

            // Execute indexes separately
            for (String index : indexSql.split(";")) {
                if (!index.trim().isEmpty()) {
                    stmt.execute(index);
                }
            }
        }
    }

    // ========== PRODUCT MANAGEMENT (Replaces ProductDatabase) ==========

    public void loadProductsFromTSV(String filePath) throws java.io.IOException, SQLException {
        String insertSQL = """
            MERGE INTO products (upc, description, price, category) 
            VALUES (?, ?, ?, ?)
        """;

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(filePath));
             PreparedStatement stmt = connection.prepareStatement(insertSQL)) {

            String line;
            int count = 0;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 3) {
                    String upc = parts[0].trim();
                    String description = parts[1].trim();
                    double price = Double.parseDouble(parts[2].trim());
                    String category = determineCategory(description);

                    stmt.setString(1, upc);
                    stmt.setString(2, description);
                    stmt.setDouble(3, price);
                    stmt.setString(4, category);
                    stmt.addBatch();

                    // Also cache in memory for fast lookup
                    Product product = new Product(upc, description, price);
                    productCache.put(upc, product);

                    count++;
                    if (count % 100 == 0) {
                        stmt.executeBatch();
                    }
                }
            }

            // Execute remaining batch
            if (count % 100 != 0) {
                stmt.executeBatch();
            }

            System.out.println("Loaded " + count + " products into persistent database");
        }
    }

    public Product findProductByUPC(String upc) {
        // Try cache first (fast)
        if (productCache.containsKey(upc)) {
            return new Product(productCache.get(upc)); // Return copy
        }

        // Cache miss - query database and update cache
        String sql = "SELECT upc, description, price FROM products WHERE upc = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, upc);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Product product = new Product(
                            rs.getString("upc"),
                            rs.getString("description"),
                            rs.getDouble("price")
                    );
                    productCache.put(upc, product); // Update cache
                    return new Product(product); // Return copy
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying product: " + e.getMessage());
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

    public List<Product> searchProducts(String keyword) {
        List<Product> results = new ArrayList<>();
        String sql = """
            SELECT upc, description, price 
            FROM products 
            WHERE description LIKE ? OR upc LIKE ?
            ORDER BY description
            LIMIT 100
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Product(
                            rs.getString("upc"),
                            rs.getString("description"),
                            rs.getDouble("price")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching products: " + e.getMessage());
        }

        return results;
    }

    // ========== TRANSACTION MANAGEMENT ==========

    public long saveTransaction(
            Transaction transaction,
            String paymentType,
            double tendered,
            double change,
            String status,
            int receiptNumber,
            Double discount,
            org.example.service.DiscountService.DiscountResponse discountInfo
    ) throws SQLException {

        String transactionSql = """
            INSERT INTO transactions (
                transaction_date, cashier, register_id, 
                subtotal, discount, tax, total,
                payment_type, tendered, change_amount, 
                status, receipt_number
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String itemSql = """
            INSERT INTO transaction_items (
                transaction_id, upc, description, price, quantity, line_total, category
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        String discountSql = """
            INSERT INTO applied_discounts (
                transaction_id, rule_name, description, amount
            ) VALUES (?, ?, ?, ?)
        """;

        connection.setAutoCommit(false);

        try {
            // Insert transaction
            long transactionId;
            try (PreparedStatement stmt = connection.prepareStatement(transactionSql,
                    Statement.RETURN_GENERATED_KEYS)) {

                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(2, "OPERATOR01");
                stmt.setString(3, "REG-001");

                double subtotal = discountInfo != null ? discountInfo.subtotal : transaction.getSubtotal();
                double discountAmount = discount != null ? discount : 0.0;
                double tax = discountInfo != null ? discountInfo.tax : transaction.getTax();
                double total = discountInfo != null ? discountInfo.total : transaction.getTotal();

                stmt.setDouble(4, subtotal);
                stmt.setDouble(5, discountAmount);
                stmt.setDouble(6, tax);
                stmt.setDouble(7, total);
                stmt.setString(8, paymentType);
                stmt.setDouble(9, tendered);
                stmt.setDouble(10, change);
                stmt.setString(11, status);
                stmt.setInt(12, receiptNumber);

                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    transactionId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to get transaction ID");
                }
            }

            // Insert items
            try (PreparedStatement stmt = connection.prepareStatement(itemSql)) {
                for (Product product : transaction.getItems()) {
                    stmt.setLong(1, transactionId);
                    stmt.setString(2, product.getUpc());
                    stmt.setString(3, product.getDescription());
                    stmt.setDouble(4, product.getPrice());
                    stmt.setInt(5, product.getQuantity());
                    stmt.setDouble(6, product.getLineTotal());
                    stmt.setString(7, determineCategory(product.getDescription()));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            // Insert discounts
            if (discountInfo != null && discountInfo.appliedDiscounts != null) {
                try (PreparedStatement stmt = connection.prepareStatement(discountSql)) {
                    for (var discount2 : discountInfo.appliedDiscounts) {
                        stmt.setLong(1, transactionId);
                        stmt.setString(2, discount2.ruleName);
                        stmt.setString(3, discount2.description);
                        stmt.setDouble(4, discount2.amount);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            }

            connection.commit();
            return transactionId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // ========== SUSPENDED TRANSACTION MANAGEMENT ==========

    public int suspendTransaction(Transaction transaction) throws SQLException {
        if (transaction.getItemCount() == 0) {
            return -1;
        }

        String transactionSql = """
            INSERT INTO transactions (
                transaction_date, cashier, register_id, 
                subtotal, discount, tax, total,
                payment_type, tendered, change_amount, 
                status, receipt_number
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String itemSql = """
            INSERT INTO transaction_items (
                transaction_id, upc, description, price, quantity, line_total, category
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        connection.setAutoCommit(false);

        try {
            // Check if this transaction was previously suspended
            Integer existingId = transaction.getSuspendedId();

            if (existingId != null) {
                // Update existing suspended transaction
                deleteSuspendedTransaction(existingId);
            }

            // Insert new suspended transaction
            long transactionId;
            try (PreparedStatement stmt = connection.prepareStatement(transactionSql,
                    Statement.RETURN_GENERATED_KEYS)) {

                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                stmt.setString(2, "OPERATOR01");
                stmt.setString(3, "REG-001");
                stmt.setDouble(4, transaction.getSubtotal());
                stmt.setDouble(5, 0.0); // No discount for suspended
                stmt.setDouble(6, transaction.getTax());
                stmt.setDouble(7, transaction.getTotal());
                stmt.setString(8, null); // No payment yet
                stmt.setDouble(9, 0.0);
                stmt.setDouble(10, 0.0);
                stmt.setString(11, "SUSPENDED");
                stmt.setInt(12, 0); // No receipt number yet

                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    transactionId = rs.getLong(1);
                } else {
                    throw new SQLException("Failed to get transaction ID");
                }
            }

            // Insert items
            try (PreparedStatement stmt = connection.prepareStatement(itemSql)) {
                for (Product product : transaction.getItems()) {
                    stmt.setLong(1, transactionId);
                    stmt.setString(2, product.getUpc());
                    stmt.setString(3, product.getDescription());
                    stmt.setDouble(4, product.getPrice());
                    stmt.setInt(5, product.getQuantity());
                    stmt.setDouble(6, product.getLineTotal());
                    stmt.setString(7, determineCategory(product.getDescription()));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            connection.commit();
            return (int) transactionId;

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public Transaction resumeTransaction(int transactionId) throws SQLException {
        String transactionSql = """
            SELECT subtotal, tax, total 
            FROM transactions 
            WHERE id = ? AND status = 'SUSPENDED'
        """;

        String itemsSql = """
            SELECT upc, description, price, quantity, line_total
            FROM transaction_items
            WHERE transaction_id = ?
        """;

        Transaction transaction = new Transaction();
        transaction.setSuspendedId(transactionId);

        // Get transaction details
        try (PreparedStatement stmt = connection.prepareStatement(transactionSql)) {
            stmt.setInt(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null; // Transaction not found or not suspended
                }
            }
        }

        // Get items
        try (PreparedStatement stmt = connection.prepareStatement(itemsSql)) {
            stmt.setInt(1, transactionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                            rs.getString("upc"),
                            rs.getString("description"),
                            rs.getDouble("price")
                    );
                    int quantity = rs.getInt("quantity");
                    transaction.addItem(product, quantity);
                }
            }
        }

        return transaction;
    }

    public void deleteSuspendedTransaction(int transactionId) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ? AND status = 'SUSPENDED'";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            stmt.executeUpdate();
        }
    }

    public List<SuspendedTransactionInfo> getSuspendedTransactions() throws SQLException {
        String sql = """
            SELECT 
                t.id,
                t.transaction_date,
                COUNT(ti.id) as item_count,
                t.total
            FROM transactions t
            LEFT JOIN transaction_items ti ON t.id = ti.transaction_id
            WHERE t.status = 'SUSPENDED'
            GROUP BY t.id, t.transaction_date, t.total
            ORDER BY t.transaction_date DESC
        """;

        List<SuspendedTransactionInfo> suspended = new ArrayList<>();

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                suspended.add(new SuspendedTransactionInfo(
                        rs.getInt("id"),
                        rs.getTimestamp("transaction_date").toLocalDateTime(),
                        rs.getInt("item_count"),
                        rs.getDouble("total")
                ));
            }
        }

        return suspended;
    }

    public boolean hasSuspendedTransactions() throws SQLException {
        String sql = "SELECT COUNT(*) as count FROM transactions WHERE status = 'SUSPENDED'";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }

        return false;
    }

    private String determineCategory(String description) {
        String desc = description.toUpperCase();

        if (desc.contains("COKE") || desc.contains("PEPSI") || desc.contains("SPRITE") ||
                desc.contains("MONSTER") || desc.contains("RED BULL") || desc.contains("GATORADE") ||
                desc.contains("WATER") || desc.contains("TEA") || desc.contains("COFFEE")) {
            return "BEVERAGE";
        }

        if (desc.contains("PIZZA") || desc.contains("HOT DOG") || desc.contains("BURGER") ||
                desc.contains("SANDWICH") || desc.contains("DONUT") || desc.contains("TAQUITO") ||
                desc.contains("CROISSANT") || desc.contains("SAUSAGE")) {
            return "FOOD";
        }

        if (desc.contains("MARLBORO") || desc.contains("CAMEL") || desc.contains("NEWPORT") ||
                desc.contains("CIGAR") || desc.contains("VUSE") || desc.contains("JUUL")) {
            return "TOBACCO";
        }

        if (desc.contains("CHIP") || desc.contains("LAYS") || desc.contains("DORITOS") ||
                desc.contains("CHEETOS") || desc.contains("SNICKERS") || desc.contains("REESE") ||
                desc.contains("CANDY") || desc.contains("GUM")) {
            return "SNACK";
        }

        return "OTHER";
    }

    // ========== REPORTING METHODS ==========

    public DailySalesReport getDailySalesReport(LocalDateTime date) throws SQLException {
        String sql = """
            SELECT 
                COUNT(*) as transaction_count,
                COALESCE(SUM(total), 0) as total_sales,
                COALESCE(SUM(discount), 0) as total_discounts,
                COALESCE(SUM(tax), 0) as total_tax,
                COALESCE(AVG(total), 0) as avg_transaction
            FROM transactions
            WHERE CAST(transaction_date AS DATE) = ?
            AND status = 'COMPLETED'
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(date.toLocalDate()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new DailySalesReport(
                            date.toLocalDate(),
                            rs.getInt("transaction_count"),
                            rs.getDouble("total_sales"),
                            rs.getDouble("total_discounts"),
                            rs.getDouble("total_tax"),
                            rs.getDouble("avg_transaction")
                    );
                }
            }
        }

        return new DailySalesReport(date.toLocalDate(), 0, 0, 0, 0, 0);
    }

    public List<CategorySalesReport> getCategorySales(LocalDateTime startDate, LocalDateTime endDate)
            throws SQLException {
        String sql = """
            SELECT 
                ti.category,
                COUNT(DISTINCT t.id) as transaction_count,
                SUM(ti.quantity) as total_quantity,
                SUM(ti.line_total) as total_sales
            FROM transactions t
            JOIN transaction_items ti ON t.id = ti.transaction_id
            WHERE t.transaction_date BETWEEN ? AND ?
            AND t.status = 'COMPLETED'
            GROUP BY ti.category
            ORDER BY total_sales DESC
        """;

        List<CategorySalesReport> reports = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reports.add(new CategorySalesReport(
                            rs.getString("category"),
                            rs.getInt("transaction_count"),
                            rs.getInt("total_quantity"),
                            rs.getDouble("total_sales")
                    ));
                }
            }
        }

        return reports;
    }

    public List<TopSellingItem> getTopSellingItems(int limit, LocalDateTime startDate, LocalDateTime endDate)
            throws SQLException {
        String sql = """
            SELECT 
                ti.upc,
                ti.description,
                SUM(ti.quantity) as total_quantity,
                SUM(ti.line_total) as total_sales,
                COUNT(DISTINCT t.id) as transaction_count
            FROM transactions t
            JOIN transaction_items ti ON t.id = ti.transaction_id
            WHERE t.transaction_date BETWEEN ? AND ?
            AND t.status = 'COMPLETED'
            GROUP BY ti.upc, ti.description
            ORDER BY total_quantity DESC
            LIMIT ?
        """;

        List<TopSellingItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));
            stmt.setInt(3, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(new TopSellingItem(
                            rs.getString("upc"),
                            rs.getString("description"),
                            rs.getInt("total_quantity"),
                            rs.getDouble("total_sales"),
                            rs.getInt("transaction_count")
                    ));
                }
            }
        }

        return items;
    }

    public PaymentMethodReport getPaymentMethodReport(LocalDateTime startDate, LocalDateTime endDate)
            throws SQLException {
        String sql = """
            SELECT 
                payment_type,
                COUNT(*) as count,
                SUM(total) as total
            FROM transactions
            WHERE transaction_date BETWEEN ? AND ?
            AND status = 'COMPLETED'
            GROUP BY payment_type
        """;

        int cashCount = 0, creditCount = 0;
        double cashTotal = 0.0, creditTotal = 0.0;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(startDate));
            stmt.setTimestamp(2, Timestamp.valueOf(endDate));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("payment_type");
                    int count = rs.getInt("count");
                    double total = rs.getDouble("total");

                    if ("CASH".equals(type)) {
                        cashCount = count;
                        cashTotal = total;
                    } else if ("CREDIT".equals(type)) {
                        creditCount = count;
                        creditTotal = total;
                    }
                }
            }
        }

        return new PaymentMethodReport(cashCount, cashTotal, creditCount, creditTotal);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Transaction database connection closed");
            }
            if (server != null) {
                server.stop();
                System.out.println("H2 TCP Server stopped");
            }
        } catch (SQLException e) {
            System.err.println("Error closing transaction database: " + e.getMessage());
        }
    }

    // ========== REPORT DATA CLASSES ==========

    public record DailySalesReport(
            java.time.LocalDate date,
            int transactionCount,
            double totalSales,
            double totalDiscounts,
            double totalTax,
            double avgTransaction
    ) {}

    public record CategorySalesReport(
            String category,
            int transactionCount,
            int totalQuantity,
            double totalSales
    ) {}

    public record TopSellingItem(
            String upc,
            String description,
            int totalQuantity,
            double totalSales,
            int transactionCount
    ) {}

    public record PaymentMethodReport(
            int cashCount,
            double cashTotal,
            int creditCount,
            double creditTotal
    ) {
        public int totalCount() { return cashCount + creditCount; }
        public double totalSales() { return cashTotal + creditTotal; }
    }

    public record SuspendedTransactionInfo(
            int id,
            LocalDateTime suspendTime,
            int itemCount,
            double total
    ) {
        @Override
        public String toString() {
            DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
            return String.format("Trans #%d - %s - %d items - $%.2f",
                    id,
                    suspendTime.format(timeFormat),
                    itemCount,
                    total);
        }
    }
}