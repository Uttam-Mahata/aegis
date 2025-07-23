package com.gradientgeeks.aegis.sfe.controller;

import com.gradientgeeks.aegis.sfe.dto.RegistrationKeyRequest;
import com.gradientgeeks.aegis.sfe.dto.RegistrationKeyResponse;
import com.gradientgeeks.aegis.sfe.service.RegistrationKeyService;
import com.gradientgeeks.aegis.sfe.util.SecurityUtils;
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
    private final SecurityUtils securityUtils;
    
    @Autowired
    public AdminController(RegistrationKeyService registrationKeyService, SecurityUtils securityUtils) {
        this.registrationKeyService = registrationKeyService;
        this.securityUtils = securityUtils;
    }
    
    @PostMapping("/registration-keys")
    public ResponseEntity<RegistrationKeyResponse> generateRegistrationKey(
            @Valid @RequestBody RegistrationKeyRequest request) {
        
        String organization = securityUtils.getCurrentUserOrganization();
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        logger.info("Request to generate registration key for clientId: {} by organization: {}", 
            request.getClientId(), organization);
        
        try {
            // Set the organization for the request
            request.setOrganization(organization);
            
            RegistrationKeyResponse response = registrationKeyService.generateRegistrationKey(request);
            
            if ("success".equals(response.getStatus())) {
                logger.info("Registration key generated successfully for clientId: {} by organization: {}", 
                    request.getClientId(), organization);
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
        String organization = securityUtils.getCurrentUserOrganization();
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        logger.info("Request to retrieve registration keys for organization: {}", organization);
        
        try {
            // Admin users can see all keys, regular users only see their organization's keys
            List<RegistrationKeyResponse> keys;
            if (securityUtils.isAdmin()) {
                keys = registrationKeyService.getAllRegistrationKeys();
                logger.info("Admin retrieved all {} registration keys", keys.size());
            } else {
                keys = registrationKeyService.getRegistrationKeysByOrganization(organization);
                logger.info("Retrieved {} registration keys for organization: {}", keys.size(), organization);
            }
            
            return ResponseEntity.ok(keys);
            
        } catch (Exception e) {
            logger.error("Error retrieving registration keys", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/registration-keys/{clientId}")
    public ResponseEntity<RegistrationKeyResponse> getRegistrationKey(@PathVariable String clientId) {
        String organization = securityUtils.getCurrentUserOrganization();
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        logger.info("Request to retrieve registration key for clientId: {} by organization: {}", 
            clientId, organization);
        
        try {
            Optional<RegistrationKeyResponse> response;
            
            // Admin can access any key, regular users only their organization's keys
            if (securityUtils.isAdmin()) {
                response = registrationKeyService.getRegistrationKeyByClientId(clientId);
            } else {
                response = registrationKeyService.getRegistrationKeyByClientIdAndOrganization(
                    clientId, organization);
            }
            
            if (response.isPresent()) {
                logger.info("Registration key found for clientId: {}", clientId);
                return ResponseEntity.ok(response.get());
            } else {
                logger.warn("Registration key not found or access denied for clientId: {}", clientId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving registration key for clientId: {}", clientId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/registration-keys/{clientId}/revoke")
    public ResponseEntity<RegistrationKeyResponse> revokeRegistrationKey(@PathVariable String clientId) {
        String organization = securityUtils.getCurrentUserOrganization();
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        logger.info("Request to revoke registration key for clientId: {} by organization: {}", 
            clientId, organization);
        
        try {
            // Check if user has access to this key
            if (!securityUtils.isAdmin()) {
                Optional<RegistrationKeyResponse> keyCheck = registrationKeyService
                    .getRegistrationKeyByClientIdAndOrganization(clientId, organization);
                if (keyCheck.isEmpty()) {
                    logger.warn("Access denied to revoke key for clientId: {} by organization: {}", 
                        clientId, organization);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            
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
        String organization = securityUtils.getCurrentUserOrganization();
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        logger.info("Request to regenerate registration key for clientId: {} by organization: {}", 
            clientId, organization);
        
        try {
            // Check if user has access to this key
            if (!securityUtils.isAdmin()) {
                Optional<RegistrationKeyResponse> keyCheck = registrationKeyService
                    .getRegistrationKeyByClientIdAndOrganization(clientId, organization);
                if (keyCheck.isEmpty()) {
                    logger.warn("Access denied to regenerate key for clientId: {} by organization: {}", 
                        clientId, organization);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }
            
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