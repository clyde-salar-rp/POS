package org.example.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private final List<Product> items;
    private static final double TAX_RATE = 0.07;
    @Getter
    @Setter
    private Integer suspendedId;

    public Transaction() {
        this.items = new ArrayList<>();
        this.suspendedId = null;
    }

    public void addItem(Product product) {
        addItem(product, 1);
    }

    public void addItem(Product product, int quantity) {
        // Check if product already exists in transaction
        for (Product existingProduct : items) {
            if (existingProduct.getUpc().equals(product.getUpc())) {
                // Product exists, increment quantity
                existingProduct.setQuantity(existingProduct.getQuantity() + quantity);
                return;
            }
        }

        // Product doesn't exist, add new copy
        Product newProduct = new Product(product);
        newProduct.setQuantity(quantity);
        items.add(newProduct);
    }

    public void voidItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void changeQuantity(int index, int newQuantity) {
        if (index >= 0 && index < items.size()) {
            items.get(index).setQuantity(newQuantity);
        }
    }

    public void clear() {
        items.clear();
        suspendedId = null;
    }

    public List<Product> getItems() {
        return new ArrayList<>(items);
    }

    public Product getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    public double getSubtotal() {
        return items.stream()
                .mapToDouble(Product::getLineTotal)
                .sum();
    }

    public double getTax() {
        return getSubtotal() * TAX_RATE;
    }

    public double getTotal() {
        return getSubtotal() + getTax();
    }

    public int getItemCount() {
        return items.size();
    }
}