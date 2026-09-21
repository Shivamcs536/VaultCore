package com.vaultcore;

import com.vaultcore.exceptions.InvalidAccountException;
import com.vaultcore.model.Account;
import com.vaultcore.model.Customer;
import com.vaultcore.service.BankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {

    private BankService bank;
    private Customer owner;

    @BeforeEach
    void setUp() {
        bank = new BankService();
        owner = new Customer("CUST-500", "Grace", "grace@example.com");
    }

    @Test
    void opensAndFindsAccount() {
        bank.openAccount("SAVINGS", "SAV-500", owner, 1000);
        Account found = bank.findAccount("SAV-500");
        assertEquals("SAV-500", found.getAccountNumber());
    }

    @Test
    void rejectsDuplicateAccountNumber() {
        bank.openAccount("SAVINGS", "SAV-501", owner, 1000);
        assertThrows(InvalidAccountException.class,
                () -> bank.openAccount("CURRENT", "SAV-501", owner, 500));
    }

    @Test
    void findingUnknownAccountThrows() {
        assertThrows(InvalidAccountException.class, () -> bank.findAccount("NO-SUCH-ACCOUNT"));
    }

    @Test
    void monthlyInterestAppliesPolymorphicallyPerAccountType() {
        Account savings = bank.openAccount("SAVINGS", "SAV-502", owner, 1000);
        Account current = bank.openAccount("CURRENT", "CUR-502", owner, 1000);

        bank.applyMonthlyInterest(); // no instanceof checks inside - relies on each type's own calculateInterest()

        assertEquals(1040.0, savings.getBalance(), 0.001); // +4% interest
        assertEquals(1000.0, current.getBalance(), 0.001); // current accounts earn nothing
    }
}
