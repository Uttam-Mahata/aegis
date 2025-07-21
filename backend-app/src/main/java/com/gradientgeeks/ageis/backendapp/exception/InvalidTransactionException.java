package com.gradientgeeks.ageis.backendapp.exception;

/**
 * Exception thrown when a transaction is invalid.
 */
public class InvalidTransactionException extends RuntimeException {
    
    public InvalidTransactionException(String message) {
        super(message);
    }
    
    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}