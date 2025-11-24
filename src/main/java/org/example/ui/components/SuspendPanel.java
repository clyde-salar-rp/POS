package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class SuspendPanel extends JPanel {
    private static final Color CARD_BG = Color.WHITE;
    private static final Color SUSPEND_COLOR = new Color(255, 152, 0);
    private static final Color RESUME_COLOR = new Color(76, 175, 80);

    public SuspendPanel(Runnable onSuspend, Runnable onResume) {
        setLayout(new GridLayout(1, 2, 12, 0));
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                        "Transaction Control",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 14),
                        new Color(60, 60, 60)
                ),
                new EmptyBorder(15, 15, 15, 15)
        ));

        add(createButton("SUSPEND", SUSPEND_COLOR, onSuspend));
        add(createButton("RESUME", RESUME_COLOR, onResume));
    }

    private JButton createButton(String text, Color bgColor, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(0, 55));

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