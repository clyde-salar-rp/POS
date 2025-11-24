package org.example.model;

import lombok.Data;

@Data
public class Product {
    private String upc;
    private String description;
    private double price;
    private int quantity;

    // Constructor for database lookups (without quantity)
    public Product(String upc, String description, double price) {
        this.upc = upc;
        this.description = description;
        this.price = price;
        this.quantity = 1; // Default quantity
    }

    // Constructor with quantity
    public Product(String upc, String description, double price, int quantity) {
        this.upc = upc;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
    }

    // Copy constructor for creating new instances
    public Product(Product other) {
        this.upc = other.upc;
        this.description = other.description;
        this.price = other.price;
        this.quantity = other.quantity;
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getLineTotal() {
        return price * quantity;
    }

    @Override
    public String toString() {
        return String.format("%dx %s @ $%.2f = $%.2f",
                quantity, description, price, getLineTotal());
    }

    public double getPriceWithTax() {
        return price * 1.07;
    }

    public double getPriceWithTax(double taxRate) {
        return price * taxRate;
    }
}