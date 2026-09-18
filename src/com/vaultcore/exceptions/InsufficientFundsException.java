package com.vaultcore.exceptions;

/**
 * Checked exception thrown when a withdrawal or transfer would push an
 * account below its allowed minimum balance.
 */
public class InsufficientFundsException extends Exception {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
