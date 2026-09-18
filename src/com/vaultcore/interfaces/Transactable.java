package com.vaultcore.interfaces;

import com.vaultcore.exceptions.InsufficientFundsException;
import com.vaultcore.model.Account;

/**
 * Contract every account type must fulfil. Implemented differently by
 * SavingsAccount and CurrentAccount (see their minimum-balance rules),
 * which is where the polymorphism in this project really shows.
 */
public interface Transactable {
    void deposit(double amount);
    void withdraw(double amount) throws InsufficientFundsException;
    void transfer(Account to, double amount) throws InsufficientFundsException;
}
