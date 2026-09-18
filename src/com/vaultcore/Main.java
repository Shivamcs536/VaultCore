package com.vaultcore;

import com.vaultcore.exceptions.InsufficientFundsException;
import com.vaultcore.model.Account;
import com.vaultcore.model.Customer;
import com.vaultcore.model.PremiumCustomer;
import com.vaultcore.service.BankService;
import com.vaultcore.service.TransactionLogger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * VaultCore - Concurrent Banking System
 * ---------------------------------------------------------------------
 * Demo entry point. Shows:
 *   - Encapsulation   -> Account.balance is private, only touched via methods
 *   - Abstraction     -> Account is abstract; callers work with the type, not the detail
 *   - Inheritance     -> SavingsAccount / CurrentAccount / PremiumCustomer
 *   - Polymorphism    -> calculateInterest(), getMinimumBalance() dispatch differently per type
 *   - Interfaces      -> Transactable contract implemented by Account
 *   - Design patterns -> Factory (AccountFactory), Singleton (TransactionLogger)
 *   - Concurrency     -> multiple threads hammering the same accounts safely
 */
public class Main {

    public static void main(String[] args) throws Exception {
        BankService bank = new BankService();

        Customer rohit = new Customer("CUST-001", "Rohit Sharma", "rohit@example.com");
        Customer asha = new PremiumCustomer("CUST-002", "Asha Verma", "asha@example.com");

        Account savings = bank.openAccount("SAVINGS", "SAV-1001", rohit, 5000.0);
        Account current = bank.openAccount("CURRENT", "CUR-2001", asha, 2000.0);

        System.out.println("=== Initial State ===");
        bank.allAccounts().forEach(System.out::println);

        System.out.println("\n=== Single-threaded sanity checks ===");
        savings.deposit(1500);
        current.withdraw(3000); // dips into the overdraft, allowed
        try {
            savings.withdraw(100000); // should fail: below minimum balance
        } catch (InsufficientFundsException e) {
            System.out.println("Expected failure caught: " + e.getMessage());
        }

        System.out.println("\n=== Concurrency demo: 10 threads hitting the same two accounts ===");
        runConcurrentTransactions(savings, current);

        System.out.println("\n=== Monthly interest run (polymorphic dispatch) ===");
        bank.applyMonthlyInterest(); // Savings earns interest, Current earns nothing - same call site

        System.out.println("\n=== Final State ===");
        bank.allAccounts().forEach(System.out::println);

        System.out.println("\n=== Customers ===");
        System.out.println(rohit + " -> service fee discount: " + rohit.getServiceFeeDiscountPercent() + "%");
        System.out.println(asha + " -> service fee discount: " + asha.getServiceFeeDiscountPercent() + "%");

        System.out.println("\nFull transaction log also written to transactions.log ("
                + TransactionLogger.getInstance().getHistory().size() + " entries)");
    }

    private static void runConcurrentTransactions(Account savings, Account current) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        double startingTotal = savings.getBalance() + current.getBalance();

        for (int i = 0; i < 10; i++) {
            final int id = i;
            pool.submit(() -> {
                try {
                    if (id % 2 == 0) {
                        savings.deposit(100);
                        savings.transfer(current, 50);
                    } else {
                        current.deposit(100);
                        current.transfer(savings, 50);
                    }
                } catch (InsufficientFundsException e) {
                    System.out.println("Thread " + id + " transfer skipped: " + e.getMessage());
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        // Net effect of 5 pairs each moving 100 in + 50 back out should be
        // startingTotal + 1000 (10 deposits of 100), with balances still
        // consistent since transfers just moved money between the two.
        double endingTotal = savings.getBalance() + current.getBalance();
        System.out.printf("Total before: %.2f | Total after: %.2f (should be +1000, no money lost)%n",
                startingTotal, endingTotal);
    }
}
