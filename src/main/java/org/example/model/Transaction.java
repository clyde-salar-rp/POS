package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private final List<TransactionItem> items;
    private static final double TAX_RATE = 0.07;

    public Transaction() {
        this.items = new ArrayList<>();
    }

    public void addItem(Product product) {
        items.add(new TransactionItem(product, 1));
    }

    public void addItem(Product product, int quantity) {
        items.add(new TransactionItem(product, quantity));
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
    }

    public List<TransactionItem> getItems() {
        return new ArrayList<>(items);
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
}