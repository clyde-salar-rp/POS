package org.example.ui.dialogs;

import org.example.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Interactive dialog that suggests promotional items to the cashier
 * Shows what items are needed to complete the promo
 */
public class InteractivePromoDialog extends JDialog {
    private boolean accepted = false;
    private static final Color PROMO_COLOR = new Color(255, 152, 0);
    private static final Color ACCEPT_COLOR = new Color(76, 175, 80);
    private static final Color DECLINE_COLOR = new Color(158, 158, 158);

    public InteractivePromoDialog(Frame owner, String promoTitle, String promoMessage,
                                  List<Product> itemsToAdd, int currentCount, int neededCount) {
        super(owner, "Promotional Opportunity", true);

        setSize(500, 350);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Header with promo color
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PROMO_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("🎁 " + promoTitle, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Message
        JLabel messageLabel = new JLabel("<html><center>" + promoMessage + "</center></html>");
        messageLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(messageLabel);

        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Progress indicator
        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressPanel.setBackground(Color.WHITE);

        JLabel progressLabel = new JLabel(String.format("Current: %d / %d items",
                currentCount, neededCount));
        progressLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        progressLabel.setForeground(new Color(60, 60, 60));
        progressPanel.add(progressLabel);

        contentPanel.add(progressPanel);

        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        // Items to add panel
        if (itemsToAdd != null && !itemsToAdd.isEmpty()) {
            JPanel itemsPanel = new JPanel();
            itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
            itemsPanel.setBackground(new Color(245, 245, 250));
            itemsPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                    new EmptyBorder(10, 15, 10, 15)
            ));

            JLabel itemsTitle = new JLabel("Items to be added:");
            itemsTitle.setFont(new Font("SansSerif", Font.BOLD, 13));
            itemsTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            itemsPanel.add(itemsTitle);

            itemsPanel.add(Box.createRigidArea(new Dimension(0, 8)));

            for (Product product : itemsToAdd) {
                JLabel itemLabel = new JLabel(String.format("  • %s ($%.2f)",
                        product.getDescription(), product.getPrice()));
                itemLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
                itemLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                itemsPanel.add(itemLabel);
            }

            contentPanel.add(itemsPanel);
        }

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.WHITE);

        JButton acceptButton = new JButton("Accept & Add Items");
        styleButton(acceptButton, ACCEPT_COLOR);
        acceptButton.addActionListener(e -> {
            accepted = true;
            dispose();
        });

        JButton declineButton = new JButton("No Thanks");
        styleButton(declineButton, DECLINE_COLOR);
        declineButton.addActionListener(e -> {
            accepted = false;
            dispose();
        });

        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(160, 40));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }

    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Show dialog and return whether cashier accepted
     */
    public static boolean showPromo(Frame owner, String title, String message,
                                    List<Product> itemsToAdd, int current, int needed) {
        InteractivePromoDialog dialog = new InteractivePromoDialog(
                owner, title, message, itemsToAdd, current, needed);
        dialog.setVisible(true);
        return dialog.isAccepted();
    }
}