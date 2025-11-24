package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;

public class TotalPanel extends JPanel {
    private final JLabel subtotalLabel;
    private final JLabel taxLabel;
    private final JLabel totalLabel;
    private static final Color CARD_BG = Color.WHITE;
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);

    public TotalPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 250, 255));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 220, 240), 2),
                new EmptyBorder(30, 50, 30, 50)
        ));

        // Left side - labels
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(245, 250, 255));
        leftPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtotalLabel = createLabel("SUBTOTAL", "$0.00", 20, new Color(70, 70, 70), Component.LEFT_ALIGNMENT);
        taxLabel = createLabel("TAX (7%)", "$0.00", 20, new Color(70, 70, 70), Component.LEFT_ALIGNMENT);

        leftPanel.add(subtotalLabel);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        leftPanel.add(taxLabel);

        // Right side - total
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(245, 250, 255));
        rightPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        totalLabel = createLabel("TOTAL", "$0.00", 36, SUCCESS_COLOR, Component.RIGHT_ALIGNMENT);
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(totalLabel);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);
    }

    private JLabel createLabel(String prefix, String value, int fontSize, Color color, float alignment) {
        JLabel label = new JLabel(prefix + ": " + value);
        label.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        label.setForeground(color);
        label.setAlignmentX(alignment);
        return label;
    }

    public void updateTotals(double subtotal, double tax, double total) {
        DecimalFormat formatter = new DecimalFormat("#,##0.00");
        subtotalLabel.setText(String.format("SUBTOTAL: $%s", formatter.format(subtotal)));
        taxLabel.setText(String.format("TAX (7%%): $%s", formatter.format(tax)));
        totalLabel.setText(String.format("TOTAL: $%s", formatter.format(total)));
    }
}