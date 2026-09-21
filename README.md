# VaultCore — Concurrent Banking System (Java)

A small console-based banking engine written in pure Java, built specifically
to *show* OOP concepts rather than bury them — good for a resume project
where you'll be asked to explain your own code line by line.

## Why this project
Most student "bank management system" projects are single-threaded CRUD apps.
VaultCore adds a genuine concurrency angle (multiple threads transacting on
the same accounts at once, with correctness verified) on top of a clean OOP
model, which gives you something specific and defensible to talk about in an
interview.

## OOP concepts demonstrated

| Concept | Where |
|---|---|
| **Encapsulation** | `Account.balance` is `private`; only touched via `deposit()`/`withdraw()`/`transfer()` |
| **Abstraction** | `Account` is `abstract`; callers work with `calculateInterest()` without knowing how each type computes it |
| **Inheritance** | `SavingsAccount` / `CurrentAccount` extend `Account`; `PremiumCustomer` extends `Customer` |
| **Polymorphism** | `BankService.applyMonthlyInterest()` calls `calculateInterest()` on every account with no `instanceof` checks — each subtype resolves it differently |
| **Interfaces** | `Transactable` — the deposit/withdraw/transfer contract every account implements |
| **Custom exceptions** | `InsufficientFundsException` (checked), `InvalidAccountException` (unchecked) |
| **Design patterns** | Factory (`AccountFactory`), Singleton (`TransactionLogger`, double-checked locking) |
| **Concurrency** | `synchronized` methods on `Account`, deadlock-safe lock ordering in `transfer()`, a thread pool hammering the same two accounts in `Main` |

## Project structure
```
src/com/vaultcore/
├── Main.java                     # demo entry point
├── model/
│   ├── Account.java              # abstract base
│   ├── SavingsAccount.java
│   ├── CurrentAccount.java
│   ├── Customer.java
│   └── PremiumCustomer.java
├── interfaces/
│   └── Transactable.java
├── exceptions/
│   ├── InsufficientFundsException.java
│   └── InvalidAccountException.java
└── service/
    ├── AccountFactory.java       # Factory pattern
    ├── BankService.java          # account registry / manager
    └── TransactionLogger.java    # Singleton pattern, thread-safe logging
```

## How to run
Requires a JDK (11+ is fine; tested on 21).

```bash
# from the project root
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out com.vaultcore.Main
```

## Running the tests
The `test/` folder has a JUnit 5 suite (30 tests) covering deposits, withdrawals,
transfers, the Factory/BankService layer, and — most importantly — concurrency:
100 threads transferring between the same two accounts at once, 50 threads
depositing into the same account at once, and two threads transferring in
opposite directions on purpose (the classic deadlock setup) to prove the
lock ordering in `Account.transfer()` actually prevents it.

The JUnit console-standalone jar is bundled in `lib/` so no build tool or
internet access is needed:

```bash
find src -name "*.java" > sources.txt
find test -name "*.java" > test_sources.txt
javac -d out @sources.txt
javac -cp "out:lib/junit-platform-console-standalone-1.9.1.jar" -d test-out @test_sources.txt
java -jar lib/junit-platform-console-standalone-1.9.1.jar \
  --class-path "out:test-out" --scan-class-path "test-out"
```

(On Windows, use `;` instead of `:` in the `--class-path` value.)

**In IntelliJ:** right-click the `test` folder → "Run All Tests". IntelliJ
will offer to add JUnit to the module's classpath automatically the first
time — accept it, or point it at the jar in `lib/`.

This will:
1. Open a `SavingsAccount` and a `CurrentAccount` for two customers (one premium).
2. Run a few single-threaded sanity checks, including a deliberately-failing
   withdrawal to show the custom exception.
3. Fire 10 concurrent transactions across a 4-thread pool against the *same*
   two accounts, then print the total balance before/after to prove no money
   was lost or duplicated.
4. Apply monthly interest polymorphically across all accounts.
5. Print final balances and the full transaction log (also written to
   `transactions.log`).

## Possible extensions 
- Swap the in-memory `BankService` map for JDBC + MySQL persistence.
- Add a `LoanAccount` type to show the Factory/abstract-class design scales.
- Expose it over a small REST layer (Spark/Javalin) instead of a console demo.
- Replace the manual lock-ordering in `transfer()` with `ReentrantLock.tryLock()`
  and a timeout, and talk through why that trade-off matters.
