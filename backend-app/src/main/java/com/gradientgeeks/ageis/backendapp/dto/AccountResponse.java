package com.gradientgeeks.ageis.backendapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for account information response.
 */
public class AccountResponse {
    
    private String accountNumber;
    private String accountHolderName;
    private BigDecimal balance;
    private String currency;
    private String accountType;
    private String status;
    private LocalDateTime createdAt;
    
    // Constructors
    public AccountResponse() {
    }
    
    public AccountResponse(String accountNumber, String accountHolderName, BigDecimal balance, 
                         String currency, String accountType, String status, LocalDateTime createdAt) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = balance;
        this.currency = currency;
        this.accountType = accountType;
        this.status = status;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}