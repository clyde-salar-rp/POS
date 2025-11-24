package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ActionPanel extends JPanel {
    private static final Color CARD_BG = Color.WHITE;
    private static final Color DANGER_COLOR = new Color(244, 67, 54);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color INFO_COLOR = new Color(33, 150, 243);

    public ActionPanel(Runnable onVoidItem, Runnable onQuantityChange,
                       Runnable onExactDollar, Runnable onNextDollar,
                       Runnable onCash, Runnable onCredit, Runnable onClear) {
        setLayout(new GridBagLayout());
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                        "Actions",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 14),
                        new Color(60, 60, 60)
                ),
                new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        add(createButton("VOID ITEM", DANGER_COLOR, onVoidItem), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        add(createButton("CHANGE QTY", WARNING_COLOR, onQuantityChange), gbc);

        // Row 1
        gbc.gridx = 0; gbc.gridy = 1;
        add(createButton("EXACT DOLLAR", INFO_COLOR, onExactDollar), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        add(createButton("NEXT DOLLAR", INFO_COLOR, onNextDollar), gbc);

        // Row 2
        gbc.gridx = 0; gbc.gridy = 2;
        add(createButton("CASH", SUCCESS_COLOR, onCash), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        add(createButton("CREDIT", SUCCESS_COLOR, onCredit), gbc);

        // Row 3 - Clear button spanning 2 columns
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(createButton("VOID TRANSACTION", DANGER_COLOR, onClear), gbc);
    }

    private JButton createButton(String text, Color bgColor, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 50));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        button.addActionListener(e -> action.run());
        return button;
    }
}