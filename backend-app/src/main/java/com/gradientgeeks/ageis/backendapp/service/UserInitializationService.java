package com.gradientgeeks.ageis.backendapp.service;

import com.gradientgeeks.ageis.backendapp.entity.User;
import com.gradientgeeks.ageis.backendapp.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to initialize sample users for demo purposes.
 */
@Service
public class UserInitializationService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInitializationService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    public UserInitializationService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @PostConstruct
    @Transactional
    public void initializeSampleUsers() {
        logger.info("Checking for sample users...");
        
        // Create sample user 1
        if (!userRepository.existsByUsername("demo1")) {
            User user1 = new User();
            user1.setUsername("demo1");
            user1.setPasswordHash(passwordEncoder.encode("password123"));
            user1.setEmail("demo1@ucobank.com");
            user1.setFullName("Anurag Sharma");
            user1.setPhoneNumber("+919876543210");
            user1.setIsActive(true);
            userRepository.save(user1);
            logger.info("Created sample user: demo1");
        }
        
        // Create sample user 2
        if (!userRepository.existsByUsername("demo2")) {
            User user2 = new User();
            user2.setUsername("demo2");
            user2.setPasswordHash(passwordEncoder.encode("password123"));
            user2.setEmail("demo2@ucobank.com");
            user2.setFullName("Priya Patel");
            user2.setPhoneNumber("+919876543211");
            user2.setIsActive(true);
            userRepository.save(user2);
            logger.info("Created sample user: demo2");
        }
        
        // Create admin user
        if (!userRepository.existsByUsername("admin")) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPasswordHash(passwordEncoder.encode("admin123"));
            adminUser.setEmail("admin@ucobank.com");
            adminUser.setFullName("Bank Administrator");
            adminUser.setPhoneNumber("+919876543220");
            adminUser.setIsActive(true);
            userRepository.save(adminUser);
            logger.info("Created admin user");
        }
        
        logger.info("Sample users initialization completed");
    }
}