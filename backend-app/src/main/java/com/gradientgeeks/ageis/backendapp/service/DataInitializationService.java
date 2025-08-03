package com.gradientgeeks.ageis.backendapp.service;

import com.gradientgeeks.ageis.backendapp.entity.Account;
import com.gradientgeeks.ageis.backendapp.entity.AccountType;
import com.gradientgeeks.ageis.backendapp.entity.User;
import com.gradientgeeks.ageis.backendapp.repository.AccountRepository;
import com.gradientgeeks.ageis.backendapp.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service to initialize sample data for demo purposes.
 */
@Service
@Transactional
@org.springframework.context.annotation.DependsOn("userInitializationService")
public class DataInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    
    public DataInitializationService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }
    
    @PostConstruct
    public void initializeData() {
        // Check if data already exists
        if (accountRepository.count() > 0) {
            logger.info("Data already initialized, skipping...");
            return;
        }
        
        logger.info("Initializing sample data...");
        
        // Get the actual user IDs from the database
        Optional<User> demo1Opt = userRepository.findByUsername("demo1");
        Optional<User> demo2Opt = userRepository.findByUsername("demo2");
        
        User demo1 = demo1Opt.orElse(null);
        User demo2 = demo2Opt.orElse(null);
        
        if (demo1 != null) {
            // Create accounts for demo1 user
            createSampleAccount("123456789012", String.valueOf(demo1.getId()), "Anurag Sharma", 
                              BigDecimal.valueOf(50000), AccountType.SAVINGS);
            createSampleAccount("123456789013", String.valueOf(demo1.getId()), "Anurag Sharma", 
                              BigDecimal.valueOf(100000), AccountType.CURRENT);
            logger.info("Created accounts for user demo1 with ID: {}", demo1.getId());
        }
        
        if (demo2 != null) {
            // Create account for demo2 user
            createSampleAccount("987654321098", String.valueOf(demo2.getId()), "Priya Patel", 
                              BigDecimal.valueOf(75000), AccountType.SAVINGS);
            logger.info("Created account for user demo2 with ID: {}", demo2.getId());
        }
        
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