package com.vaultcore.model;

/**
 * No interest, but allows an overdraft (a negative minimum balance)
 * to support business-style spending.
 */
public class CurrentAccount extends Account {

    private final double overdraftLimit;

    public CurrentAccount(String accountNumber, Customer owner, double initialBalance, double overdraftLimit) {
        super(accountNumber, owner, initialBalance);
        this.overdraftLimit = Math.abs(overdraftLimit);
    }

    @Override
    public double calculateInterest() {
        return 0.0; // current accounts don't earn interest
    }

    @Override
    public double getMinimumBalance() {
        return -overdraftLimit;
    }

    @Override
    public String getAccountType() {
        return "Current";
    }
}
