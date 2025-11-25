package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Product;
import org.example.model.Transaction;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class DiscountService {
    private static final String DISCOUNT_API_URL = "http://localhost:8080/discount";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DiscountService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public DiscountResponse calculateDiscount(Transaction transaction) throws IOException, InterruptedException {
        // Build request payload
        DiscountRequest request = new DiscountRequest();
        request.items = new ArrayList<>();

        for (Product product : transaction.getItems()) {
            DiscountRequest.Item item = new DiscountRequest.Item();
            item.upc = product.getUpc();
            item.description = product.getDescription();
            item.price = product.getPrice();
            item.quantity = product.getQuantity();
            item.category = determineCategory(product);
            request.items.add(item);
        }

        // Serialize request
        String jsonBody = objectMapper.writeValueAsString(request);

        // Create HTTP request
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(DISCOUNT_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Send request
        HttpResponse<String> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString()
        );

        // Check response status
        if (response.statusCode() != 200) {
            throw new IOException("Discount API returned status: " + response.statusCode());
        }

        // Parse response
        return objectMapper.readValue(response.body(), DiscountResponse.class);
    }

    private String determineCategory(Product product) {
        String desc = product.getDescription().toUpperCase();

        // Beverage keywords
        if (desc.contains("COKE") || desc.contains("PEPSI") || desc.contains("SPRITE") ||
                desc.contains("MONSTER") || desc.contains("RED BULL") || desc.contains("GATORADE") ||
                desc.contains("WATER") || desc.contains("TEA") || desc.contains("COFFEE")) {
            return "BEVERAGE";
        }

        // Food keywords
        if (desc.contains("PIZZA") || desc.contains("HOT DOG") || desc.contains("BURGER") ||
                desc.contains("SANDWICH") || desc.contains("DONUT") || desc.contains("TAQUITO") ||
                desc.contains("CROISSANT") || desc.contains("SAUSAGE")) {
            return "FOOD";
        }

        // Tobacco keywords
        if (desc.contains("MARLBORO") || desc.contains("CAMEL") || desc.contains("NEWPORT") ||
                desc.contains("CIGAR") || desc.contains("VUSE") || desc.contains("JUUL")) {
            return "TOBACCO";
        }

        // Snack keywords
        if (desc.contains("CHIP") || desc.contains("LAYS") || desc.contains("DORITOS") ||
                desc.contains("CHEETOS") || desc.contains("SNICKERS") || desc.contains("REESE") ||
                desc.contains("CANDY") || desc.contains("GUM")) {
            return "SNACK";
        }

        return "OTHER";
    }

    // Request classes
    public static class DiscountRequest {
        public List<Item> items;

        public static class Item {
            public String upc;
            public String description;
            public double price;
            public int quantity;
            public String category;
        }
    }

    // Response classes
    public static class DiscountResponse {
        public double subtotal;
        public double tax;
        public double total;
        public double totalDiscount;
        public List<AppliedDiscount> appliedDiscounts;

        public static class AppliedDiscount {
            public String ruleName;
            public String description;
            public double amount;
            public List<String> affectedItems;
        }
    }
}