package com.vaultcore.model;

/**
 * Earns interest, and cannot be drawn below a fixed minimum balance.
 */
public class SavingsAccount extends Account {

    private static final double INTEREST_RATE = 0.04; // 4% p.a.
    private static final double MINIMUM_BALANCE = 1000.0;

    public SavingsAccount(String accountNumber, Customer owner, double initialBalance) {
        super(accountNumber, owner, initialBalance);
    }

    @Override
    public double calculateInterest() {
        return getBalance() * INTEREST_RATE;
    }

    @Override
    public double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}
