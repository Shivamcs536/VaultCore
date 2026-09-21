package com.vaultcore;

import com.vaultcore.exceptions.InsufficientFundsException;
import com.vaultcore.model.Account;
import com.vaultcore.model.Customer;
import com.vaultcore.model.SavingsAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single-threaded transfer tests: the straightforward cases that need to
 * hold before the concurrent version (ConcurrentTransferTest) is trusted.
 */
class TransferTest {

    private Account from;
    private Account to;

    @BeforeEach
    void setUp() {
        Customer c1 = new Customer("CUST-200", "Alice", "alice@example.com");
        Customer c2 = new Customer("CUST-201", "Bob", "bob@example.com");
        from = new SavingsAccount("SAV-200", c1, 5000);
        to = new SavingsAccount("SAV-201", c2, 2000);
    }

    @Test
    void transferMovesMoneyBetweenAccounts() throws InsufficientFundsException {
        from.transfer(to, 1000);
        assertEquals(4000.0, from.getBalance(), 0.001);
        assertEquals(3000.0, to.getBalance(), 0.001);
    }

    @Test
    void transferPreservesTotalBalance() throws InsufficientFundsException {
        double totalBefore = from.getBalance() + to.getBalance();
        from.transfer(to, 1500);
        double totalAfter = from.getBalance() + to.getBalance();
        assertEquals(totalBefore, totalAfter, 0.001);
    }

    @Test
    void transferBelowMinimumBalanceThrowsAndChangesNothing() {
        double fromBefore = from.getBalance();
        double toBefore = to.getBalance();

        assertThrows(InsufficientFundsException.class, () -> from.transfer(to, 100000));

        // A failed transfer must not have moved any money on either side
        assertEquals(fromBefore, from.getBalance(), 0.001);
        assertEquals(toBefore, to.getBalance(), 0.001);
    }

    @Test
    void transferRejectsZeroOrNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> from.transfer(to, 0));
        assertThrows(IllegalArgumentException.class, () -> from.transfer(to, -200));
    }

    @Test
    void transferToNullDestinationThrows() {
        assertThrows(IllegalArgumentException.class, () -> from.transfer(null, 100));
    }

    @Test
    void transferToSameAccountLeavesBalanceUnchanged() throws InsufficientFundsException {
        double before = from.getBalance();
        from.transfer(from, 500); // withdraw 500 then immediately credit 500 back
        assertEquals(before, from.getBalance(), 0.001);
    }
}
