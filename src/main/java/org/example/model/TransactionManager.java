package org.example.model;

import org.example.TransactionDatabase;
import org.example.TransactionDatabase.SuspendedTransactionInfo;

import java.sql.SQLException;
import java.util.List;

/**
 * TransactionManager now delegates all suspend/resume operations to the database.
 * Suspended transactions are persistent and survive application restarts.
 */
public class TransactionManager {
    private final TransactionDatabase database;

    public TransactionManager(TransactionDatabase database) {
        this.database = database;
    }

    /**
     * Suspend a transaction by saving it to the database with SUSPENDED status.
     * Returns the transaction ID, or -1 if the transaction is empty.
     */
    public int suspendTransaction(Transaction transaction) {
        if (transaction.getItemCount() == 0) {
            return -1;
        }

        try {
            return database.suspendTransaction(transaction);
        } catch (SQLException e) {
            System.err.println("Error suspending transaction: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Resume a suspended transaction from the database.
     * Returns null if the transaction doesn't exist or is not suspended.
     */
    public Transaction resumeTransaction(int transactionId) {
        try {
            Transaction transaction = database.resumeTransaction(transactionId);

            if (transaction != null) {
                // Delete from suspended list after successful resume
                database.deleteSuspendedTransaction(transactionId);
            }

            return transaction;
        } catch (SQLException e) {
            System.err.println("Error resuming transaction: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get all suspended transactions from the database.
     */
    public List<SuspendedTransactionInfo> getSuspendedTransactions() {
        try {
            return database.getSuspendedTransactions();
        } catch (SQLException e) {
            System.err.println("Error getting suspended transactions: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Check if there are any suspended transactions in the database.
     */
    public boolean hasSuspendedTransactions() {
        try {
            return database.hasSuspendedTransactions();
        } catch (SQLException e) {
            System.err.println("Error checking suspended transactions: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}