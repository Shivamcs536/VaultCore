package com.vaultcore.model;

import com.vaultcore.exceptions.InsufficientFundsException;
import com.vaultcore.interfaces.Transactable;
import com.vaultcore.service.TransactionLogger;

/**
 * Abstract base for every account type. Holds the balance privately
 * (encapsulation) and implements Transactable so deposit/withdraw/transfer
 * behave consistently, while calculateInterest(), getMinimumBalance() and
 * getAccountType() are left abstract so each subclass defines its own
 * rules (abstraction + polymorphism).
 *
 * All balance mutations are synchronized on the account's own monitor so
 * concurrent threads (see ConcurrencyDemo) cannot corrupt the balance.
 */
public abstract class Account implements Transactable {

    private final String accountNumber;
    private final Customer owner;
    private double balance;

    protected Account(String accountNumber, Customer owner, double initialBalance) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.balance = initialBalance;
    }

    public String getAccountNumber() { return accountNumber; }
    public Customer getOwner() { return owner; }

    public synchronized double getBalance() {
        return balance;
    }

    /** Each subclass decides how interest is calculated (or not at all). */
    public abstract double calculateInterest();

    /** Each subclass decides its floor: 0, a positive minimum, or a
     *  negative number to represent an overdraft limit. */
    public abstract double getMinimumBalance();

    /** Human-readable label used in logs and reports. */
    public abstract String getAccountType();

    @Override
    public synchronized void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance += amount;
        TransactionLogger.getInstance().log(accountNumber, "DEPOSIT", amount, balance);
    }

    @Override
    public synchronized void withdraw(double amount) throws InsufficientFundsException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }
        if (balance - amount < getMinimumBalance()) {
            throw new InsufficientFundsException(
                    String.format("Withdrawal of %.2f from %s denied - balance %.2f, minimum allowed %.2f",
                            amount, accountNumber, balance, getMinimumBalance()));
        }
        balance -= amount;
        TransactionLogger.getInstance().log(accountNumber, "WITHDRAW", amount, balance);
    }

    /**
     * Transfers between two accounts. Locks are acquired in a fixed order
     * (by account number) so two threads transferring in opposite
     * directions at the same time can never deadlock.
     */
    @Override
    public void transfer(Account to, double amount) throws InsufficientFundsException {
        if (to == null) {
            throw new IllegalArgumentException("Destination account cannot be null");
        }
        Account first = this.accountNumber.compareTo(to.accountNumber) <= 0 ? this : to;
        Account second = first == this ? to : this;

        synchronized (first) {
            synchronized (second) {
                this.withdraw(amount);
                to.creditFromTransfer(amount);
            }
        }
    }

    /** Internal credit used only by transfer(), kept separate from the
     *  public deposit() so the log entry reads "TRANSFER_IN" instead of
     *  "DEPOSIT". Still guarded by the same monitor as everything else. */
    private synchronized void creditFromTransfer(double amount) {
        balance += amount;
        TransactionLogger.getInstance().log(accountNumber, "TRANSFER_IN", amount, balance);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | Owner: %s | Balance: %.2f",
                getAccountType(), accountNumber, owner.getName(), balance);
    }
}
