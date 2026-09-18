package com.vaultcore.service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Singleton (double-checked locking) that records every transaction to
 * both an in-memory list (for the console summary) and a log file
 * (transactions.log). Every write is synchronized so concurrent threads
 * hitting different accounts at the same time still produce a clean,
 * non-interleaved log.
 */
public final class TransactionLogger {

    private static volatile TransactionLogger instance;
    private final List<String> history = Collections.synchronizedList(new ArrayList<>());
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private TransactionLogger() { }

    public static TransactionLogger getInstance() {
        if (instance == null) {
            synchronized (TransactionLogger.class) {
                if (instance == null) {
                    instance = new TransactionLogger();
                }
            }
        }
        return instance;
    }

    public synchronized void log(String accountNumber, String type, double amount, double resultingBalance) {
        String line = String.format("[%s] %-12s %-10s amount=%-10.2f newBalance=%-10.2f thread=%s",
                LocalDateTime.now().format(TS), accountNumber, type, amount, resultingBalance,
                Thread.currentThread().getName());
        history.add(line);
        System.out.println(line);
        writeToFile(line);
    }

    private void writeToFile(String line) {
        try (PrintWriter out = new PrintWriter(new FileWriter("transactions.log", true))) {
            out.println(line);
        } catch (IOException e) {
            System.err.println("Could not write to transactions.log: " + e.getMessage());
        }
    }

    public List<String> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }
}
