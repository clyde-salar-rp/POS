package org.example;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        PricebookParser parser = new PricebookParser();

        // Parse the TSV file
        parser.parseTSV("pricebook.tsv");

        System.out.println("\n--- Total Products ---");
        System.out.println("Total products loaded: " + parser.getProductCount());

        // Search by UPC
        System.out.println("\n--- Search by UPC ---");
        Product p1 = parser.searchByUPC("041594904794");
        if (p1 != null) {
            System.out.println("Found: " + p1);
        } else {
            System.out.println("Product not found");
        }

        // Search by description
        System.out.println("\n--- Search by Description (MONSTER) ---");
        List<Product> monsters = parser.searchByDescription("MONSTER");
        System.out.println("Found " + monsters.size() + " products:");
        for (int i = 0; i < Math.min(5, monsters.size()); i++) {
            System.out.println("  " + monsters.get(i));
        }

        // Search by price range
        System.out.println("\n--- Products between $2.00 and $3.00 ---");
        List<Product> priceRange = parser.searchByPriceRange(2.00, 3.00);
        System.out.println("Found " + priceRange.size() + " products:");
        for (int i = 0; i < Math.min(5, priceRange.size()); i++) {
            System.out.println("  " + priceRange.get(i));
        }

        // Check if product exists
        System.out.println("\n--- Check Product Existence ---");
        System.out.println("Does UPC 028200003843 exist? " + parser.hasProduct("028200003843"));
        System.out.println("Does UPC 999999999999 exist? " + parser.hasProduct("999999999999"));
    }
}