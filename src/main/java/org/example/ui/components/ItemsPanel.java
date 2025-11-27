package org.example.ui.components;

import org.example.model.Product;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;

public class ItemsPanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final DecimalFormat moneyFormat;
    private static final Color HEADER_BG = new Color(25, 118, 210);
    private static final Color ROW_ALT_BG = new Color(248, 249, 252);
    private static final Color CARD_BG = Color.WHITE;

    public ItemsPanel() {
        setLayout(new BorderLayout());
        setBackground(CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 224, 224), 1),
                new EmptyBorder(0, 0, 0, 0)
        ));

        this.moneyFormat = new DecimalFormat("#,##0.00");

        String[] columns = {"Qty", "UPC", "Description", "Price", "Total"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(10, 4));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setSelectionBackground(new Color(227, 242, 253));
        table.setSelectionForeground(Color.BLACK);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(350);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBackground(HEADER_BG);
        header.setForeground(Color.WHITE);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 45));
        header.setBorder(BorderFactory.createEmptyBorder());
        header.setReorderingAllowed(false); // Disable column dragging

        // Cell alignment
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        // Alternating row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? CARD_BG : ROW_ALT_BG);
                }

                // Alignment
                if (column == 0) {
                    ((JLabel) c).setHorizontalAlignment(JLabel.CENTER);
                } else if (column == 3 || column == 4) {
                    ((JLabel) c).setHorizontalAlignment(JLabel.RIGHT);
                } else {
                    ((JLabel) c).setHorizontalAlignment(JLabel.LEFT);
                }

                ((JLabel) c).setBorder(new EmptyBorder(5, 10, 5, 10));

                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_BG);

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