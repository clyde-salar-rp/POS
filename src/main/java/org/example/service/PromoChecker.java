package org.example.service;

import org.example.model.Product;
import org.example.model.Transaction;
import java.util.*;

/**
 * Checks for available promotions when items are scanned
 * and determines what additional items are needed to activate them
 *
 * MATCHES DISCOUNT API RULES:
 * 1. Polar Pop: Buy 2 Get 1 Free
 * 2. Food: 5% off all food items
 * 3. Beverages: BOGO (Buy One Get One Free)
 */
public class PromoChecker {

    public static class PromoOpportunity {
        private final String promoName;
        private final String description;
        private final double potentialSavings;
        private final List<ItemToAdd> itemsNeeded;
        private final String promoType;

        public PromoOpportunity(String promoName, String description,
                                double potentialSavings, List<ItemToAdd> itemsNeeded,
                                String promoType) {
            this.promoName = promoName;
            this.description = description;
            this.potentialSavings = potentialSavings;
            this.itemsNeeded = itemsNeeded;
            this.promoType = promoType;
        }

        public String getPromoName() { return promoName; }
        public String getDescription() { return description; }
        public double getPotentialSavings() { return potentialSavings; }
        public List<ItemToAdd> getItemsNeeded() { return itemsNeeded; }
        public String getPromoType() { return promoType; }
    }

    public static class ItemToAdd {
        private final Product product;
        private final int quantity;

        public ItemToAdd(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
    }

    /**
     * Check if scanning this product creates a promotion opportunity
     * Priority order matches discount API:
     * 1. Polar Pop Buy 2 Get 1 (most specific)
     * 2. Food 5% off
     * 3. Beverage BOGO (most general)
     */
    public PromoOpportunity checkForPromoOpportunity(Product scannedProduct,
                                                     Transaction currentTransaction) {

        String category = determineCategory(scannedProduct.getDescription());
        String desc = scannedProduct.getDescription().toUpperCase();

        // Priority 1: Check Polar Pop Buy 2 Get 1 (most specific promotion)
        if (desc.contains("POLAR POP")) {
            PromoOpportunity polarPopOpp = checkPolarPopOpportunity(scannedProduct, currentTransaction);
            if (polarPopOpp != null) {
                return polarPopOpp;
            }
        }

        // Priority 2: Check Food 5% off
        if (category.equals("FOOD")) {
            PromoOpportunity foodOpp = checkFoodDiscountOpportunity(scannedProduct, currentTransaction);
            if (foodOpp != null) {
                return foodOpp;
            }
        }

        // Priority 3: Check Beverage BOGO (general beverage promotion)
        if (category.equals("BEVERAGE")) {
            PromoOpportunity bogoOpp = checkBeverageBOGOOpportunity(scannedProduct, currentTransaction);
            if (bogoOpp != null) {
                return bogoOpp;
            }
        }

        return null;
    }

    /**
     * Check Polar Pop Buy 2 Get 1 Free opportunity
     * Rule: Buy 2 Polar Pops, Get 1 Free
     */
    private PromoOpportunity checkPolarPopOpportunity(Product scannedProduct,
                                                      Transaction transaction) {
        int polarPopCount = countProductKeywordInTransaction(transaction, "POLAR POP");

        // Buy 2 Get 1 = need 3 total for a complete set
        int remainder = polarPopCount % 3;

        // If we have 1 or 2 Polar Pops, suggest completing the set
        if (remainder > 0 && remainder < 3) {
            int needed = 3 - remainder;
            double savings = scannedProduct.getPrice(); // 1 free Polar Pop

            List<ItemToAdd> itemsNeeded = new ArrayList<>();
            itemsNeeded.add(new ItemToAdd(scannedProduct, needed));

            return new PromoOpportunity(
                    "Polar Pop Buy 2 Get 1 Free",
                    String.format("Add %d more Polar Pop to get 1 FREE!", needed),
                    savings,
                    itemsNeeded,
                    "BUY_X_GET_Y"
            );
        }

        return null;
    }

    /**
     * Check Food 5% off opportunity
     * Rule: 5% off all food items
     */
    private PromoOpportunity checkFoodDiscountOpportunity(Product scannedProduct,
                                                          Transaction transaction) {
        // Calculate total food in transaction (including just scanned item)
        double currentFoodTotal = calculateCategoryTotal(transaction, "FOOD");
        double savings = currentFoodTotal * 0.05; // 5% off

        if (savings > 0) {
            return new PromoOpportunity(
                    "5% Off Food",
                    "You're getting 5% off all Food items!",
                    savings,
                    new ArrayList<>(), // No items needed - already eligible
                    "PERCENT_OFF"
            );
        }

        return null;
    }

    /**
     * Check BOGO (Buy One Get One) for Beverages
     * Rule: BOGO on all beverages (Monster, Red Bull, etc.)
     * Note: Polar Pop has its own specific rule and is handled separately
     */
    private PromoOpportunity checkBeverageBOGOOpportunity(Product scannedProduct,
                                                          Transaction transaction) {
        // Don't apply BOGO to Polar Pop - it has its own promotion
        if (scannedProduct.getDescription().toUpperCase().contains("POLAR POP")) {
            return null;
        }

        // Count how many beverages (excluding Polar Pop) are in the transaction
        int beverageCount = countNonPolarPopBeverages(transaction);

        // BOGO requires pairs (2, 4, 6, etc.)
        // If we have an odd number, we need one more to complete the pair
        if (beverageCount % 2 == 1) {
            // Calculate potential savings (price of cheapest beverage)
            double cheapestBeveragePrice = findCheapestNonPolarPopBeverage(transaction);

            List<ItemToAdd> itemsNeeded = new ArrayList<>();
            itemsNeeded.add(new ItemToAdd(scannedProduct, 1));

            return new PromoOpportunity(
                    "BOGO Beverages",
                    "Buy One Get One FREE on all Beverages! Add 1 more to activate.",
                    cheapestBeveragePrice,
                    itemsNeeded,
                    "BOGO"
            );
        }

        return null;
    }

    // Helper methods

    private int countCategoryInTransaction(Transaction transaction, String category) {
        int count = 0;
        for (Product p : transaction.getItems()) {
            if (determineCategory(p.getDescription()).equals(category)) {
                count += p.getQuantity();
            }
        }
        return count;
    }

    private int countProductKeywordInTransaction(Transaction transaction, String keyword) {
        int count = 0;
        for (Product p : transaction.getItems()) {
            if (p.getDescription().toUpperCase().contains(keyword)) {
                count += p.getQuantity();
            }
        }
        return count;
    }

    /**
     * Count beverages excluding Polar Pop (since Polar Pop has its own promotion)
     */
    private int countNonPolarPopBeverages(Transaction transaction) {
        int count = 0;
        for (Product p : transaction.getItems()) {
            String desc = p.getDescription().toUpperCase();
            if (determineCategory(p.getDescription()).equals("BEVERAGE")
                    && !desc.contains("POLAR POP")) {
                count += p.getQuantity();
            }
        }
        return count;
    }

    private double findCheapestInCategory(Transaction transaction, String category) {
        double cheapest = Double.MAX_VALUE;
        for (Product p : transaction.getItems()) {
            if (determineCategory(p.getDescription()).equals(category)) {
                if (p.getPrice() < cheapest) {
                    cheapest = p.getPrice();
                }
            }
        }
        return cheapest == Double.MAX_VALUE ? 0 : cheapest;
    }

    /**
     * Find cheapest beverage excluding Polar Pop
     */
    private double findCheapestNonPolarPopBeverage(Transaction transaction) {
        double cheapest = Double.MAX_VALUE;
        for (Product p : transaction.getItems()) {
            String desc = p.getDescription().toUpperCase();
            if (determineCategory(p.getDescription()).equals("BEVERAGE")
                    && !desc.contains("POLAR POP")) {
                if (p.getPrice() < cheapest) {
                    cheapest = p.getPrice();
                }
            }
        }
        return cheapest == Double.MAX_VALUE ? 0 : cheapest;
    }

    private double calculateCategoryTotal(Transaction transaction, String category) {
        double total = 0;
        for (Product p : transaction.getItems()) {
            if (determineCategory(p.getDescription()).equals(category)) {
                total += p.getLineTotal();
            }
        }
        return total;
    }

    /**
     * Determine product category - matches API categorization
     */
    private String determineCategory(String description) {
        String desc = description.toUpperCase();

        // Beverages (including Polar Pop, energy drinks, sodas, water, etc.)
        if (desc.contains("COKE") || desc.contains("PEPSI") || desc.contains("SPRITE") ||
                desc.contains("MONSTER") || desc.contains("RED BULL") || desc.contains("GATORADE") ||
                desc.contains("WATER") || desc.contains("TEA") || desc.contains("COFFEE") ||
                desc.contains("POLAR POP") || desc.contains("ROCKSTAR") || desc.contains("ENERGY") ||
                desc.contains("SODA") || desc.contains("DRINK")) {
            return "BEVERAGE";
        }

        // Food (hot food items - triggers 5% off)
        if (desc.contains("PIZZA") || desc.contains("HOT DOG") || desc.contains("BURGER") ||
                desc.contains("SANDWICH") || desc.contains("DONUT") || desc.contains("TAQUITO") ||
                desc.contains("CROISSANT") || desc.contains("SAUSAGE") || desc.contains("BREAKFAST") ||
                desc.contains("BURRITO") || desc.contains("WRAP")) {
            return "FOOD";
        }

        // Tobacco
        if (desc.contains("MARLBORO") || desc.contains("CAMEL") || desc.contains("NEWPORT") ||
                desc.contains("CIGAR") || desc.contains("VUSE") || desc.contains("JUUL") ||
                desc.contains("TOBACCO") || desc.contains("CIGARETTE")) {
            return "TOBACCO";
        }

        // Snacks
        if (desc.contains("CHIP") || desc.contains("LAYS") || desc.contains("DORITOS") ||
                desc.contains("CHEETOS") || desc.contains("SNICKERS") || desc.contains("REESE") ||
                desc.contains("CANDY") || desc.contains("GUM") || desc.contains("CHOCOLATE")) {
            return "SNACK";
        }

        return "OTHER";
    }
}