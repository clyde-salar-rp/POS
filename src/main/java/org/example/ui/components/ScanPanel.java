package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.BiConsumer;

public class ScanPanel extends JPanel {
    private final JTextField scanField;
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color CARD_BG = Color.WHITE;

    public ScanPanel(BiConsumer<String, String> onScan) {
        setLayout(new BorderLayout(15, 0));
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                new EmptyBorder(20, 25, 20, 25)
        ));

        // Label
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelPanel.setBackground(CARD_BG);

        JLabel textLabel = new JLabel("SCAN OR ENTER UPC");
        textLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        textLabel.setForeground(new Color(60, 60, 60));

        labelPanel.add(textLabel);

        // Input field
        scanField = new JTextField(30);
        scanField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        scanField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        scanField.addActionListener(e -> {
            String text = scanField.getText().trim();
            if (!text.isEmpty()) {
                onScan.accept(text, "MANUAL");
                scanField.setText("");
            }
        });

        // Scan button
        JButton scanButton = new JButton("SCAN");
        scanButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        scanButton.setForeground(Color.WHITE);
        scanButton.setBackground(PRIMARY_COLOR);
        scanButton.setFocusPainted(false);
        scanButton.setBorderPainted(false);
        scanButton.setOpaque(true);
        scanButton.setPreferredSize(new Dimension(100, 42));
        scanButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        scanButton.addActionListener(e -> {
            String text = scanField.getText().trim();
            if (!text.isEmpty()) {
                onScan.accept(text, "MANUAL");
                scanField.setText("");
            }
        });

        // Center panel for field and button
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        centerPanel.setBackground(CARD_BG);
        centerPanel.add(scanField);
        centerPanel.add(scanButton);

        add(labelPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
    }

    public JTextField getScanField() {
        return scanField;
    }
}