package org.example.ui;

import org.example.model.Product;
import org.example.model.Transaction;
import org.example.service.DiscountService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Customer-facing display with INTERACTIVE promotional offers
 * Customers can click Accept/Decline buttons directly on this display
 */
public class CustomerDisplay extends JFrame {
    private final JLabel totalLabel;
    private final JTextArea itemsArea;
    private final JPanel promoPanel;
    private final JLabel promoMessageLabel;
    private final DecimalFormat moneyFormat;
    private Timer promoTimer;

    // Interactive promo components
    private final JPanel interactivePromoPanel;
    private final JLabel promoTitleLabel;
    private final JLabel promoDetailsLabel;
    private final JButton acceptButton;
    private final JButton declineButton;
    private Consumer<Boolean> currentPromoCallback;

    // Dynamic promo loading
    private final DiscountService discountService;
    private List<String> cachedPromoMessages;
    private Timer promoRefreshTimer;

    private static final Color DISPLAY_BG = new Color(20, 20, 20);
    private static final Color TEXT_COLOR = new Color(0, 255, 100);
    private static final Color TOTAL_COLOR = new Color(255, 255, 0);
    private static final Color PROMO_BG = new Color(255, 152, 0);
    private static final Color ACCEPT_COLOR = new Color(76, 175, 80);
    private static final Color DECLINE_COLOR = new Color(244, 67, 54);
    private static final Color INTERACTIVE_BG = new Color(33, 150, 243);

    private static final Font MAIN_FONT = new Font("Courier New", Font.BOLD, 16);
    private static final Font TOTAL_FONT = new Font("Courier New", Font.BOLD, 36);
    private static final Font PROMO_FONT = new Font("SansSerif", Font.BOLD, 14);
    private static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 16);
    private static final Font PROMO_TITLE_FONT = new Font("SansSerif", Font.BOLD, 18);

    private static final int PROMO_REFRESH_INTERVAL = 300000; // 5 minutes

    public CustomerDisplay(DiscountService discountService) {
        this.discountService = discountService;
        this.moneyFormat = new DecimalFormat("#,##0.00");
        this.cachedPromoMessages = new ArrayList<>();

        setTitle("Customer Display");
        setSize(320, 700);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(DISPLAY_BG);
        setLayout(new BorderLayout(0, 0));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(DISPLAY_BG);
        headerPanel.setBorder(new EmptyBorder(10, 10, 5, 10));

        JLabel welcomeLabel = new JLabel("CLYDE'S STORE");
        welcomeLabel.setFont(HEADER_FONT);
        welcomeLabel.setForeground(TEXT_COLOR);
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(welcomeLabel);

        // Items display area
        itemsArea = new JTextArea();
        itemsArea.setFont(MAIN_FONT);
        itemsArea.setForeground(TEXT_COLOR);
        itemsArea.setBackground(DISPLAY_BG);
        itemsArea.setEditable(false);
        itemsArea.setLineWrap(false);
        itemsArea.setText("Ready...");
        itemsArea.setMargin(new Insets(5, 5, 5, 5));

        JScrollPane itemsScroll = new JScrollPane(itemsArea);
        itemsScroll.setBorder(BorderFactory.createEmptyBorder());
        itemsScroll.setBackground(DISPLAY_BG);
        itemsScroll.getViewport().setBackground(DISPLAY_BG);

        // Simple promo banner (non-interactive)
        promoPanel = new JPanel(new BorderLayout());
        promoPanel.setBackground(PROMO_BG);
        promoPanel.setBorder(new EmptyBorder(10, 8, 10, 8));
        promoPanel.setPreferredSize(new Dimension(320, 60));
        promoPanel.setVisible(false);
        promoPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        promoMessageLabel = new JLabel("", SwingConstants.CENTER);
        promoMessageLabel.setFont(PROMO_FONT);
        promoMessageLabel.setForeground(Color.WHITE);
        promoPanel.add(promoMessageLabel, BorderLayout.CENTER);

        // Make the banner clickable
        promoPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                handlePromoBannerClick();
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                promoPanel.setBackground(PROMO_BG.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                promoPanel.setBackground(PROMO_BG);
            }
        });

        // INTERACTIVE PROMO PANEL (with buttons)
        interactivePromoPanel = new JPanel();
        interactivePromoPanel.setLayout(new BoxLayout(interactivePromoPanel, BoxLayout.Y_AXIS));
        interactivePromoPanel.setBackground(INTERACTIVE_BG);
        interactivePromoPanel.setBorder(new EmptyBorder(15, 10, 15, 10));
        interactivePromoPanel.setVisible(false);

        promoTitleLabel = new JLabel("", SwingConstants.CENTER);
        promoTitleLabel.setFont(PROMO_TITLE_FONT);
        promoTitleLabel.setForeground(Color.WHITE);
        promoTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        interactivePromoPanel.add(promoTitleLabel);

        interactivePromoPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        promoDetailsLabel = new JLabel("", SwingConstants.CENTER);
        promoDetailsLabel.setFont(PROMO_FONT);
        promoDetailsLabel.setForeground(Color.WHITE);
        promoDetailsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        interactivePromoPanel.add(promoDetailsLabel);

        interactivePromoPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setMaximumSize(new Dimension(280, 50));
        buttonPanel.setBackground(INTERACTIVE_BG);

        acceptButton = new JButton("YES");
        acceptButton.setFont(BUTTON_FONT);
        acceptButton.setForeground(Color.WHITE);
        acceptButton.setBackground(ACCEPT_COLOR);
        acceptButton.setFocusPainted(false);
        acceptButton.setBorderPainted(false);
        acceptButton.setOpaque(true);
        acceptButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        declineButton = new JButton("NO");
        declineButton.setFont(BUTTON_FONT);
        declineButton.setForeground(Color.WHITE);
        declineButton.setBackground(DECLINE_COLOR);
        declineButton.setFocusPainted(false);
        declineButton.setBorderPainted(false);
        declineButton.setOpaque(true);
        declineButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(acceptButton);
        buttonPanel.add(declineButton);

        interactivePromoPanel.add(buttonPanel);

        // Total panel
        JPanel totalPanel = new JPanel();
        totalPanel.setBackground(new Color(30, 30, 30));
        totalPanel.setBorder(new EmptyBorder(15, 10, 15, 10));
        totalPanel.setLayout(new BorderLayout());

        totalLabel = new JLabel("$0.00", SwingConstants.CENTER);
        totalLabel.setFont(TOTAL_FONT);
        totalLabel.setForeground(TOTAL_COLOR);
        totalPanel.add(totalLabel, BorderLayout.CENTER);

        // Center panel - stack items, simple promo, interactive promo
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(DISPLAY_BG);

        JPanel promoStack = new JPanel();
        promoStack.setLayout(new BoxLayout(promoStack, BoxLayout.Y_AXIS));
        promoStack.setBackground(DISPLAY_BG);
        promoStack.add(promoPanel);
        promoStack.add(interactivePromoPanel);

        centerPanel.add(itemsScroll, BorderLayout.CENTER);
        centerPanel.add(promoStack, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(totalPanel, BorderLayout.SOUTH);

        // Load initial promotions
        refreshPromotions();

        // Start auto-refresh timer
        startPromoRefreshTimer();

        positionOnSecondaryMonitor();
        setVisible(true);

        System.out.println("✓ Customer Display initialized (interactive mode)");
    }

    /**
     * Start timer to refresh promotions periodically
     */
    private void startPromoRefreshTimer() {
        promoRefreshTimer = new Timer(PROMO_REFRESH_INTERVAL, e -> refreshPromotions());
        promoRefreshTimer.start();
    }

    /**
     * Fetch active promotions from the API
     */
    private void refreshPromotions() {
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                List<DiscountService.DiscountRuleInfo> rules = discountService.getActiveDiscountRules();
                List<String> messages = new ArrayList<>();

                // Sort by priority (highest first)
                List<DiscountService.DiscountRuleInfo> sortedRules = new ArrayList<>(rules);
                sortedRules.sort((a, b) -> Integer.compare(
                        b.priority != null ? b.priority : 0,
                        a.priority != null ? a.priority : 0
                ));

                // Convert to display messages
                for (DiscountService.DiscountRuleInfo rule : sortedRules) {
                    messages.add(formatPromoForDisplay(rule));
                }

                return messages;
            }

            @Override
            protected void done() {
                try {
                    List<String> messages = get();
                    cachedPromoMessages = messages;
                    System.out.println("✓ Customer Display: Loaded " + messages.size() + " promotions");

                    // Refresh the display if we're on the attract screen
                    if (itemsArea.getText().contains("Loading deals")) {
                        showAttractScreen();
                    }
                } catch (Exception e) {
                    System.err.println("⚠ Customer Display: Failed to load promotions - " + e.getMessage());
                    cachedPromoMessages = getFallbackPromotions();

                    if (itemsArea.getText().contains("Loading deals")) {
                        showAttractScreen();
                    }
                }
            }
        };

        worker.execute();
    }

    /**
     * Format a discount rule for the customer display
     */
    private String formatPromoForDisplay(DiscountService.DiscountRuleInfo rule) {
        switch (rule.ruleType) {
            case "PERCENT_OFF":
                if (rule.category != null && !rule.category.isEmpty()) {
                    return String.format("%.0f%% off %s", rule.percentOff, rule.category);
                } else {
                    return String.format("%.0f%% off", rule.percentOff);
                }

            case "BUY_ONE_GET_ONE":
                return "BOGO " + (rule.category != null ? rule.category : "Items");

            case "BUY_X_GET_Y":
                if (rule.itemKeyword != null) {
                    return String.format("Buy %d Get %d %s", rule.buyQuantity, rule.freeQuantity, rule.itemKeyword);
                } else {
                    return String.format("Buy %d Get %d", rule.buyQuantity, rule.freeQuantity);
                }

            case "MIX_AND_MATCH":
                return String.format("%d for $%.2f", rule.requiredQuantity, rule.bundlePrice);

            default:
                return rule.description;
        }
    }

    /**
     * Get fallback promotions if API is unavailable
     */
    private List<String> getFallbackPromotions() {
        List<String> fallback = new ArrayList<>();
        fallback.add("BOGO Beverages");
        fallback.add("5% off Food");
        fallback.add("Buy 2 Get 1 POLAR POP");
        return fallback;
    }

    /**
     * Stop the promo refresh timer (call on shutdown)
     */
    public void stopPromoRefresh() {
        if (promoRefreshTimer != null) {
            promoRefreshTimer.stop();
        }
    }

    private void positionOnSecondaryMonitor() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();

        if (screens.length > 1) {
            GraphicsDevice secondScreen = screens[1];
            Rectangle bounds = secondScreen.getDefaultConfiguration().getBounds();
            setLocation(bounds.x, bounds.y);
            System.out.println("✓ Customer Display on secondary monitor");
        } else {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            setLocation(screenSize.width - getWidth(), 100);
            System.out.println("⚠ Customer Display positioned at right edge");
        }
    }

    /**
     * Show CLICKABLE promo banner that customer can tap to accept
     */
    public void showClickablePromo(String message, Consumer<Boolean> callback) {
        System.out.println("📢 CLICKABLE PROMO: " + message);

        interactivePromoPanel.setVisible(false);

        if (promoTimer != null && promoTimer.isRunning()) {
            promoTimer.stop();
        }

        currentPromoCallback = callback;

        promoMessageLabel.setText("<html><center>👆 TAP HERE 👆<br>" + message + "</center></html>");
        promoPanel.setVisible(true);
        promoPanel.revalidate();
        promoPanel.repaint();

        // Auto-decline after 15 seconds if not clicked
        promoTimer = new Timer(15000, e -> {
            if (currentPromoCallback != null) {
                System.out.println("📢 Promo timed out - auto-declining");
                currentPromoCallback.accept(false);
                currentPromoCallback = null;
            }
            hidePromo();
        });
        promoTimer.setRepeats(false);
        promoTimer.start();
    }

    /**
     * Handle when customer clicks the promo banner
     */
    private void handlePromoBannerClick() {
        System.out.println("📢 Customer CLICKED promo banner");

        promoPanel.setVisible(false);

        if (currentPromoCallback != null) {
            currentPromoCallback.accept(true);
            showConfirmationMessage("✓ Promo Added!");
            currentPromoCallback = null;
        }
    }

    /**
     * Show a quick confirmation message (non-clickable)
     */
    private void showConfirmationMessage(String message) {
        JLabel confirmLabel = new JLabel(message, SwingConstants.CENTER);
        confirmLabel.setFont(PROMO_FONT);
        confirmLabel.setForeground(Color.WHITE);

        JPanel confirmPanel = new JPanel(new BorderLayout());
        confirmPanel.setBackground(ACCEPT_COLOR);
        confirmPanel.setBorder(new EmptyBorder(10, 8, 10, 8));
        confirmPanel.add(confirmLabel);

        Container parent = promoPanel.getParent();
        parent.remove(promoPanel);
        parent.add(confirmPanel, 0);
        parent.revalidate();
        parent.repaint();

        Timer timer = new Timer(2000, e -> {
            parent.remove(confirmPanel);
            parent.add(promoPanel, 0);
            parent.revalidate();
            parent.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void updateTransaction(Transaction transaction) {
        if (transaction == null || transaction.getItemCount() == 0) {
            itemsArea.setText("\n  Ready to scan...");
            totalLabel.setText("$0.00");
            hidePromo();
            return;
        }

        StringBuilder display = new StringBuilder();
        List<Product> items = transaction.getItems();

        int startIndex = Math.max(0, items.size() - 8);

        if (startIndex > 0) {
            display.append(String.format(" +%d more\n", startIndex));
            display.append(" ─────────────\n");
        }

        for (int i = startIndex; i < items.size(); i++) {
            Product product = items.get(i);
            String desc = truncate(product.getDescription(), 15);

            display.append(String.format(" %dx %s\n",
                    product.getQuantity(),
                    desc
            ));
            display.append(String.format("    $%s\n",
                    moneyFormat.format(product.getLineTotal())
            ));
        }

        itemsArea.setText(display.toString());
        totalLabel.setText(String.format("$%s", moneyFormat.format(transaction.getTotal())));
        itemsArea.setCaretPosition(itemsArea.getDocument().getLength());
    }

    public void updateWithDiscount(Transaction transaction,
                                   DiscountService.DiscountResponse discountInfo) {
        updateTransaction(transaction);

        if (discountInfo != null && discountInfo.totalDiscount > 0) {
            showPromo(String.format("SAVED $%s!", moneyFormat.format(discountInfo.totalDiscount)), 0);

            totalLabel.setText(String.format("<html><center>$%s<br><font size=4>(Saved $%s)</font></center></html>",
                    moneyFormat.format(discountInfo.total),
                    moneyFormat.format(discountInfo.totalDiscount)
            ));
        } else {
            hidePromo();
            totalLabel.setText(String.format("$%s", moneyFormat.format(transaction.getTotal())));
        }
    }

    public void showPromo(String message) {
        showPromo(message, 5000);
    }

    public void showPromo(String message, int durationMs) {
        System.out.println("📢 Customer: " + message);

        interactivePromoPanel.setVisible(false);

        if (promoTimer != null && promoTimer.isRunning()) {
            promoTimer.stop();
        }

        promoMessageLabel.setText("<html><center>" + message + "</center></html>");
        promoPanel.setVisible(true);
        promoPanel.revalidate();
        promoPanel.repaint();

        if (durationMs > 0) {
            promoTimer = new Timer(durationMs, e -> hidePromo());
            promoTimer.setRepeats(false);
            promoTimer.start();
        }
    }

    public void hidePromo() {
        if (promoTimer != null && promoTimer.isRunning()) {
            promoTimer.stop();
        }
        promoPanel.setVisible(false);
        interactivePromoPanel.setVisible(false);
        promoPanel.revalidate();
        promoPanel.repaint();
    }

    public void showAttractScreen() {
        StringBuilder attractText = new StringBuilder();
        attractText.append("\n\n");
        attractText.append("  🛒 WELCOME! 🛒\n\n");
        attractText.append("  Scan items\n\n");

        if (!cachedPromoMessages.isEmpty()) {
            attractText.append("  TODAY'S DEALS:\n");
            for (String promo : cachedPromoMessages) {
                attractText.append("  • ").append(promo).append("\n");
            }
            attractText.append("\n");
        } else {
            attractText.append("  Loading deals...\n\n");
        }

        itemsArea.setText(attractText.toString());
        totalLabel.setText("$0.00");
        hidePromo();
    }

    public void showThankYou(double total, double saved) {
        if (saved > 0) {
            itemsArea.setText(String.format(
                    "\n\n" +
                            "  ✓ COMPLETE ✓\n\n" +
                            "  THANK YOU!\n\n" +
                            "  Total: $%s\n" +
                            "  Saved: $%s\n\n" +
                            "  Have a great\n" +
                            "  day!",
                    moneyFormat.format(total),
                    moneyFormat.format(saved)
            ));

            showPromo("YOU SAVED $" + moneyFormat.format(saved) + "!", 3000);
        } else {
            itemsArea.setText(
                    "\n\n" +
                            "  ✓ COMPLETE ✓\n\n" +
                            "  THANK YOU!\n\n" +
                            "  Have a great\n" +
                            "  day!"
            );
        }

        totalLabel.setText("$" + moneyFormat.format(total));

        Timer timer = new Timer(5000, e -> showAttractScreen());
        timer.setRepeats(false);
        timer.start();
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 2) + "..";
    }
}