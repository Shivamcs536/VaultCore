package com.vaultcore.exceptions;

/**
 * Unchecked exception thrown when an unknown account type is requested
 * from the AccountFactory, or an account cannot be located.
 */
public class InvalidAccountException extends RuntimeException {
    public InvalidAccountException(String message) {
        super(message);
    }
}
