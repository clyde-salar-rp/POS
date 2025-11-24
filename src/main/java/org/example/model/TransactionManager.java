package org.example.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionManager {
    private final Map<Integer, SuspendedTransaction> suspendedTransactions;
    private int nextTransactionId;
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public TransactionManager() {
        this.suspendedTransactions = new HashMap<>();
        this.nextTransactionId = 1;
    }

    public int suspendTransaction(Transaction transaction) {
        if (transaction.getItemCount() == 0) {
            return -1;
        }

        int transactionId = nextTransactionId++;
        SuspendedTransaction suspended = new SuspendedTransaction(
                transactionId,
                transaction.getItems(),
                LocalDateTime.now()
        );

        suspendedTransactions.put(transactionId, suspended);
        return transactionId;
    }

    public Transaction resumeTransaction(int transactionId) {
        SuspendedTransaction suspended = suspendedTransactions.remove(transactionId);

        if (suspended == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        for (TransactionItem item : suspended.getItems()) {
            transaction.addItem(item.getProduct(), item.getQuantity());
        }

        return transaction;
    }

    public List<SuspendedTransaction> getSuspendedTransactions() {
        return new ArrayList<>(suspendedTransactions.values());
    }

    public boolean hasSuspendedTransactions() {
        return !suspendedTransactions.isEmpty();
    }

    public void clearSuspendedTransaction(int transactionId) {
        suspendedTransactions.remove(transactionId);
    }

    public static class SuspendedTransaction {
        private final int id;
        private final List<TransactionItem> items;
        private final LocalDateTime suspendTime;

        public SuspendedTransaction(int id, List<TransactionItem> items, LocalDateTime suspendTime) {
            this.id = id;
            this.items = new ArrayList<>(items);
            this.suspendTime = suspendTime;
        }

        public int getId() {
            return id;
        }

        public List<TransactionItem> getItems() {
            return items;
        }

        public LocalDateTime getSuspendTime() {
            return suspendTime;
        }

        public double getTotal() {
            double subtotal = items.stream()
                    .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                    .sum();
            return subtotal * 1.07; // Include tax
        }

        public int getItemCount() {
            return items.size();
        }

        @Override
        public String toString() {
            return String.format("Trans #%d - %s - %d items - $%.2f",
                    id,
                    suspendTime.format(TIME_FORMAT),
                    items.size(),
                    getTotal());
        }
    }
}