package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class TotalPanel extends JPanel {
    private final JLabel totalLabel;

    public TotalPanel(Runnable onClear) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JButton clearButton = new JButton("Clear Transaction");
        clearButton.addActionListener(e -> onClear.run());

        totalLabel = new JLabel("TOTAL: $0.00");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        totalLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        add(clearButton, BorderLayout.WEST);
        add(totalLabel, BorderLayout.EAST);
    }

    public void updateTotal(double total) {
        totalLabel.setText(String.format("TOTAL: $%.2f", total));
    }
}