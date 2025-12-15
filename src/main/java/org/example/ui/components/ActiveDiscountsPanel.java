package org.example.ui.components;

import org.example.service.DiscountService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Panel that displays currently active discount promotions.
 * Automatically refreshes periodically to show the latest rules.
 */
public class ActiveDiscountsPanel extends JPanel {
    private final DiscountService discountService;
    private final JPanel promotionsPanel;
    private final JLabel statusLabel;
    private final JButton refreshButton;
    private Timer autoRefreshTimer;

    private static final Color PROMO_BG = new Color(255, 248, 220);
    private static final Color PROMO_BORDER = new Color(255, 193, 7);
    private static final int AUTO_REFRESH_INTERVAL = 300000; // 5 minutes

    public ActiveDiscountsPanel(DiscountService discountService) {
        this.discountService = discountService;

        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                new TitledBorder("🎉 Active Promotions"),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Status/header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        statusLabel = new JLabel("Loading promotions...");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        statusLabel.setForeground(Color.GRAY);

        refreshButton = new JButton("🔄 Refresh");
        refreshButton.setFocusable(false);
        refreshButton.addActionListener(e -> refreshDiscounts());

        headerPanel.add(statusLabel, BorderLayout.CENTER);
        headerPanel.add(refreshButton, BorderLayout.EAST);

        // Scrollable promotions panel
        promotionsPanel = new JPanel();
        promotionsPanel.setLayout(new BoxLayout(promotionsPanel, BoxLayout.Y_AXIS));
        promotionsPanel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(promotionsPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.setPreferredSize(new Dimension(300, 200));

        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Initial load
        refreshDiscounts();

        // Start auto-refresh timer
        startAutoRefresh();
    }

    /**
     * Refreshes the discount list from the API
     */
    public void refreshDiscounts() {
        refreshButton.setEnabled(false);
        statusLabel.setText("Refreshing...");

        // Run in background thread to not block UI
        SwingWorker<List<DiscountService.DiscountRuleInfo>, Void> worker =
                new SwingWorker<>() {
                    @Override
                    protected List<DiscountService.DiscountRuleInfo> doInBackground() throws Exception {
                        return discountService.getActiveDiscountRules();
                    }

                    @Override
                    protected void done() {
                        try {
                            List<DiscountService.DiscountRuleInfo> rules = get();
                            updatePromotionsDisplay(rules);
                            statusLabel.setText("Updated: " + getCurrentTime());
                        } catch (Exception e) {
                            // Get the root cause and provide a detailed error message
                            String errorMsg = getDetailedErrorMessage(e);
                            statusLabel.setText("Error loading promotions");
                            showError(errorMsg);

                            // Log to console for debugging
                            System.err.println("❌ Error fetching active discount rules:");
                            e.printStackTrace();
                        } finally {
                            refreshButton.setEnabled(true);
                        }
                    }
                };

        worker.execute();
    }

    /**
     * Extracts a detailed error message from an exception
     */
    private String getDetailedErrorMessage(Exception e) {
        // Unwrap the exception to get the root cause
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        // Build a descriptive error message
        StringBuilder msg = new StringBuilder();

        if (cause instanceof java.net.ConnectException) {
            msg.append("Cannot connect to discount API server.\n");
            msg.append("Please check that the API is running at:\n");
            msg.append(getApiBaseUrl());
        } else if (cause instanceof java.net.UnknownHostException) {
            msg.append("Cannot resolve discount API hostname.\n");
            msg.append("Check your network connection.");
        } else if (cause instanceof java.io.IOException) {
            msg.append("Network error: ");
            msg.append(cause.getMessage() != null ? cause.getMessage() : "Unknown I/O error");
        } else if (cause instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            msg.append("Invalid response from API.\n");
            msg.append("The server returned malformed JSON.");
        } else {
            // Generic error message
            String exceptionName = cause.getClass().getSimpleName();
            String exceptionMsg = cause.getMessage();

            msg.append(exceptionName);
            if (exceptionMsg != null && !exceptionMsg.isEmpty()) {
                msg.append(": ").append(exceptionMsg);
            } else {
                msg.append(" occurred while fetching promotions");
            }
        }

        return msg.toString();
    }

    /**
     * Gets the base API URL from the DiscountService (via reflection if needed)
     */
    private String getApiBaseUrl() {
        // You could make this configurable, but for now return the expected URL
        return "http://localhost:8080";
    }

    /**
     * Updates the UI with the list of active discount rules
     */
    private void updatePromotionsDisplay(List<DiscountService.DiscountRuleInfo> rules) {
        promotionsPanel.removeAll();

        if (rules.isEmpty()) {
            JLabel noPromos = new JLabel("No active promotions at this time");
            noPromos.setForeground(Color.GRAY);
            noPromos.setAlignmentX(Component.LEFT_ALIGNMENT);
            promotionsPanel.add(noPromos);
        } else {
            // Create a mutable copy of the list before sorting
            List<DiscountService.DiscountRuleInfo> sortableRules = new ArrayList<>(rules);

            // Sort by priority (highest first)
            sortableRules.sort((a, b) -> Integer.compare(
                    b.priority != null ? b.priority : 0,
                    a.priority != null ? a.priority : 0
            ));

            for (DiscountService.DiscountRuleInfo rule : sortableRules) {
                promotionsPanel.add(createPromotionCard(rule));
                promotionsPanel.add(Box.createVerticalStrut(8));
            }
        }

        promotionsPanel.revalidate();
        promotionsPanel.repaint();
    }

    /**
     * Creates a visual card for a single promotion
     */
    private JPanel createPromotionCard(DiscountService.DiscountRuleInfo rule) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout(8, 4));
        card.setBackground(PROMO_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PROMO_BORDER, 2),
                new EmptyBorder(8, 10, 8, 10)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Main promotion text
        JLabel titleLabel = new JLabel(rule.getDisplayString());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(new Color(139, 69, 19)); // Brown color

        // Description text
        JLabel descLabel = new JLabel(rule.description);
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descLabel.setForeground(Color.DARK_GRAY);

        // Priority badge (if high priority)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.CENTER);

        if (rule.priority != null && rule.priority >= 90) {
            JLabel priorityBadge = new JLabel("⭐ HOT DEAL");
            priorityBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
            priorityBadge.setForeground(Color.RED);
            topPanel.add(priorityBadge, BorderLayout.EAST);
        }

        card.add(topPanel, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);

        return card;
    }

    /**
     * Starts automatic refresh timer
     */
    private void startAutoRefresh() {
        autoRefreshTimer = new Timer(true);
        autoRefreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> refreshDiscounts());
            }
        }, AUTO_REFRESH_INTERVAL, AUTO_REFRESH_INTERVAL);
    }

    /**
     * Stops automatic refresh (call when closing window)
     */
    public void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.cancel();
        }
    }

    private void showError(String message) {
        promotionsPanel.removeAll();

        // Create a multi-line error display
        JTextArea errorArea = new JTextArea(message);
        errorArea.setEditable(false);
        errorArea.setForeground(Color.RED);
        errorArea.setBackground(promotionsPanel.getBackground());
        errorArea.setFont(new Font("SansSerif", Font.PLAIN, 11));
        errorArea.setWrapStyleWord(true);
        errorArea.setLineWrap(true);
        errorArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        errorArea.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBackground(promotionsPanel.getBackground());
        errorPanel.add(new JLabel("⚠️ "), BorderLayout.WEST);
        errorPanel.add(errorArea, BorderLayout.CENTER);
        errorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        promotionsPanel.add(errorPanel);
        promotionsPanel.revalidate();
        promotionsPanel.repaint();
    }

    private String getCurrentTime() {
        return java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("h:mm a")
        );
    }
}