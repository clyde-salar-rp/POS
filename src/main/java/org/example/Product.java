package org.example;


public class Product {
    private String upc;
    private String description;
    private double price;

    public Product(String upc, String description, double price) {
        this.upc = upc;
        this.description = description;
        this.price = price;
    }

    public String getUpc() {
        return upc;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("UPC: %s, Desc: %s, Price: $%.2f", upc, description, price);
    }
}