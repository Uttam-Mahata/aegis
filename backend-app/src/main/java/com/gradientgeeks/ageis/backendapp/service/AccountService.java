package com.gradientgeeks.ageis.backendapp.service;

import com.gradientgeeks.ageis.backendapp.dto.AccountResponse;
import com.gradientgeeks.ageis.backendapp.entity.Account;
import com.gradientgeeks.ageis.backendapp.entity.AccountStatus;
import com.gradientgeeks.ageis.backendapp.entity.AccountType;
import com.gradientgeeks.ageis.backendapp.exception.AccountNotFoundException;
import com.gradientgeeks.ageis.backendapp.exception.InsufficientBalanceException;
import com.gradientgeeks.ageis.backendapp.repository.AccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing bank accounts.
 */
@Service
@Transactional
public class AccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    
    private final AccountRepository accountRepository;
    
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    /**
     * Creates a new account.
     */
    public Account createAccount(String userId, String accountHolderName, AccountType accountType) {
        Account account = new Account();
        account.setUserId(userId);
        account.setAccountHolderName(accountHolderName);
        account.setAccountType(accountType);
        account.setAccountNumber(generateAccountNumber());
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(BigDecimal.ZERO);
        account.setCurrency("INR");
        
        Account savedAccount = accountRepository.save(account);
        logger.info("Created new account: {} for user: {}", savedAccount.getAccountNumber(), userId);
        
        return savedAccount;
    }
    
    /**
     * Retrieves account by account number.
     */
    @Cacheable(value = "accounts", key = "#accountNumber")
    public AccountResponse getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        
        return mapToAccountResponse(account);
    }
    
    /**
     * Retrieves all accounts for a user.
     */
    public List<AccountResponse> getAccountsByUserId(String userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        return accounts.stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets account with pessimistic lock for transaction processing.
     */
    public Account getAccountWithLock(String accountNumber) {
        return accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }
    
    /**
     * Updates account balance.
     */
    public void updateBalance(Account account, BigDecimal amount) {
        BigDecimal newBalance = account.getBalance().add(amount);
        
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account: " + account.getAccountNumber());
        }
        
        account.setBalance(newBalance);
        accountRepository.save(account);
        logger.debug("Updated balance for account: {} to: {}", account.getAccountNumber(), newBalance);
    }
    
    /**
     * Validates if an account exists and is active.
     */
    public boolean validateAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(account -> account.getStatus() == AccountStatus.ACTIVE)
                .orElse(false);
    }
    
    /**
     * Associates a device ID with an account.
     */
    public void associateDevice(String accountNumber, String deviceId) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        
        account.setDeviceId(deviceId);
        accountRepository.save(account);
        logger.info("Associated device {} with account {}", deviceId, accountNumber);
    }
    
    /**
     * Generates a unique account number.
     */
    private String generateAccountNumber() {
        String accountNumber;
        do {
            // Generate a 12-digit account number
            accountNumber = String.format("%012d", (long) (Math.random() * 1000000000000L));
        } while (accountRepository.existsByAccountNumber(accountNumber));
        
        return accountNumber;
    }
    
    /**
     * Maps Account entity to AccountResponse DTO.
     */
    private AccountResponse mapToAccountResponse(Account account) {
        return new AccountResponse(
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getBalance(),
                account.getCurrency(),
                account.getAccountType().name(),
                account.getStatus().name(),
                account.getCreatedAt()
        );
    }
}