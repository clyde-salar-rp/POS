package org.example.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.DiscountRuleDTO;
import org.example.service.DiscountService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

/**
 * Discount Rule Manager - CRUD interface for managing discount rules via API
 * Themed to match RegisterWindow
 */
public class DiscountRuleManager extends JFrame {
    // Theme colors matching RegisterWindow
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210);
    private static final Color ACCENT_COLOR = new Color(245, 245, 250);
    private static final Color SUCCESS_COLOR = new Color(76, 175, 80);
    private static final Color DANGER_COLOR = new Color(232, 17, 35);
    private static final Color WARNING_COLOR = new Color(255, 152, 0);

    // Change this to match your API endpoint
    private static final String BASE_API_URL = "http://localhost:8080";
    private static final String RULES_ENDPOINT = BASE_API_URL + "/api/discount-rules";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private JTable rulesTable;
    private DefaultTableModel tableModel;

    public DiscountRuleManager() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        // Configure to ignore unknown properties (like createdAt, updatedAt from API)
        this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        setupUI();
        loadRules();
    }

    private void setupUI() {
        setTitle("Discount Rule Manager - POS System");
        setSize(1400, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(ACCENT_COLOR);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(ACCENT_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header with modern styling
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel("Discount Rule Manager");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton refreshBtn = createButton("Refresh", SUCCESS_COLOR);
        refreshBtn.addActionListener(e -> loadRules());
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Table with modern styling
        String[] columns = {"ID", "Type", "Name", "Category", "Value", "Priority", "Active"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        rulesTable = new JTable(tableModel);
        rulesTable.setFont(new Font("SansSerif", Font.PLAIN, 14));
        rulesTable.setRowHeight(40);
        rulesTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        rulesTable.getTableHeader().setBackground(PRIMARY_COLOR);
        rulesTable.getTableHeader().setForeground(Color.WHITE);
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rulesTable.setSelectionBackground(new Color(173, 216, 230));
        rulesTable.setSelectionForeground(Color.BLACK);
        rulesTable.setShowGrid(true);
        rulesTable.setGridColor(new Color(220, 220, 220));

        JScrollPane scrollPane = new JScrollPane(rulesTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(10, 0, 10, 0),
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1)
        ));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Button Panel with modern styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonPanel.setBackground(ACCENT_COLOR);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton createBtn = createButton("Create New Rule", SUCCESS_COLOR);
        createBtn.addActionListener(e -> showCreateDialog());
        buttonPanel.add(createBtn);

        JButton editBtn = createButton("Edit Selected", PRIMARY_COLOR);
        editBtn.addActionListener(e -> editSelectedRule());
        buttonPanel.add(editBtn);

        JButton toggleBtn = createButton("Toggle Active", WARNING_COLOR);
        toggleBtn.addActionListener(e -> toggleSelectedRule());
        buttonPanel.add(toggleBtn);

        JButton deleteBtn = createButton("Delete Selected", DANGER_COLOR);
        deleteBtn.addActionListener(e -> deleteSelectedRule());
        buttonPanel.add(deleteBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(180, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);

        // Add hover effect
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor.darker());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bgColor);
            }
        });

        return btn;
    }

    private void loadRules() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RULES_ENDPOINT))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                DiscountRuleDTO[] rules = objectMapper.readValue(
                        response.body(),
                        DiscountRuleDTO[].class
                );

                tableModel.setRowCount(0);
                for (DiscountRuleDTO rule : rules) {
                    tableModel.addRow(new Object[]{
                            rule.getId(),
                            rule.getRuleType(),
                            rule.getName() != null ? rule.getName() : "N/A",
                            rule.getCategory() != null ? rule.getCategory() : "N/A",
                            formatValue(rule),
                            rule.getPriority(),
                            rule.isActive() ? "Yes" : "No"
                    });
                }

                showMessage("Loaded " + rules.length + " discount rules successfully", "Success");
            } else {
                showError("Failed to load rules: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            showError("Error loading rules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String formatValue(DiscountRuleDTO rule) {
        switch (rule.getRuleType()) {
            case "PERCENT_OFF":
                return rule.getPercentOff() + "%";
            case "BUY_ONE_GET_ONE":
                return "BOGO";
            case "BUY_X_GET_Y":
                return "Buy " + rule.getBuyQuantity() + " Get " + rule.getFreeQuantity();
            case "MIX_AND_MATCH":
                return rule.getRequiredQuantity() + " for $" + String.format("%.2f", rule.getBundlePrice());
            default:
                return "N/A";
        }
    }

    private void showCreateDialog() {
        JDialog dialog = new JDialog(this, "Create New Discount Rule", true);
        dialog.setSize(700, 800);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        Font labelFont = new Font("SansSerif", Font.BOLD, 13);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 13);

        // Rule Type
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel typeLabel = new JLabel("Rule Type:");
        typeLabel.setFont(labelFont);
        panel.add(typeLabel, gbc);
        gbc.gridx = 1;
        String[] ruleTypes = {"PERCENT_OFF", "BUY_ONE_GET_ONE", "BUY_X_GET_Y", "MIX_AND_MATCH"};
        JComboBox<String> typeCombo = new JComboBox<>(ruleTypes);
        typeCombo.setFont(fieldFont);
        panel.add(typeCombo, gbc);

        // Name
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(labelFont);
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField();
        nameField.setFont(fieldFont);
        panel.add(nameField, gbc);

        // Description
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(labelFont);
        panel.add(descLabel, gbc);
        gbc.gridx = 1;
        JTextField descField = new JTextField();
        descField.setFont(fieldFont);
        panel.add(descField, gbc);

        // Category
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel catLabel = new JLabel("Category (optional):");
        catLabel.setFont(labelFont);
        panel.add(catLabel, gbc);
        gbc.gridx = 1;
        String[] categories = {"", "BEVERAGE", "FOOD", "TOBACCO", "GENERAL"};
        JComboBox<String> categoryCombo = new JComboBox<>(categories);
        categoryCombo.setFont(fieldFont);
        panel.add(categoryCombo, gbc);

        // Item Keyword
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel keywordLabel = new JLabel("Item Keyword:");
        keywordLabel.setFont(labelFont);
        panel.add(keywordLabel, gbc);
        gbc.gridx = 1;
        JTextField keywordField = new JTextField();
        keywordField.setFont(fieldFont);
        panel.add(keywordField, gbc);

        // Percent Off
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel percentLabel = new JLabel("Percent Off:");
        percentLabel.setFont(labelFont);
        panel.add(percentLabel, gbc);
        gbc.gridx = 1;
        JSpinner percentSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(0.0), Double.valueOf(0.0), Double.valueOf(100.0), Double.valueOf(5.0)));
        percentSpinner.setFont(fieldFont);
        panel.add(percentSpinner, gbc);

        // Buy Quantity
        gbc.gridx = 0; gbc.gridy = 6;
        JLabel buyQtyLabel = new JLabel("Buy Quantity:");
        buyQtyLabel.setFont(labelFont);
        panel.add(buyQtyLabel, gbc);
        gbc.gridx = 1;
        JSpinner buyQtySpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), Integer.valueOf(100), Integer.valueOf(1)));
        buyQtySpinner.setFont(fieldFont);
        panel.add(buyQtySpinner, gbc);

        // Free Quantity
        gbc.gridx = 0; gbc.gridy = 7;
        JLabel freeQtyLabel = new JLabel("Free Quantity:");
        freeQtyLabel.setFont(labelFont);
        panel.add(freeQtyLabel, gbc);
        gbc.gridx = 1;
        JSpinner freeQtySpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(100), Integer.valueOf(1)));
        freeQtySpinner.setFont(fieldFont);
        panel.add(freeQtySpinner, gbc);

        // Required Quantity
        gbc.gridx = 0; gbc.gridy = 8;
        JLabel reqQtyLabel = new JLabel("Required Quantity:");
        reqQtyLabel.setFont(labelFont);
        panel.add(reqQtyLabel, gbc);
        gbc.gridx = 1;
        JSpinner reqQtySpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), Integer.valueOf(100), Integer.valueOf(1)));
        reqQtySpinner.setFont(fieldFont);
        panel.add(reqQtySpinner, gbc);

        // Bundle Price
        gbc.gridx = 0; gbc.gridy = 9;
        JLabel bundlePriceLabel = new JLabel("Bundle Price $:");
        bundlePriceLabel.setFont(labelFont);
        panel.add(bundlePriceLabel, gbc);
        gbc.gridx = 1;
        JSpinner bundlePriceSpinner = new JSpinner(new SpinnerNumberModel(Double.valueOf(0.0), Double.valueOf(0.0), Double.valueOf(1000.0), Double.valueOf(0.50)));
        bundlePriceSpinner.setFont(fieldFont);
        panel.add(bundlePriceSpinner, gbc);

        // Priority
        gbc.gridx = 0; gbc.gridy = 10;
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(labelFont);
        panel.add(priorityLabel, gbc);
        gbc.gridx = 1;
        JSpinner prioritySpinner = new JSpinner(new SpinnerNumberModel(Integer.valueOf(1), Integer.valueOf(1), Integer.valueOf(100), Integer.valueOf(1)));
        prioritySpinner.setFont(fieldFont);
        panel.add(prioritySpinner, gbc);

        // Active
        gbc.gridx = 0; gbc.gridy = 11;
        JLabel activeLabel = new JLabel("Active:");
        activeLabel.setFont(labelFont);
        panel.add(activeLabel, gbc);
        gbc.gridx = 1;
        JCheckBox activeCheck = new JCheckBox();
        activeCheck.setSelected(true);
        activeCheck.setBackground(Color.WHITE);
        panel.add(activeCheck, gbc);

        // Dynamic field enabling
        typeCombo.addActionListener(e -> {
            String selected = (String) typeCombo.getSelectedItem();
            percentLabel.setEnabled(selected.equals("PERCENT_OFF"));
            percentSpinner.setEnabled(selected.equals("PERCENT_OFF"));
            keywordLabel.setEnabled(selected.equals("BUY_X_GET_Y"));
            keywordField.setEnabled(selected.equals("BUY_X_GET_Y"));
            buyQtyLabel.setEnabled(selected.equals("BUY_X_GET_Y"));
            buyQtySpinner.setEnabled(selected.equals("BUY_X_GET_Y"));
            freeQtyLabel.setEnabled(selected.equals("BUY_X_GET_Y"));
            freeQtySpinner.setEnabled(selected.equals("BUY_X_GET_Y"));
            reqQtyLabel.setEnabled(selected.equals("MIX_AND_MATCH"));
            reqQtySpinner.setEnabled(selected.equals("MIX_AND_MATCH"));
            bundlePriceLabel.setEnabled(selected.equals("MIX_AND_MATCH"));
            bundlePriceSpinner.setEnabled(selected.equals("MIX_AND_MATCH"));
        });
        typeCombo.setSelectedIndex(0);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);
        JButton saveBtn = createButton("Save", SUCCESS_COLOR);
        JButton cancelBtn = createButton("Cancel", DANGER_COLOR);

        saveBtn.addActionListener(e -> {
            try {
                DiscountRuleDTO dto = new DiscountRuleDTO();
                dto.setName(nameField.getText().trim());
                dto.setDescription(descField.getText().trim());
                dto.setRuleType((String) typeCombo.getSelectedItem());

                String cat = (String) categoryCombo.getSelectedItem();
                dto.setCategory(cat.isEmpty() ? null : cat);

                dto.setItemKeyword(keywordField.getText().trim().isEmpty() ? null : keywordField.getText().trim());
                dto.setPercentOff((Double) percentSpinner.getValue());
                dto.setBuyQuantity((Integer) buyQtySpinner.getValue());
                dto.setFreeQuantity((Integer) freeQtySpinner.getValue());
                dto.setRequiredQuantity((Integer) reqQtySpinner.getValue());
                dto.setBundlePrice((Double) bundlePriceSpinner.getValue());
                dto.setPriority((Integer) prioritySpinner.getValue());
                dto.setActive(activeCheck.isSelected());

                createRule(dto);
                dialog.dispose();
                loadRules();
            } catch (Exception ex) {
                showError("Error creating rule: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);

        gbc.gridx = 0; gbc.gridy = 12; gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void createRule(DiscountRuleDTO dto) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(dto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RULES_ENDPOINT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 201) {
            showMessage("Rule created successfully!", "Success");
        } else {
            showError("Failed to create rule: " + response.body());
        }
    }

    private void editSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a rule to edit");
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        try {
            DiscountRuleDTO rule = getRuleById(id);
            showEditDialog(rule);
        } catch (Exception e) {
            showError("Error loading rule: " + e.getMessage());
        }
    }

    private DiscountRuleDTO getRuleById(Long id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RULES_ENDPOINT + "/" + id))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), DiscountRuleDTO.class);
        } else {
            throw new IOException("Failed to get rule: HTTP " + response.statusCode());
        }
    }

    private void showEditDialog(DiscountRuleDTO rule) {
        JDialog dialog = new JDialog(this, "Edit Discount Rule #" + rule.getId(), true);
        dialog.setSize(700, 800);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        Font labelFont = new Font("SansSerif", Font.BOLD, 13);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 13);

        // Pre-fill fields with existing data
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel typeLabel = new JLabel("Rule Type:");
        typeLabel.setFont(labelFont);
        panel.add(typeLabel, gbc);
        gbc.gridx = 1;
        String[] ruleTypes = {"PERCENT_OFF", "BUY_ONE_GET_ONE", "BUY_X_GET_Y", "MIX_AND_MATCH"};
        JComboBox<String> typeCombo = new JComboBox<>(ruleTypes);
        typeCombo.setFont(fieldFont);
        typeCombo.setSelectedItem(rule.getRuleType());
        panel.add(typeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setFont(labelFont);
        panel.add(nameLabel, gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(rule.getName() != null ? rule.getName() : "");
        nameField.setFont(fieldFont);
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(labelFont);
        panel.add(descLabel, gbc);
        gbc.gridx = 1;
        JTextField descField = new JTextField(rule.getDescription() != null ? rule.getDescription() : "");
        descField.setFont(fieldFont);
        panel.add(descField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        JLabel catLabel = new JLabel("Category (optional):");
        catLabel.setFont(labelFont);
        panel.add(catLabel, gbc);
        gbc.gridx = 1;
        String[] categories = {"", "BEVERAGE", "FOOD", "TOBACCO", "GENERAL"};
        JComboBox<String> categoryCombo = new JComboBox<>(categories);
        categoryCombo.setFont(fieldFont);
        categoryCombo.setSelectedItem(rule.getCategory() != null ? rule.getCategory() : "");
        panel.add(categoryCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        JLabel keywordLabel = new JLabel("Item Keyword:");
        keywordLabel.setFont(labelFont);
        panel.add(keywordLabel, gbc);
        gbc.gridx = 1;
        JTextField keywordField = new JTextField(rule.getItemKeyword() != null ? rule.getItemKeyword() : "");
        keywordField.setFont(fieldFont);
        panel.add(keywordField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        JLabel percentLabel = new JLabel("Percent Off:");
        percentLabel.setFont(labelFont);
        panel.add(percentLabel, gbc);
        gbc.gridx = 1;
        JSpinner percentSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(rule.getPercentOff() != null ? rule.getPercentOff() : 0.0),
                Double.valueOf(0.0), Double.valueOf(100.0), Double.valueOf(5.0)));
        percentSpinner.setFont(fieldFont);
        panel.add(percentSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        JLabel buyQtyLabel = new JLabel("Buy Quantity:");
        buyQtyLabel.setFont(labelFont);
        panel.add(buyQtyLabel, gbc);
        gbc.gridx = 1;
        JSpinner buyQtySpinner = new JSpinner(new SpinnerNumberModel(
                Integer.valueOf(rule.getBuyQuantity() != null ? rule.getBuyQuantity() : 1),
                Integer.valueOf(1), Integer.valueOf(100), Integer.valueOf(1)));
        buyQtySpinner.setFont(fieldFont);
        panel.add(buyQtySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 7;
        JLabel freeQtyLabel = new JLabel("Free Quantity:");
        freeQtyLabel.setFont(labelFont);
        panel.add(freeQtyLabel, gbc);
        gbc.gridx = 1;
        JSpinner freeQtySpinner = new JSpinner(new SpinnerNumberModel(
                Integer.valueOf(rule.getFreeQuantity() != null ? rule.getFreeQuantity() : 0),
                Integer.valueOf(0), Integer.valueOf(100), Integer.valueOf(1)));
        freeQtySpinner.setFont(fieldFont);
        panel.add(freeQtySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 8;
        JLabel reqQtyLabel = new JLabel("Required Quantity:");
        reqQtyLabel.setFont(labelFont);
        panel.add(reqQtyLabel, gbc);
        gbc.gridx = 1;
        JSpinner reqQtySpinner = new JSpinner(new SpinnerNumberModel(
                Integer.valueOf(rule.getRequiredQuantity() != null ? rule.getRequiredQuantity() : 1),
                Integer.valueOf(1), Integer.valueOf(100), Integer.valueOf(1)));
        reqQtySpinner.setFont(fieldFont);
        panel.add(reqQtySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 9;
        JLabel bundlePriceLabel = new JLabel("Bundle Price $:");
        bundlePriceLabel.setFont(labelFont);
        panel.add(bundlePriceLabel, gbc);
        gbc.gridx = 1;
        JSpinner bundlePriceSpinner = new JSpinner(new SpinnerNumberModel(
                Double.valueOf(rule.getBundlePrice() != null ? rule.getBundlePrice() : 0.0),
                Double.valueOf(0.0), Double.valueOf(1000.0), Double.valueOf(0.50)));
        bundlePriceSpinner.setFont(fieldFont);
        panel.add(bundlePriceSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 10;
        JLabel priorityLabel = new JLabel("Priority:");
        priorityLabel.setFont(labelFont);
        panel.add(priorityLabel, gbc);
        gbc.gridx = 1;
        JSpinner prioritySpinner = new JSpinner(new SpinnerNumberModel(
                Integer.valueOf(rule.getPriority()), Integer.valueOf(1), Integer.valueOf(100), Integer.valueOf(1)));
        prioritySpinner.setFont(fieldFont);
        panel.add(prioritySpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 11;
        JLabel activeLabel = new JLabel("Active:");
        activeLabel.setFont(labelFont);
        panel.add(activeLabel, gbc);
        gbc.gridx = 1;
        JCheckBox activeCheck = new JCheckBox();
        activeCheck.setSelected(rule.isActive());
        activeCheck.setBackground(Color.WHITE);
        panel.add(activeCheck, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnPanel.setBackground(Color.WHITE);
        JButton updateBtn = createButton("Update", SUCCESS_COLOR);
        JButton cancelBtn = createButton("Cancel", DANGER_COLOR);

        updateBtn.addActionListener(e -> {
            try {
                rule.setName(nameField.getText().trim());
                rule.setDescription(descField.getText().trim());
                rule.setRuleType((String) typeCombo.getSelectedItem());

                String cat = (String) categoryCombo.getSelectedItem();
                rule.setCategory(cat.isEmpty() ? null : cat);

                rule.setItemKeyword(keywordField.getText().trim().isEmpty() ? null : keywordField.getText().trim());
                rule.setPercentOff((Double) percentSpinner.getValue());
                rule.setBuyQuantity((Integer) buyQtySpinner.getValue());
                rule.setFreeQuantity((Integer) freeQtySpinner.getValue());
                rule.setRequiredQuantity((Integer) reqQtySpinner.getValue());
                rule.setBundlePrice((Double) bundlePriceSpinner.getValue());
                rule.setPriority((Integer) prioritySpinner.getValue());
                rule.setActive(activeCheck.isSelected());

                updateRule(rule.getId(), rule);
                dialog.dispose();
                loadRules();
            } catch (Exception ex) {
                showError("Error updating rule: " + ex.getMessage());
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        btnPanel.add(updateBtn);
        btnPanel.add(cancelBtn);

        gbc.gridx = 0; gbc.gridy = 12; gbc.gridwidth = 2;
        panel.add(btnPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void updateRule(Long id, DiscountRuleDTO dto) throws IOException, InterruptedException {
        String jsonBody = objectMapper.writeValueAsString(dto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RULES_ENDPOINT + "/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 200) {
            showMessage("Rule updated successfully!", "Success");
        } else {
            showError("Failed to update rule: " + response.body());
        }
    }

    private void toggleSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a rule to toggle");
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RULES_ENDPOINT + "/" + id + "/toggle"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                showMessage("Rule status toggled successfully!", "Success");
                loadRules();
            } else {
                showError("Failed to toggle rule: " + response.body());
            }
        } catch (Exception e) {
            showError("Error toggling rule: " + e.getMessage());
        }
    }

    private void deleteSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow == -1) {
            showError("Please select a rule to delete");
            return;
        }

        Long id = (Long) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete rule #" + id + "?\n\nThis action cannot be undone.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RULES_ENDPOINT + "/" + id))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                showMessage("Rule deleted successfully!", "Success");
                loadRules();
            } else {
                showError("Failed to delete rule: " + response.body());
            }
        } catch (Exception e) {
            showError("Error deleting rule: " + e.getMessage());
        }
    }

    private void showMessage(String message, String title) {
        JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            DiscountRuleManager manager = new DiscountRuleManager();
            manager.setVisible(true);
        });
    }
}