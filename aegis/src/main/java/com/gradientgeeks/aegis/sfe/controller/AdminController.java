package com.gradientgeeks.aegis.sfe.controller;

import com.gradientgeeks.aegis.sfe.dto.*;
import com.gradientgeeks.aegis.sfe.entity.User;
import com.gradientgeeks.aegis.sfe.service.AdminService;
import com.gradientgeeks.aegis.sfe.service.RegistrationKeyService;
import com.gradientgeeks.aegis.sfe.util.SecurityUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final RegistrationKeyService registrationKeyService;
    private final SecurityUtils securityUtils;
    private final AdminService adminService;
    
    @Autowired
    public AdminController(RegistrationKeyService registrationKeyService, SecurityUtils securityUtils, AdminService adminService) {
        this.registrationKeyService = registrationKeyService;
        this.securityUtils = securityUtils;
        this.adminService = adminService;
    }
    
    @PostMapping("/registration-keys")
    public ResponseEntity<RegistrationKeyResponse> generateRegistrationKey(
            @Valid @RequestBody RegistrationKeyRequest request) {
        
        String organization = securityUtils.getCurrentUserOrganization();
        if (organization == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Prevent admin users from creating registration keys
        if (securityUtils.isAdmin()) {
            logger.warn("Admin user attempted to create registration key");
            RegistrationKeyResponse errorResponse = new RegistrationKeyResponse(
                "error", "Admin users cannot create registration keys");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
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
                // Hide registration keys from admin
                keys.forEach(key -> key.setRegistrationKey(null));
                logger.info("Admin retrieved all {} registration keys (keys hidden)", keys.size());
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
                // Hide registration key from admin
                if (response.isPresent()) {
                    response.get().setRegistrationKey(null);
                }
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
        
        // Prevent admin users from revoking registration keys
        if (securityUtils.isAdmin()) {
            logger.warn("Admin user attempted to revoke registration key");
            RegistrationKeyResponse errorResponse = new RegistrationKeyResponse(
                "error", "Admin users cannot revoke registration keys");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
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
        
        // Prevent admin users from regenerating registration keys
        if (securityUtils.isAdmin()) {
            logger.warn("Admin user attempted to regenerate registration key");
            RegistrationKeyResponse errorResponse = new RegistrationKeyResponse(
                "error", "Admin users cannot regenerate registration keys");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
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
    
    /**
     * Get all organizations pending approval (Admin only)
     * @return List of organizations pending approval
     */
    @GetMapping("/organizations/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrganizationDto>> getPendingOrganizations() {
        logger.info("Admin fetching pending organizations");
        
        try {
            List<OrganizationDto> organizations = adminService.getPendingOrganizations();
            logger.info("Found {} pending organizations", organizations.size());
            return ResponseEntity.ok(organizations);
        } catch (Exception e) {
            logger.error("Error fetching pending organizations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all organizations (Admin only)
     * @return List of all organizations
     */
    @GetMapping("/organizations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrganizationDto>> getAllOrganizations() {
        logger.info("Admin fetching all organizations");
        
        try {
            List<OrganizationDto> organizations = adminService.getAllOrganizations();
            logger.info("Found {} organizations", organizations.size());
            return ResponseEntity.ok(organizations);
        } catch (Exception e) {
            logger.error("Error fetching organizations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Approve an organization (Admin only)
     * @param userId the user ID to approve
     * @param approvalRequest approval details
     * @return Updated organization details
     */
    @PostMapping("/organizations/{userId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> approveOrganization(
            @PathVariable Long userId,
            @Valid @RequestBody ApprovalRequest approvalRequest) {
        
        logger.info("Admin approving organization for user ID: {}", userId);
        
        try {
            OrganizationDto approvedOrg = adminService.approveOrganization(userId, approvalRequest.getApprovedBy());
            logger.info("Organization approved successfully: {}", approvedOrg.getOrganization());
            return ResponseEntity.ok(approvedOrg);
        } catch (RuntimeException e) {
            logger.error("Failed to approve organization: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Reject an organization (Admin only)
     * @param userId the user ID to reject
     * @param approvalRequest rejection details
     * @return Updated organization details
     */
    @PostMapping("/organizations/{userId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rejectOrganization(
            @PathVariable Long userId,
            @Valid @RequestBody ApprovalRequest approvalRequest) {
        
        logger.info("Admin rejecting organization for user ID: {}", userId);
        
        try {
            OrganizationDto rejectedOrg = adminService.rejectOrganization(userId, approvalRequest.getApprovedBy());
            logger.info("Organization rejected successfully: {}", rejectedOrg.getOrganization());
            return ResponseEntity.ok(rejectedOrg);
        } catch (RuntimeException e) {
            logger.error("Failed to reject organization: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            error.put("status", "error");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
}