package org.example.ui.dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ReceiptDialog extends JDialog {
    private static final Color PAPER_COLOR = new Color(255, 255, 250);
    private static final Color TEXT_COLOR = new Color(30, 30, 30);

    public ReceiptDialog(Frame owner, String receiptText) {
        super(owner, "Receipt", true);
        setSize(500, 700);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Receipt paper
        JPanel receiptPanel = new JPanel(new BorderLayout());
        receiptPanel.setBackground(PAPER_COLOR);
        receiptPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(20, 30, 20, 30)
        ));

        // Receipt text
        JTextArea receiptArea = new JTextArea(receiptText);
        receiptArea.setFont(new Font("Courier New", Font.PLAIN, 12));
        receiptArea.setForeground(TEXT_COLOR);
        receiptArea.setBackground(PAPER_COLOR);
        receiptArea.setEditable(false);
        receiptArea.setLineWrap(false);
        receiptArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(receiptArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(PAPER_COLOR);
        scrollPane.getViewport().setBackground(PAPER_COLOR);

        receiptPanel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(new Color(240, 240, 240));

        JButton printButton = new JButton("Print Receipt");
        JButton emailButton = new JButton("Email Receipt");
        JButton closeButton = new JButton("Close");

        styleButton(printButton, new Color(25, 118, 210));
        styleButton(emailButton, new Color(76, 175, 80));
        styleButton(closeButton, new Color(120, 120, 120));

        printButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "Receipt sent to printer!",
                    "Print",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        emailButton.addActionListener(e -> {
            String email = JOptionPane.showInputDialog(this,
                    "Enter email address:",
                    "Email Receipt",
                    JOptionPane.PLAIN_MESSAGE);
            if (email != null && !email.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Receipt sent to: " + email,
                        "Email Sent",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(printButton);
        buttonPanel.add(emailButton);
        buttonPanel.add(closeButton);

        mainPanel.add(receiptPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(140, 40));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    public static void showReceipt(Frame owner, String receiptText) {
        ReceiptDialog dialog = new ReceiptDialog(owner, receiptText);
        dialog.setVisible(true);
    }
}