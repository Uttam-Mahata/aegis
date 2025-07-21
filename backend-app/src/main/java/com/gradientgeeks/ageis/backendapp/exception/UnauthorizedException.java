package com.gradientgeeks.ageis.backendapp.exception;

/**
 * Exception thrown when a request is unauthorized.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}