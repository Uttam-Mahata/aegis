package com.gradientgeeks.aegis.sfe.controller;

import com.gradientgeeks.aegis.sfe.dto.RegistrationKeyRequest;
import com.gradientgeeks.aegis.sfe.dto.RegistrationKeyResponse;
import com.gradientgeeks.aegis.sfe.service.RegistrationKeyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final RegistrationKeyService registrationKeyService;
    
    @Autowired
    public AdminController(RegistrationKeyService registrationKeyService) {
        this.registrationKeyService = registrationKeyService;
    }
    
    @PostMapping("/registration-keys")
    public ResponseEntity<RegistrationKeyResponse> generateRegistrationKey(
            @Valid @RequestBody RegistrationKeyRequest request) {
        
        logger.info("Admin request to generate registration key for clientId: {}", request.getClientId());
        
        try {
            RegistrationKeyResponse response = registrationKeyService.generateRegistrationKey(request);
            
            if ("success".equals(response.getStatus())) {
                logger.info("Registration key generated successfully for clientId: {}", request.getClientId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                logger.warn("Failed to generate registration key for clientId: {} - {}", 
                    request.getClientId(), response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error generating registration key for clientId: {}", 
                request.getClientId(), e);
            
            RegistrationKeyResponse errorResponse = new RegistrationKeyResponse(
                "error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/registration-keys")
    public ResponseEntity<List<RegistrationKeyResponse>> getAllRegistrationKeys() {
        logger.info("Admin request to retrieve all registration keys");
        
        try {
            List<RegistrationKeyResponse> keys = registrationKeyService.getAllRegistrationKeys();
            logger.info("Retrieved {} registration keys", keys.size());
            return ResponseEntity.ok(keys);
            
        } catch (Exception e) {
            logger.error("Error retrieving registration keys", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/registration-keys/{clientId}")
    public ResponseEntity<RegistrationKeyResponse> getRegistrationKey(@PathVariable String clientId) {
        logger.info("Admin request to retrieve registration key for clientId: {}", clientId);
        
        try {
            Optional<RegistrationKeyResponse> response = registrationKeyService
                .getRegistrationKeyByClientId(clientId);
            
            if (response.isPresent()) {
                logger.info("Registration key found for clientId: {}", clientId);
                return ResponseEntity.ok(response.get());
            } else {
                logger.warn("Registration key not found for clientId: {}", clientId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving registration key for clientId: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/registration-keys/{clientId}/revoke")
    public ResponseEntity<RegistrationKeyResponse> revokeRegistrationKey(@PathVariable String clientId) {
        logger.info("Admin request to revoke registration key for clientId: {}", clientId);
        
        try {
            RegistrationKeyResponse response = registrationKeyService.revokeRegistrationKey(clientId);
            
            if ("success".equals(response.getStatus()) || response.getStatus() == null) {
                logger.info("Registration key revoked successfully for clientId: {}", clientId);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Failed to revoke registration key for clientId: {} - {}", 
                    clientId, response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error revoking registration key for clientId: {}", clientId, e);
            
            RegistrationKeyResponse errorResponse = new RegistrationKeyResponse(
                "error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PutMapping("/registration-keys/{clientId}/regenerate")
    public ResponseEntity<RegistrationKeyResponse> regenerateRegistrationKey(@PathVariable String clientId) {
        logger.info("Admin request to regenerate registration key for clientId: {}", clientId);
        
        try {
            RegistrationKeyResponse response = registrationKeyService.regenerateRegistrationKey(clientId);
            
            if ("success".equals(response.getStatus()) || response.getStatus() == null) {
                logger.info("Registration key regenerated successfully for clientId: {}", clientId);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Failed to regenerate registration key for clientId: {} - {}", 
                    clientId, response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error regenerating registration key for clientId: {}", clientId, e);
            
            RegistrationKeyResponse errorResponse = new RegistrationKeyResponse(
                "error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> adminHealth() {
        return ResponseEntity.ok("Aegis Admin API is running");
    }
}