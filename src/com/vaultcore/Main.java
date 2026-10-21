package com.vaultcore;

import com.vaultcore.exceptions.InsufficientFundsException;
import com.vaultcore.model.Account;
import com.vaultcore.model.Customer;
import com.vaultcore.model.PremiumCustomer;
import com.vaultcore.service.BankService;
import com.vaultcore.service.TransactionLogger;

import java.util.concurrent.ExecutorService; // manages thread/tasks
import java.util.concurrent.Executors;// thread pool create
import java.util.concurrent.TimeUnit;//represent time like seconds

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
        //Bank is created(Manager)
        BankService bank = new BankService();

        // Two customers is created
        Customer rohit = new Customer("CUST-001", "Rohit Sharma", "rohit@example.com");
        Customer asha = new PremiumCustomer("CUST-002", "Asha Verma", "asha@example.com");
        //whenever call sout(rohit) -> call print like rohit.toString() otherwise it prints not correctly(needed)

        // Two accounts are opened
        Account savings = bank.openAccount("SAVINGS", "SAV-1001", rohit, 5000.0);
        Account current = bank.openAccount("CURRENT", "CUR-2001", asha, 2000.0);

        System.out.println("=== Initial State ===");
        bank.allAccounts().forEach(System.out::println);

        //Normal banking is tested(Rupees 3000 current withdraw etc)
        System.out.println("\n=== Single-threaded sanity checks ===");
        savings.deposit(1500);
        current.withdraw(3000);
        try {
            savings.withdraw(100000);
        } catch (InsufficientFundsException e) {
            System.out.println("Expected failure caught: " + e.getMessage());
        }

        System.out.println("\n=== Concurrency demo: 10 threads hitting the same two accounts ===");
        runConcurrentTransactions(savings, current);

        System.out.println("\n=== Monthly interest run (polymorphic dispatch) ===");
        bank.applyMonthlyInterest();

        System.out.println("\n=== Final State ===");
        bank.allAccounts().forEach(System.out::println);

        System.out.println("\n=== Customers ===");
        System.out.println(rohit + " -> service fee discount: " + rohit.getServiceFeeDiscountPercent() + "%");
        System.out.println(asha + " -> service fee discount: " + asha.getServiceFeeDiscountPercent() + "%");

        System.out.println("\nFull transaction log also written to transactions.log ("
                + TransactionLogger.getInstance().getHistory().size() + " entries)");
    }

    //Concurrency
    //4 worker thread given 10 banking tasks eg: deposit 100 , transfer 50 etc.
    private static void runConcurrentTransactions(Account savings, Account current) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        double startingTotal = savings.getBalance() + current.getBalance();

        for (int i = 0; i < 10; i++) {
            final int id = i;
            //Yes. submit just hands the pool a piece of work (the code inside () -> { ... }),
            // and the pool decides which worker runs it and when. It doesn’t run the work itself and
            // doesn’t wait for it to finish.
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
        //Without shutdown(), the 4 worker threads stay alive forever, waiting for more jobs.
        // The program never exits, even after all the work is done, so you’d have to press the stop button yourself.
        //shutdown() tells the workers “no more jobs are coming, so once you finish the ones you have, you can stop.” That lets the program end normally.
        //It’s also needed for awaitTermination to make sense. That call waits for the pool to finish, but a pool that’s still open to new jobs never counts as finished.
        pool.awaitTermination(10, TimeUnit.SECONDS);
        //makes main wait only up to your time limit (10 seconds). After that it stops waiting and moves on to
        // the print, so the program can never get stuck forever, even if a job hangs.
        //The trade-off is that the print may happen before the jobs finish, so the total shown can be wrong.


        //Threads finish(did loose anything while all operations where happening)
        //eg: 10 deposits of 100 rupees
        //total money across both account increase by 1000 rupees

        double endingTotal = savings.getBalance() + current.getBalance();
        System.out.printf("Total before: %.2f | Total after: %.2f (should be +1000, no money lost)%n",
                startingTotal, endingTotal);

        //Finally, monthly interest apply and print final account/customer info and transaction log
    }
}
