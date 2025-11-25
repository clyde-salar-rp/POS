package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;

public class TenderPanel extends JPanel {
    private static final Color CARD_BG = Color.WHITE;
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color INFO_COLOR = new Color(33, 150, 243);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);

    private final JLabel totalLabel;
    private final DecimalFormat formatter;

    public TenderPanel(Runnable onExactDollar, Runnable onNextDollar,
                       Runnable onCash, Runnable onCredit, Runnable onCancel) {
        setLayout(new BorderLayout(0, 15));
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
                new EmptyBorder(15, 15, 15, 15)
        ));

        formatter = new DecimalFormat("#,##0.00");

        // Total display panel
        JPanel totalPanel = new JPanel();
        totalPanel.setBackground(new Color(245, 250, 255));
        totalPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 220, 240), 2),
                new EmptyBorder(20, 20, 20, 20)
        ));

        totalLabel = new JLabel("TOTAL: $0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        totalLabel.setForeground(new Color(76, 175, 80));
        totalPanel.add(totalLabel);

        // Quick tender buttons
        JPanel quickTenderPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        quickTenderPanel.setBackground(CARD_BG);
        quickTenderPanel.add(createButton("EXACT DOLLAR", INFO_COLOR, onExactDollar));
        quickTenderPanel.add(createButton("NEXT DOLLAR", INFO_COLOR, onNextDollar));

        // Payment method buttons
        JPanel paymentPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        paymentPanel.setBackground(CARD_BG);
        paymentPanel.add(createButton("CASH", SUCCESS_COLOR, onCash));
        paymentPanel.add(createButton("CREDIT", SUCCESS_COLOR, onCredit));

        // Cancel button
        JPanel cancelPanel = new JPanel(new BorderLayout());
        cancelPanel.setBackground(CARD_BG);
        cancelPanel.add(createButton("CANCEL PAYMENT", WARNING_COLOR, onCancel));

        // Main content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(CARD_BG);

        // Set max sizes for consistent layout
        totalPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        quickTenderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        paymentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        cancelPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        contentPanel.add(totalPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(quickTenderPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        contentPanel.add(paymentPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(cancelPanel);

        add(contentPanel, BorderLayout.NORTH);
    }

    public void updateTotal(double total) {
        totalLabel.setText(String.format("TOTAL: $%s", formatter.format(total)));
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