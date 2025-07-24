package com.gradientgeeks.ageis.backendapp.service;

import com.gradientgeeks.ageis.backendapp.dto.TransferRequest;
import com.gradientgeeks.ageis.backendapp.dto.TransferResponse;
import com.gradientgeeks.ageis.backendapp.entity.Account;
import com.gradientgeeks.ageis.backendapp.entity.Transaction;
import com.gradientgeeks.ageis.backendapp.entity.TransactionStatus;
import com.gradientgeeks.ageis.backendapp.entity.TransactionType;
import com.gradientgeeks.ageis.backendapp.exception.InvalidTransactionException;
import com.gradientgeeks.ageis.backendapp.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for processing financial transactions.
 */
@Service
@Transactional
public class TransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    
    public TransactionService(TransactionRepository transactionRepository, AccountService accountService) {
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
    }
    
    /**
     * Processes a money transfer between accounts.
     * Uses pessimistic locking to prevent race conditions.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse processTransfer(TransferRequest request, String deviceId, 
                                          String signature, String nonce, 
                                          LocalDateTime signatureTimestamp) {
        logger.info("Processing transfer from {} to {} for amount {}", 
                   request.getFromAccount(), request.getToAccount(), request.getAmount());
        
        // Validate the transfer request
        validateTransferRequest(request);
        
        // Create transaction record
        Transaction transaction = createTransaction(request, deviceId, signature, nonce, signatureTimestamp);
        
        try {
            // Get accounts with pessimistic lock to prevent concurrent modifications
            Account fromAccount = accountService.getAccountWithLock(request.getFromAccount());
            Account toAccount = accountService.getAccountWithLock(request.getToAccount());
            
            // Validate accounts
            validateAccounts(fromAccount, toAccount, request);
            
            // Update transaction with account IDs
            transaction.setFromAccountId(fromAccount.getId());
            transaction.setToAccountId(toAccount.getId());
            transaction.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(transaction);
            
            // Perform the transfer
            accountService.updateBalance(fromAccount, request.getAmount().negate());
            accountService.updateBalance(toAccount, request.getAmount());
            
            // Mark transaction as completed
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            
            logger.info("Transfer completed successfully: {}", transaction.getTransactionReference());
            
            return new TransferResponse(
                    transaction.getTransactionReference(),
                    transaction.getStatus().name(),
                    transaction.getAmount(),
                    transaction.getCurrency(),
                    request.getFromAccount(),
                    request.getToAccount(),
                    transaction.getCompletedAt(),
                    "Transfer completed successfully"
            );
            
        } catch (Exception e) {
            // Mark transaction as failed
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setRemarks("Error: " + e.getMessage());
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            
            logger.error("Transfer failed: {}", e.getMessage(), e);
            throw new InvalidTransactionException("Transfer failed: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves transaction history for an account.
     */
    public Page<Transaction> getTransactionHistory(String accountNumber, Pageable pageable) {
        Account account = accountService.getAccountWithLock(accountNumber);
        return transactionRepository.findByAccountId(account.getId(), pageable);
    }
    
    /**
     * Retrieves a transaction by reference number.
     */
    public Transaction getTransactionByReference(String transactionReference) {
        return transactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found: " + transactionReference));
    }
    
    /**
     * Creates a new transaction record.
     */
    private Transaction createTransaction(TransferRequest request, String deviceId, 
                                        String signature, String nonce, 
                                        LocalDateTime signatureTimestamp) {
        Transaction transaction = new Transaction();
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setDescription(request.getDescription());
        transaction.setRemarks(request.getRemarks());
        transaction.setDeviceId(deviceId);
        transaction.setSignature(signature);
        transaction.setNonce(nonce);
        transaction.setSignatureTimestamp(signatureTimestamp);
        transaction.setStatus(TransactionStatus.PENDING);
        
        // Don't save yet - we need to set the account IDs first
        return transaction;
    }
    
    /**
     * Validates a transfer request.
     */
    private void validateTransferRequest(TransferRequest request) {
        if (request.getFromAccount().equals(request.getToAccount())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }
        
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be positive");
        }
        
        if (request.getAmount().compareTo(new BigDecimal("1000000")) > 0) {
            throw new InvalidTransactionException("Transfer amount exceeds maximum limit");
        }
    }
    
    /**
     * Validates accounts for transfer.
     */
    private void validateAccounts(Account fromAccount, Account toAccount, TransferRequest request) {
        if (!fromAccount.getCurrency().equals(request.getCurrency()) || 
            !toAccount.getCurrency().equals(request.getCurrency())) {
            throw new InvalidTransactionException("Currency mismatch");
        }
        
        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InvalidTransactionException("Insufficient balance");
        }
    }
}