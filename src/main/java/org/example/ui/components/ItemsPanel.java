package org.example.ui.components;

import org.example.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class ItemsPanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final DecimalFormat moneyFormat;

    public ItemsPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        this.moneyFormat = new DecimalFormat("#,##0.00");

        String[] columns = {"Qty", "UPC", "Description", "Price", "Total"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(250);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateItems(List<Product> items) {
        tableModel.setRowCount(0);
        for (Product product : items) {
            tableModel.addRow(new Object[]{
                    product.getQuantity(),
                    product.getUpc(),
                    product.getDescription(),
                    String.format("$%s", moneyFormat.format(product.getPrice())),
                    String.format("$%s", moneyFormat.format(product.getLineTotal()))
            });
        }

        // Auto-scroll to bottom
        if (table.getRowCount() > 0) {
            table.scrollRectToVisible(
                    table.getCellRect(table.getRowCount() - 1, 0, true)
            );
        }
    }

    public int getSelectedRow() {
        return table.getSelectedRow();
    }
}