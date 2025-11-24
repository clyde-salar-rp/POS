package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private final List<TransactionItem> items;
    private static final double TAX_RATE = 0.07;
    private Integer suspendedId; // Track if this transaction was previously suspended

    public Transaction() {
        this.items = new ArrayList<>();
        this.suspendedId = null;
    }

    public void addItem(Product product) {
        items.add(new TransactionItem(product, 1));
    }

    public void addItem(Product product, int quantity) {
        items.add(new TransactionItem(product, quantity));
    }

    public void voidItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    public void voidLastItem() {
        if (!items.isEmpty()) {
            items.remove(items.size() - 1);
        }
    }

    public void changeQuantity(int index, int newQuantity) {
        if (index >= 0 && index < items.size()) {
            items.get(index).setQuantity(newQuantity);
        }
    }

    public void clear() {
        items.clear();
        suspendedId = null; // Clear the suspended ID when transaction is cleared
    }

    public List<TransactionItem> getItems() {
        return new ArrayList<>(items);
    }

    public TransactionItem getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }

    public double getSubtotal() {
        return items.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
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

    public TransactionItem getLastItem() {
        return items.isEmpty() ? null : items.get(items.size() - 1);
    }

    // Methods for tracking suspended transaction ID
    public Integer getSuspendedId() {
        return suspendedId;
    }

    public void setSuspendedId(Integer suspendedId) {
        this.suspendedId = suspendedId;
    }
}