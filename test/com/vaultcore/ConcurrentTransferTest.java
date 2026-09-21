package com.vaultcore;

import com.vaultcore.model.Account;
import com.vaultcore.model.Customer;
import com.vaultcore.model.SavingsAccount;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This is the test that actually backs up the "thread-safe, deadlock-safe"
 * claim in the README. It is not enough to run the demo once and eyeball
 * the output - these tests fire many threads at the same accounts and
 * assert, programmatically, that the total balance is exactly conserved
 * and that the whole thing finishes (i.e. no deadlock).
 */
class ConcurrentTransferTest {

    /**
     * 100 threads transfer money back and forth between two accounts at
     * the same time. If synchronization were missing, some updates would
     * be lost and the total would drift. If the locking order were wrong,
     * this would deadlock and the test would time out instead of failing
     * cleanly - the @Timeout below turns a hang into a clear failure.
     */
    @RepeatedTest(5) // run several times - race conditions don't always show up on the first try
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void concurrentBidirectionalTransfersPreserveTotalBalance() throws InterruptedException {
        Customer c1 = new Customer("CUST-300", "Alice", "alice@example.com");
        Customer c2 = new Customer("CUST-301", "Bob", "bob@example.com");
        Account a = new SavingsAccount("SAV-300", c1, 50000);
        Account b = new SavingsAccount("SAV-301", c2, 50000);

        double totalBefore = a.getBalance() + b.getBalance();

        int threadCount = 100;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch startGate = new CountDownLatch(1); // release all threads at once
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final boolean forward = (i % 2 == 0);
            pool.submit(() -> {
                try {
                    startGate.await();
                    if (forward) {
                        a.transfer(b, 10);
                    } else {
                        b.transfer(a, 10);
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        startGate.countDown(); // let every thread go at once - maximises contention
        boolean finishedInTime = doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finishedInTime, "Threads did not finish in time - possible deadlock");
        assertEquals(0, failures.get(), "No transfer should fail with equal-sized accounts and amounts");

        double totalAfter = a.getBalance() + b.getBalance();
        assertEquals(totalBefore, totalAfter, 0.001,
                "Total balance must be conserved across all concurrent transfers");
    }

    /**
     * 50 threads deposit concurrently into the same account. Without
     * synchronized deposit(), this reliably loses updates (classic
     * read-modify-write race). With it, every deposit must land.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void concurrentDepositsAllLand() throws InterruptedException {
        Customer owner = new Customer("CUST-302", "Carol", "carol@example.com");
        Account acc = new SavingsAccount("SAV-302", owner, 0);

        int threadCount = 50;
        double amountEach = 100;
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                acc.deposit(amountEach);
                doneGate.countDown();
            });
        }

        assertTrue(doneGate.await(10, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(threadCount * amountEach, acc.getBalance(), 0.001,
                "Every concurrent deposit must be reflected - none should be lost to a race condition");
    }

    /**
     * Threads on opposite ends of the same pair of accounts (A->B and
     * B->A simultaneously) is exactly the classic deadlock setup. This
     * proves the fixed lock-ordering in Account.transfer() prevents it.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void oppositeDirectionTransfersDoNotDeadlock() throws InterruptedException {
        Customer c1 = new Customer("CUST-303", "Dev", "dev@example.com");
        Customer c2 = new Customer("CUST-304", "Eve", "eve@example.com");
        Account a = new SavingsAccount("SAV-303", c1, 10000);
        Account b = new SavingsAccount("SAV-304", c2, 10000);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch doneGate = new CountDownLatch(2);

        pool.submit(() -> {
            for (int i = 0; i < 200; i++) {
                try { a.transfer(b, 5); } catch (Exception ignored) { }
            }
            doneGate.countDown();
        });
        pool.submit(() -> {
            for (int i = 0; i < 200; i++) {
                try { b.transfer(a, 5); } catch (Exception ignored) { }
            }
            doneGate.countDown();
        });

        boolean finished = doneGate.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertTrue(finished, "Opposite-direction concurrent transfers deadlocked");
        assertEquals(20000.0, a.getBalance() + b.getBalance(), 0.001);
    }
}
