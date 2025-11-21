package org.example.ui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.BiConsumer;

public class ScanPanel extends JPanel {
    private final JTextField scanField;

    public ScanPanel(BiConsumer<String, String> onScan) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel label = new JLabel("UPC:");
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));

        scanField = new JTextField(20);
        scanField.setFont(new Font("Monospaced", Font.PLAIN, 14));
        scanField.addActionListener(e -> {
            String text = scanField.getText().trim();
            if (!text.isEmpty()) {
                onScan.accept(text, "MANUAL");
                scanField.setText("");
            }
        });

        JButton scanButton = new JButton("Scan");
        scanButton.addActionListener(e -> {
            String text = scanField.getText().trim();
            if (!text.isEmpty()) {
                onScan.accept(text, "MANUAL");
                scanField.setText("");
            }
        });

        add(label);
        add(scanField);
        add(scanButton);
    }

    public JTextField getScanField() {
        return scanField;
    }
}