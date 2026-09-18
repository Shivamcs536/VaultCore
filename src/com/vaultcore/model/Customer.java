package com.vaultcore.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates customer identity. Kept deliberately simple: fields are
 * private, exposed only through getters, and account ownership is
 * tracked here rather than in the bank service.
 */
public class Customer {
    private final String customerId;
    private final String name;
    private final String email;
    private final List<Account> accounts = new ArrayList<>();

    public Customer(String customerId, String name, String email) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
    }

    public String getCustomerId() { return customerId; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    void addAccount(Account account) { // package-private, called by AccountFactory
        accounts.add(account);
    }

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts); // defensive copy - protects encapsulation
    }

    /** Overridden by PremiumCustomer to waive fees. Demonstrates polymorphism
     *  on the customer side, not just the account side. */
    public double getServiceFeeDiscountPercent() {
        return 0.0;
    }

    @Override
    public String toString() {
        return String.format("%s (%s) <%s>", name, customerId, email);
    }
}
