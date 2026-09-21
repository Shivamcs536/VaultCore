package com.vaultcore;

import com.vaultcore.exceptions.InvalidAccountException;
import com.vaultcore.model.Account;
import com.vaultcore.model.CurrentAccount;
import com.vaultcore.model.Customer;
import com.vaultcore.model.SavingsAccount;
import com.vaultcore.service.AccountFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountFactoryTest {

    private final Customer owner = new Customer("CUST-400", "Frank", "frank@example.com");

    @Test
    void createsSavingsAccountForSavingsType() {
        Account acc = AccountFactory.createAccount("SAVINGS", "SAV-400", owner, 1000);
        assertInstanceOf(SavingsAccount.class, acc);
        assertEquals("Savings", acc.getAccountType());
    }

    @Test
    void createsCurrentAccountForCurrentType() {
        Account acc = AccountFactory.createAccount("CURRENT", "CUR-400", owner, 1000);
        assertInstanceOf(CurrentAccount.class, acc);
        assertEquals("Current", acc.getAccountType());
    }

    @Test
    void isCaseInsensitiveAndTrimsWhitespace() {
        Account acc = AccountFactory.createAccount("  savings ", "SAV-401", owner, 1000);
        assertInstanceOf(SavingsAccount.class, acc);
    }

    @Test
    void rejectsUnknownAccountType() {
        assertThrows(InvalidAccountException.class,
                () -> AccountFactory.createAccount("CRYPTO", "X-1", owner, 1000));
    }

    @Test
    void rejectsNullAccountType() {
        assertThrows(InvalidAccountException.class,
                () -> AccountFactory.createAccount(null, "X-2", owner, 1000));
    }
}
