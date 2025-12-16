package org.example.ui.components;

import org.example.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class QuickKeysPanel extends JPanel {
    private static final Color CARD_BG = Color.WHITE;
    private static final Color QUICK_KEY_COLOR = new Color(0, 150, 136); // Teal
    private Consumer<Product> onQuickKey;

    public QuickKeysPanel(Consumer<Product> onQuickKey) {
        this.onQuickKey = onQuickKey;
        setLayout(new GridLayout(3, 3, 12, 12));
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                        "Quick Keys - Top Sellers",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 14),
                        new Color(60, 60, 60)
                ),
                new EmptyBorder(15, 15, 15, 15)
        ));
    }

    /**
     * Updates the quick keys with the most popular products
     * @param popularProducts List of products sorted by popularity (most popular first)
     */
    public void updateQuickKeys(List<Product> popularProducts) {
        removeAll();

        // Add up to 9 products
        int count = Math.min(9, popularProducts.size());
        for (int i = 0; i < count; i++) {
            Product product = popularProducts.get(i);
            addQuickKey(product.getName(), product.getPrice(), product.getUpc(), onQuickKey);
        }

        // Fill remaining slots with empty panels if less than 9 products
        for (int i = count; i < 9; i++) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(new Color(240, 240, 240));
            emptyPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            add(emptyPanel);
        }

        revalidate();
        repaint();
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