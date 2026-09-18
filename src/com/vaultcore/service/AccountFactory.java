package com.vaultcore.service;

import com.vaultcore.exceptions.InvalidAccountException;
import com.vaultcore.model.Account;
import com.vaultcore.model.CurrentAccount;
import com.vaultcore.model.Customer;
import com.vaultcore.model.SavingsAccount;

/**
 * Factory Design Pattern: centralises account creation so callers never
 * call "new SavingsAccount(...)" directly. Makes it trivial to add a new
 * account type later without touching client code.
 */
public final class AccountFactory {

    private AccountFactory() { } // static factory, no instances needed

    public static Account createAccount(String type, String accountNumber, Customer owner, double initialBalance) {
        if (type == null) {
            throw new InvalidAccountException("Account type cannot be null");
        }
        switch (type.trim().toUpperCase()) {
            case "SAVINGS":
                return new SavingsAccount(accountNumber, owner, initialBalance);
            case "CURRENT":
                return new CurrentAccount(accountNumber, owner, initialBalance, 5000.0);
            default:
                throw new InvalidAccountException("Unknown account type: " + type);
        }
    }
}
