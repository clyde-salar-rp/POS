package org.example.ui.components;

import org.example.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Consumer;

public class QuickKeysPanel extends JPanel {
    private static final Color CARD_BG = Color.WHITE;
    private static final Color QUICK_KEY_COLOR = new Color(0, 150, 136); // Teal

    public QuickKeysPanel(Consumer<Product> onQuickKey) {
        setLayout(new GridLayout(3, 3, 12, 12));
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                        "Quick Keys",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 14),
                        new Color(60, 60, 60)
                ),
                new EmptyBorder(15, 15, 15, 15)
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
        JButton button = new JButton(String.format(
                "<html><div style='text-align: center;'><b>%s</b><br>$%.2f</div></html>",
                name,
                price
        ));

        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(QUICK_KEY_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 75));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(QUICK_KEY_COLOR.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(QUICK_KEY_COLOR);
            }
        });

        button.addActionListener(e -> {
            Product product = new Product(upc, name, price);
            action.accept(product);
        });

        add(button);
    }
}