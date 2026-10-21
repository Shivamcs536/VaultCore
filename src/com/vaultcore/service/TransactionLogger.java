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

    //Your program should record:
    //- Which account deposited money?
    //- How much money was deposited?
    //- What is the new balance?
    //TransactionLogger is simply the record keeper of your banking application.
    // It records these details whenever a transaction happens

    private static volatile TransactionLogger instance; // helps initialized object to be visible to other threads
    private final List<String> history = Collections.synchronizedList(new ArrayList<>());
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private TransactionLogger() { }
    //constructor is private other classes cannot directly create TransactionLogger()

    //Singleton means a class should have only one object created and that same
    // object is reused whenever needed.

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
        //This method receives the transaction details.
        //Inside it:
        //- String.format() creates a properly formatted transaction message.
        //- history.add(line) saves the message in the list.
        //- System.out.println(line) displays it in the console.
        //- writeToFile(line) saves it in transactions.log.
        //The thread=%s part also records which thread performed the transaction.
    }

    private void writeToFile(String line) {//true means append — add the new record at the end without deleting the old records.
        try (PrintWriter out = new PrintWriter(new FileWriter("transactions.log", true))) {
            out.println(line);
            //FileWriter can write text directly. PrintWriter adds convenient methods like println() and printf().
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
