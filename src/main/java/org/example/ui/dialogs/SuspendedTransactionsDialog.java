package org.example.ui.dialogs;

import org.example.model.TransactionManager;
import org.example.model.TransactionManager.SuspendedTransaction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class SuspendedTransactionsDialog extends JDialog {
    private final JList<SuspendedTransaction> transactionList;
    private final DefaultListModel<SuspendedTransaction> listModel;
    private SuspendedTransaction selectedTransaction;

    public SuspendedTransactionsDialog(Frame owner, TransactionManager manager) {
        super(owner, "Suspended Transactions", true);
        setSize(500, 400);
        setLocationRelativeTo(owner);

        listModel = new DefaultListModel<>();
        List<SuspendedTransaction> suspended = manager.getSuspendedTransactions();
        for (SuspendedTransaction trans : suspended) {
            listModel.addElement(trans);
        }

        transactionList = new JList<>(listModel);
        transactionList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        transactionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(transactionList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Select Transaction to Resume"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resumeButton = new JButton("Resume");
        JButton cancelButton = new JButton("Cancel");

        resumeButton.addActionListener(e -> {
            selectedTransaction = transactionList.getSelectedValue();
            if (selectedTransaction != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Please select a transaction to resume",
                        "No Selection",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> {
            selectedTransaction = null;
            dispose();
        });

        buttonPanel.add(resumeButton);
        buttonPanel.add(cancelButton);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    public SuspendedTransaction getSelectedTransaction() {
        return selectedTransaction;
    }

    public static SuspendedTransaction showDialog(Frame owner, TransactionManager manager) {
        if (!manager.hasSuspendedTransactions()) {
            JOptionPane.showMessageDialog(owner,
                    "No suspended transactions available",
                    "No Transactions",
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        SuspendedTransactionsDialog dialog = new SuspendedTransactionsDialog(owner, manager);
        dialog.setVisible(true);
        return dialog.getSelectedTransaction();
    }
}