package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Product {
    private String upc;
    private String description;
    private double price;

    @Override
    public String toString() {
        return String.format("UPC: %s, Desc: %s, Price: $%.2f", upc, description, price);
    }
}
