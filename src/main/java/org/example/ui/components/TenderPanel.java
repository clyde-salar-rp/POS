package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TenderPanel extends JPanel {
    private static final Color CARD_BG = Color.WHITE;
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color INFO_COLOR = new Color(33, 150, 243);
    private static final Color DANGER_COLOR = new Color(244, 67, 54);

    private JLabel totalLabel;
    private double currentTotal;

    public TenderPanel(Runnable onExactDollar, Runnable onNextDollar,
                       Runnable onCash, Runnable onCredit, Runnable onCancel) {
        setLayout(new BorderLayout(0, 20));
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                        "Payment Options",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 14),
                        new Color(60, 60, 60)
                ),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Total display at top
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        totalPanel.setBackground(CARD_BG);

        JLabel totalLabelPrefix = new JLabel("AMOUNT DUE:");
        totalLabelPrefix.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabelPrefix.setForeground(new Color(70, 70, 70));

        totalLabel = new JLabel("$0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 36));
        totalLabel.setForeground(SUCCESS_COLOR);

        JPanel totalLabelPanel = new JPanel();
        totalLabelPanel.setLayout(new BoxLayout(totalLabelPanel, BoxLayout.Y_AXIS));
        totalLabelPanel.setBackground(CARD_BG);
        totalLabelPanel.add(totalLabelPrefix);
        totalLabelPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        totalLabelPanel.add(totalLabel);

        totalPanel.add(totalLabelPanel);

        // Payment buttons grid
        JPanel buttonsPanel = new JPanel(new GridLayout(5, 1, 0, 12));
        buttonsPanel.setBackground(CARD_BG);

        buttonsPanel.add(createButton("EXACT DOLLAR", INFO_COLOR, onExactDollar));
        buttonsPanel.add(createButton("NEXT DOLLAR UP", INFO_COLOR, onNextDollar));
        buttonsPanel.add(createButton("CASH (ENTER AMOUNT)", SUCCESS_COLOR, onCash));
        buttonsPanel.add(createButton("CREDIT/DEBIT", SUCCESS_COLOR, onCredit));
        buttonsPanel.add(createButton("CANCEL - RETURN TO TRANSACTION", DANGER_COLOR, onCancel));

        add(totalPanel, BorderLayout.NORTH);
        add(buttonsPanel, BorderLayout.CENTER);
    }

    public void updateTotal(double total) {
        this.currentTotal = total;
        totalLabel.setText(String.format("$%.2f", total));
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
        button.setPreferredSize(new Dimension(0, 60));

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