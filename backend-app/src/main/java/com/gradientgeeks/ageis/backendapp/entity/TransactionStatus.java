package com.gradientgeeks.ageis.backendapp.entity;

/**
 * Enum representing the status of a transaction.
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED,
    REVERSED
}