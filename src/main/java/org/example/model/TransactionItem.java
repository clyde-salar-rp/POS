package org.example.model;

import lombok.Data;

@Data
public class TransactionItem {
    private final Product product;
    private int quantity;

    public TransactionItem(Product product) {
        this.product = product;
        this.quantity = 1;
    }

    public TransactionItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public double getLineTotal() {
        return product.getPrice() * quantity;
    }

    @Override
    public String toString() {
        return String.format("%dx %s @ $%.2f = $%.2f",
                quantity,
                product.getDescription(),
                product.getPrice(),
                getLineTotal());
    }
}
