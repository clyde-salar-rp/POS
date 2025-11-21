package org.example.ui.components;

import org.example.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.function.Consumer;

public class QuickKeysPanel extends JPanel {

    public QuickKeysPanel(Consumer<Product> onQuickKey) {
        setLayout(new GridLayout(3, 3, 8, 8));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("Quick Keys"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Popular items based on pricebook
        addQuickKey("Hot Dog", 2.69, "999999955678", onQuickKey);
        addQuickKey("Coffee Med", 2.09, "999991218955", onQuickKey);
        addQuickKey("Polar Pop M", 0.89, "999999937551", onQuickKey);
        addQuickKey("Donut", 2.49, "049000000443", onQuickKey);
        addQuickKey("Monster", 3.29, "070847811169", onQuickKey);
        addQuickKey("Red Bull", 3.79, "611269818994", onQuickKey);
        addQuickKey("Coke 20oz", 2.69, "012000001291", onQuickKey);
        addQuickKey("Water 16oz", 1.35, "194283301326", onQuickKey);
        addQuickKey("Snickers", 3.19, "040000002635", onQuickKey);
    }

    private void addQuickKey(String name, double price, String upc, Consumer<Product> action) {
        JButton button = new JButton("<html><center>" + name + "<br>$" +
                String.format("%.2f", price) + "</center></html>");
        button.setFont(new Font("SansSerif", Font.PLAIN, 11));
        button.setFocusable(false);
        button.addActionListener(e -> {
            Product product = new Product(upc, name, price);
            action.accept(product);
        });
        add(button);
    }
}