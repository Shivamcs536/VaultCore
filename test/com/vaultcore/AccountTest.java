package com.vaultcore;

import com.vaultcore.exceptions.InsufficientFundsException;
import com.vaultcore.model.Account;
import com.vaultcore.model.CurrentAccount;
import com.vaultcore.model.Customer;
import com.vaultcore.model.SavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic behavioural tests for a single account: deposits, withdrawals,
 * and the per-type minimum balance rules (Savings vs Current) that back
 * up the "polymorphism" claim in the README.
 */
class AccountTest {

    private Customer owner;

    @BeforeEach
    void setUp() {
        owner = new Customer("CUST-100", "Test User", "test@example.com");
    }

    @Test
    void depositIncreasesBalance() {
        Account acc = new SavingsAccount("SAV-100", owner, 1000);
        acc.deposit(500);
        assertEquals(1500.0, acc.getBalance(), 0.001);
    }

    @Test
    void depositRejectsZeroOrNegativeAmount() {
        Account acc = new SavingsAccount("SAV-101", owner, 1000);
        assertThrows(IllegalArgumentException.class, () -> acc.deposit(0));
        assertThrows(IllegalArgumentException.class, () -> acc.deposit(-50));
    }

    @Test
    void withdrawDecreasesBalanceWhenAboveMinimum() throws InsufficientFundsException {
        Account acc = new SavingsAccount("SAV-102", owner, 2000);
        acc.withdraw(500);
        assertEquals(1500.0, acc.getBalance(), 0.001);
    }

    @Test
    void savingsWithdrawBelowMinimumBalanceThrows() {
        // Savings accounts must keep at least 1000
        Account acc = new SavingsAccount("SAV-103", owner, 1200);
        assertThrows(InsufficientFundsException.class, () -> acc.withdraw(500));
    }

    @Test
    void currentAccountAllowsDippingIntoOverdraft() throws InsufficientFundsException {
        // Current accounts have a negative minimum balance (overdraft limit)
        Account acc = new CurrentAccount("CUR-100", owner, 1000, 5000);
        acc.withdraw(3000); // goes to -2000, still within the 5000 overdraft limit
        assertEquals(-2000.0, acc.getBalance(), 0.001);
    }

    @Test
    void currentAccountRejectsWithdrawalBeyondOverdraftLimit() {
        Account acc = new CurrentAccount("CUR-101", owner, 1000, 5000);
        assertThrows(InsufficientFundsException.class, () -> acc.withdraw(10000));
    }

    @Test
    void withdrawRejectsZeroOrNegativeAmount() {
        Account acc = new SavingsAccount("SAV-104", owner, 2000);
        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(0));
        assertThrows(IllegalArgumentException.class, () -> acc.withdraw(-10));
    }

    @Test
    void savingsAccountEarnsInterestButCurrentDoesNot() {
        Account savings = new SavingsAccount("SAV-105", owner, 1000);
        Account current = new CurrentAccount("CUR-102", owner, 1000, 5000);

        assertEquals(40.0, savings.calculateInterest(), 0.001); // 4% of 1000
        assertEquals(0.0, current.calculateInterest(), 0.001);
    }
}
