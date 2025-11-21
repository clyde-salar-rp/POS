package org.example;

import org.example.model.Product;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProductDatabase {
    private final Map<String, Product> products;

    public ProductDatabase() {
        this.products = new HashMap<>();
    }

    public void loadFromTSV(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length >= 3) {
                    String upc = parts[0].trim();
                    String description = parts[1].trim();
                    double price = Double.parseDouble(parts[2].trim());
                    products.put(upc, new Product(upc, description, price));
                }
            }
        }
    }

    public Product findByUPC(String upc) {
        return products.get(upc);
    }

    public int getProductCount() {
        return products.size();
    }
}