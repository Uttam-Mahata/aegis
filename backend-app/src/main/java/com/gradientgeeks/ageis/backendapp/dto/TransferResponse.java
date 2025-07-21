package com.gradientgeeks.ageis.backendapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for money transfer response.
 */
public class TransferResponse {
    
    private String transactionReference;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String fromAccount;
    private String toAccount;
    private LocalDateTime timestamp;
    private String message;
    
    // Constructors
    public TransferResponse() {
    }
    
    public TransferResponse(String transactionReference, String status, BigDecimal amount, 
                          String currency, String fromAccount, String toAccount, 
                          LocalDateTime timestamp, String message) {
        this.transactionReference = transactionReference;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.timestamp = timestamp;
        this.message = message;
    }
    
    // Getters and Setters
    public String getTransactionReference() {
        return transactionReference;
    }
    
    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getFromAccount() {
        return fromAccount;
    }
    
    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }
    
    public String getToAccount() {
        return toAccount;
    }
    
    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}