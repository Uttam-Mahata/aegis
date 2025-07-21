package com.gradientgeeks.ageis.backendapp.controller;

import com.gradientgeeks.ageis.backendapp.dto.AccountResponse;
import com.gradientgeeks.ageis.backendapp.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for account management operations.
 */
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    
    private final AccountService accountService;
    
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }
    
    /**
     * Get account details by account number.
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber,
                                                    HttpServletRequest request) {
        String deviceId = (String) request.getAttribute("deviceId");
        logger.info("Account details requested for {} by device {}", accountNumber, deviceId);
        
        AccountResponse account = accountService.getAccountByNumber(accountNumber);
        
        // Associate device with account if not already done
        accountService.associateDevice(accountNumber, deviceId);
        
        return ResponseEntity.ok(account);
    }
    
    /**
     * Get all accounts for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AccountResponse>> getUserAccounts(@PathVariable String userId,
                                                               HttpServletRequest request) {
        String deviceId = (String) request.getAttribute("deviceId");
        logger.info("Account list requested for user {} by device {}", userId, deviceId);
        
        List<AccountResponse> accounts = accountService.getAccountsByUserId(userId);
        return ResponseEntity.ok(accounts);
    }
    
    /**
     * Check if an account exists and is active.
     */
    @GetMapping("/{accountNumber}/validate")
    public ResponseEntity<Boolean> validateAccount(@PathVariable String accountNumber) {
        boolean isValid = accountService.validateAccount(accountNumber);
        return ResponseEntity.ok(isValid);
    }
}