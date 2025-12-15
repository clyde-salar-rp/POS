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
    // Change this to your API endpoint:
    // - Local development: "http://localhost:8080"
    // - AWS production: "http://discount-api-alb-1415305850.ap-southeast-2.elb.amazonaws.com"
    private static final String BASE_API_URL = "http://localhost:8080";
    private static final String DISCOUNT_ENDPOINT = BASE_API_URL + "/discount";
    private static final String ACTIVE_RULES_ENDPOINT = BASE_API_URL + "/api/discount-rules/active";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DiscountService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches all currently active discount rules from the API
     * @return List of active discount rules
     * @throws IOException if network error occurs
     * @throws InterruptedException if request is interrupted
     */
    public List<DiscountRuleInfo> getActiveDiscountRules() throws IOException, InterruptedException {
        System.out.println("📡 Fetching active rules from: " + ACTIVE_RULES_ENDPOINT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ACTIVE_RULES_ENDPOINT))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("📥 Response Status: " + response.statusCode());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to fetch active rules. Status: " + response.statusCode());
        }

        System.out.println("📄 Response Body: " + response.body());

        // Parse response as array of DiscountRuleInfo
        DiscountRuleInfo[] rules = objectMapper.readValue(response.body(), DiscountRuleInfo[].class);
        System.out.println("✅ Parsed " + rules.length + " discount rules");

        for (DiscountRuleInfo rule : rules) {
            System.out.println("   - " + rule.name + " (" + rule.ruleType + ")");
        }

        return List.of(rules);
    }

    /**
     * Calculates discounts for a transaction using the active rules
     * @param transaction The transaction to calculate discounts for
     * @return DiscountResponse containing calculated discounts
     * @throws IOException if network error occurs
     * @throws InterruptedException if request is interrupted
     */
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
                .uri(URI.create(DISCOUNT_ENDPOINT))
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
                desc.contains("WATER") || desc.contains("TEA") || desc.contains("COFFEE") ||
                desc.contains("POLAR POP")) {
            return "BEVERAGE";
        }

        // Food keywords
        if (desc.contains("PIZZA") || desc.contains("HOT DOG") || desc.contains("BURGER") ||
                desc.contains("SANDWICH") || desc.contains("DONUT") || desc.contains("TAQUITO") ||
                desc.contains("ROLLER") || desc.contains("FOOD")) {
            return "FOOD";
        }

        // Tobacco keywords
        if (desc.contains("CIGARETTE") || desc.contains("CIGAR") || desc.contains("TOBACCO") ||
                desc.contains("VAPE") || desc.contains("MARLBORO") || desc.contains("CAMEL")) {
            return "TOBACCO";
        }

        return "GENERAL";
    }

    // ===== REQUEST/RESPONSE CLASSES =====

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

    public static class DiscountResponse {
        public double subtotal;
        public double tax;
        public double total;
        public double totalDiscount;
        public List<AppliedDiscount> appliedDiscounts;

        public DiscountResponse() {
            this.appliedDiscounts = new ArrayList<>();
        }

        public static class AppliedDiscount {
            public String ruleName;
            public String description;
            public double amount;
            public List<String> affectedItems;
        }
    }

    /**
     * Represents information about a discount rule from the API
     */
    public static class DiscountRuleInfo {
        public Long id;
        public String name;
        public String description;
        public String ruleType;
        public Double percentOff;
        public String category;
        public Integer buyQuantity;
        public Integer freeQuantity;
        public String itemKeyword;
        public Integer requiredQuantity;
        public Double bundlePrice;
        public Boolean active;
        public Integer priority;
        public String createdAt;
        public String updatedAt;

        @Override
        public String toString() {
            return String.format("%s (%s) - %s", name, ruleType, description);
        }

        /**
         * Returns a user-friendly display string for the discount
         */
        public String getDisplayString() {
            switch (ruleType) {
                case "PERCENT_OFF":
                    String categoryInfo = (category != null && !category.isEmpty())
                            ? " on " + category
                            : "";
                    return String.format("%.0f%% off%s", percentOff, categoryInfo);

                case "BUY_ONE_GET_ONE":
                    return "Buy One Get One Free on " + category;

                case "BUY_X_GET_Y":
                    return String.format("Buy %d Get %d Free on %s items",
                            buyQuantity, freeQuantity, itemKeyword);

                case "MIX_AND_MATCH":
                    return String.format("Mix & Match: %d for $%.2f",
                            requiredQuantity, bundlePrice);

                default:
                    return description;
            }
        }
    }
}