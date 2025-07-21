package com.gradientgeeks.ageis.backendapp.service;

import com.gradientgeeks.ageis.backendapp.entity.Account;
import com.gradientgeeks.ageis.backendapp.entity.AccountType;
import com.gradientgeeks.ageis.backendapp.repository.AccountRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service to initialize sample data for demo purposes.
 */
@Service
@Transactional
public class DataInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    
    private final AccountRepository accountRepository;
    
    public DataInitializationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    @PostConstruct
    public void initializeData() {
        // Check if data already exists
        if (accountRepository.count() > 0) {
            logger.info("Data already initialized, skipping...");
            return;
        }
        
        logger.info("Initializing sample data...");
        
        // Create sample accounts
        createSampleAccount("123456789012", "USER001", "Anurag Sharma", BigDecimal.valueOf(50000), AccountType.SAVINGS);
        createSampleAccount("123456789013", "USER001", "Anurag Sharma", BigDecimal.valueOf(100000), AccountType.CURRENT);
        createSampleAccount("987654321098", "USER002", "Priya Patel", BigDecimal.valueOf(75000), AccountType.SAVINGS);
        createSampleAccount("987654321099", "USER003", "Rajesh Kumar", BigDecimal.valueOf(25000), AccountType.SAVINGS);
        
        logger.info("Sample data initialization completed");
    }
    
    private void createSampleAccount(String accountNumber, String userId, String holderName, 
                                   BigDecimal balance, AccountType type) {
        Account account = new Account();
        account.setAccountNumber(accountNumber);
        account.setUserId(userId);
        account.setAccountHolderName(holderName);
        account.setBalance(balance);
        account.setAccountType(type);
        account.setCurrency("INR");
        
        accountRepository.save(account);
        logger.info("Created sample account: {} for {}", accountNumber, holderName);
    }
}