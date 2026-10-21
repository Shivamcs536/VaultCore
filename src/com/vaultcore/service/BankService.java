package com.vaultcore.service;

import com.vaultcore.exceptions.InvalidAccountException;
import com.vaultcore.model.Account;
import com.vaultcore.model.Customer;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry the rest of the app talks to instead of poking
 * Account objects directly. Uses a thread-safe map since accounts can be
 * looked up while other threads are transacting on them.
 */
public class BankService {

    //BankService.java is like the bank manager in your project. It keeps track of
    // accounts, creates accounts through the factory, finds accounts, and applies interest.
    private final Map<String, Account> accounts = new LinkedHashMap<>();

    public synchronized Account openAccount(String type, String accountNumber, Customer owner, double initialBalance) {
        if (accounts.containsKey(accountNumber)) {
            throw new InvalidAccountException("Account number already exists: " + accountNumber);
        }
        Account account = AccountFactory.createAccount(type, accountNumber, owner, initialBalance);
        accounts.put(accountNumber, account);
        return account;
    }

    public synchronized Account findAccount(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) {
            throw new InvalidAccountException("No such account: " + accountNumber);
        }
        return account;
    }

    public synchronized Collection<Account> allAccounts() {
        return accounts.values();
    }

    /** Applies each account's own interest rule (polymorphic dispatch -
     *  the service never checks "if instanceof SavingsAccount"). */
    public void applyMonthlyInterest() {
        for (Account acc : allAccounts()) {
            double interest = acc.calculateInterest();
            if (interest > 0) {
                acc.deposit(interest);
            }
        }
    }
}
