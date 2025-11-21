package org.example.model;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
    private final List<Product> items;
    private double total;

    public Transaction() {
        this.items = new ArrayList<>();
        this.total = 0.0;
    }

    public void addItem(Product product) {
        items.add(product);
        total += product.getPrice();
    }

    public void clear() {
        items.clear();
        total = 0.0;
    }

    public List<Product> getItems() {
        return new ArrayList<>(items);
    }

    public double getTotal() {
        return total;
    }

    public int getItemCount() {
        return items.size();
    }
}