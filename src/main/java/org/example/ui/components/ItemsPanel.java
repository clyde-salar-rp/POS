package org.example.ui.components;

import org.example.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ItemsPanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;

    public ItemsPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] columns = {"#", "UPC", "Description", "Price"};
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
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateItems(List<Product> items) {
        tableModel.setRowCount(0);
        for (int i = 0; i < items.size(); i++) {
            Product p = items.get(i);
            tableModel.addRow(new Object[]{
                    i + 1, p.getUpc(), p.getDescription(), String.format("$%.2f", p.getPrice())
            });
        }

        // Auto-scroll to bottom
        if (table.getRowCount() > 0) {
            table.scrollRectToVisible(
                    table.getCellRect(table.getRowCount() - 1, 0, true)
            );
        }
    }
}