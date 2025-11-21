package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TotalPanel extends JPanel {
    private final JLabel subtotalLabel;
    private final JLabel taxLabel;
    private final JLabel totalLabel;

    public TotalPanel(Runnable onClear) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JButton clearButton = new JButton("Clear Transaction");
        clearButton.setFont(new Font("SansSerif", Font.PLAIN, 14));
        clearButton.addActionListener(e -> onClear.run());

        JPanel totalsPanel = new JPanel();
        totalsPanel.setLayout(new BoxLayout(totalsPanel, BoxLayout.Y_AXIS));
        totalsPanel.setBackground(Color.WHITE);

        subtotalLabel = new JLabel("SUBTOTAL: $0.00");
        subtotalLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        subtotalLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        taxLabel = new JLabel("TAX (7%): $0.00");
        taxLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        taxLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        totalLabel = new JLabel("TOTAL: $0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        totalLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        totalsPanel.add(subtotalLabel);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        totalsPanel.add(taxLabel);
        totalsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        totalsPanel.add(totalLabel);

        add(clearButton, BorderLayout.WEST);
        add(totalsPanel, BorderLayout.EAST);
    }

    public void updateTotals(double subtotal, double tax, double total) {
        subtotalLabel.setText(String.format("SUBTOTAL: $%.2f", subtotal));
        taxLabel.setText(String.format("TAX (7%%): $%.2f", tax));
        totalLabel.setText(String.format("TOTAL: $%.2f", total));
    }
}