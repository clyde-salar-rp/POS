package org.example.ui.dialogs;

import org.example.service.PromoChecker;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Dialog that shows promotion opportunities and allows user to accept
 */
public class PromoOpportunityDialog extends JDialog {
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);

    private boolean accepted = false;

    public PromoOpportunityDialog(Frame owner, PromoChecker.PromoOpportunity opportunity) {
        super(owner, "Promotion Available!", true);
        setSize(550, 450);
        setLocationRelativeTo(owner);
        setResizable(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(Color.WHITE);

        // Header with icon
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerPanel.setBackground(new Color(245, 250, 255));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 220, 240), 2),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel iconLabel = new JLabel("");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 48));

        JLabel headerLabel = new JLabel(opportunity.getPromoName());
        headerLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerLabel.setForeground(PRIMARY_COLOR);

        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setBackground(new Color(245, 250, 255));
        headerTextPanel.add(headerLabel);

        headerPanel.add(iconLabel);
        headerPanel.add(Box.createHorizontalStrut(15));
        headerPanel.add(headerTextPanel);

        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Description
        JPanel descPanel = new JPanel(new BorderLayout());
        descPanel.setBackground(Color.WHITE);
        descPanel.setBorder(new EmptyBorder(20, 0, 15, 0));

        JLabel descLabel = new JLabel("<html><div style='text-align: center;'>"
                + opportunity.getDescription() + "</div></html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descPanel.add(descLabel, BorderLayout.CENTER);

        // Savings amount
        JPanel savingsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        savingsPanel.setBackground(new Color(232, 245, 233));
        savingsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SUCCESS_COLOR, 2),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel savingsLabel = new JLabel(
                String.format("💰 Potential Savings: $%.2f", opportunity.getPotentialSavings())
        );
        savingsLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        savingsLabel.setForeground(SUCCESS_COLOR);
        savingsPanel.add(savingsLabel);

        savingsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Items needed (if any)
        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(Color.WHITE);
        itemsPanel.setBorder(new EmptyBorder(20, 10, 20, 10));

        if (!opportunity.getItemsNeeded().isEmpty()) {
            JLabel itemsHeaderLabel = new JLabel("Items to be added:");
            itemsHeaderLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            itemsHeaderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            itemsPanel.add(itemsHeaderLabel);
            itemsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

            for (PromoChecker.ItemToAdd item : opportunity.getItemsNeeded()) {
                JLabel itemLabel = new JLabel(String.format(
                        "  • %dx %s ($%.2f each)",
                        item.getQuantity(),
                        item.getProduct().getDescription(),
                        item.getProduct().getPrice()
                ));
                itemLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
                itemLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                itemsPanel.add(itemLabel);
                itemsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            }
        } else {
            // No items needed - already eligible
            JLabel eligibleLabel = new JLabel("You're already eligible for this discount!");
            eligibleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            eligibleLabel.setForeground(SUCCESS_COLOR);
            eligibleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            itemsPanel.add(eligibleLabel);
        }

        // Wrap items panel in a scroll pane for long lists
        JScrollPane itemsScrollPane = new JScrollPane(itemsPanel);
        itemsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        itemsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        itemsScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JButton acceptButton;
        if (!opportunity.getItemsNeeded().isEmpty()) {
            acceptButton = createButton("Add Items & Apply Promo", SUCCESS_COLOR);
        } else {
            acceptButton = createButton("OK - Apply Discount", SUCCESS_COLOR);
        }

        JButton declineButton = createButton("No Thanks", new Color(120, 120, 120));

        acceptButton.addActionListener(e -> {
            accepted = true;
            dispose();
        });

        declineButton.addActionListener(e -> {
            accepted = false;
            dispose();
        });

        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);

        // Assemble
        mainPanel.add(headerPanel);
        mainPanel.add(descPanel);
        mainPanel.add(savingsPanel);
        mainPanel.add(itemsScrollPane);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(buttonPanel);

        add(mainPanel);
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 45));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Show the dialog and return whether user accepted the promotion
     */
    public static boolean showPromoDialog(Frame owner, PromoChecker.PromoOpportunity opportunity) {
        PromoOpportunityDialog dialog = new PromoOpportunityDialog(owner, opportunity);
        dialog.setVisible(true);
        return dialog.isAccepted();
    }
}