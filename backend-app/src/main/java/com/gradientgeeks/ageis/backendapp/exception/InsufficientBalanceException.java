package com.gradientgeeks.ageis.backendapp.exception;

/**
 * Exception thrown when an account has insufficient balance for a transaction.
 */
public class InsufficientBalanceException extends RuntimeException {
    
    public InsufficientBalanceException(String message) {
        super(message);
    }
    
    public InsufficientBalanceException(String message, Throwable cause) {
        super(message, cause);
    }
}