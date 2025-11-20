package org.example;

import java.io.*;
import java.util.*;
import org.example.Product;

public class PricebookParser {
    private Map<String, Product> productMap;

    public PricebookParser() {
        productMap = new HashMap<>();
    }

    // Parse TSV file
    public void parseTSV(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int count = 0;

            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");

                if (parts.length >= 3) {
                    String upc = parts[0].trim();
                    String description = parts[1].trim();
                    double price = Double.parseDouble(parts[2].trim());

                    Product product = new Product(upc, description, price);
                    productMap.put(upc, product);

                    count++;
                }
            }

            System.out.println("Parsed " + count + " products successfully");
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Error parsing price: " + e.getMessage());
        }
    }

    // Search product by UPC
    public Product searchByUPC(String upc) {
        return productMap.get(upc);
    }

    // Get total count
    public int getProductCount() {
        return productMap.size();
    }
}