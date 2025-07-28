package com.gradientgeeks.ageis.backendapp.controller;

import com.gradientgeeks.ageis.backendapp.dto.LoginRequest;
import com.gradientgeeks.ageis.backendapp.dto.LoginResponse;
import com.gradientgeeks.ageis.backendapp.dto.DeviceRebindRequest;
import com.gradientgeeks.ageis.backendapp.dto.ErrorResponse;
import com.gradientgeeks.ageis.backendapp.exception.AuthenticationException;
import com.gradientgeeks.ageis.backendapp.service.AuthService;
import com.gradientgeeks.ageis.backendapp.entity.User;
import com.gradientgeeks.ageis.backendapp.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.jwtTokenProvider = jwtTokenProvider;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                 @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        try {
            if (deviceId == null || deviceId.trim().isEmpty()) {
                logger.warn("Login request without device ID for username: {}", loginRequest.getUsername());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Device ID required");
                errorResponse.put("message", "X-Device-Id header is required for authentication");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
            logger.info("Login request received for username: {} from device: {}", 
                loginRequest.getUsername(), deviceId);
            
            LoginResponse loginResponse = authService.login(loginRequest, deviceId);
            return ResponseEntity.ok(loginResponse);
            
        } catch (AuthenticationException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed");
            errorResponse.put("message", e.getMessage());
            
            // Add specific error codes for device binding issues
            if (e.getMessage().contains("device verification") || e.getMessage().contains("new device")) {
                errorResponse.put("errorCode", "DEVICE_VERIFICATION_REQUIRED");
                errorResponse.put("requiresRebinding", "true");
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        } catch (Exception e) {
            logger.error("Unexpected error during login: ", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        try {
            logger.info("Logout request received");
            // In a real application, you would invalidate the JWT token here
            // For now, we'll just return a success response
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Logged out successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during logout: ", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Logout failed");
            errorResponse.put("message", "An error occurred during logout");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Endpoint for device rebinding when a user needs to verify their identity on a new device.
     * This endpoint requires strong identity verification before allowing device migration.
     */
    @PostMapping("/rebind-device")
    public ResponseEntity<?> rebindDevice(
            @Valid @RequestBody DeviceRebindRequest request,
            @RequestHeader("X-Device-Id") String newDeviceId) {
        
        try {
            logger.info("Device rebinding request - User: {}, New Device: {}, Method: {}", 
                request.getUsername(), newDeviceId, request.getVerificationMethod());
            
            // Verify user identity through multiple factors
            boolean identityVerified = verifyUserIdentity(request);
            
            if (!identityVerified) {
                logger.warn("Identity verification failed for device rebind - User: {}", request.getUsername());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Identity verification failed");
                errorResponse.put("message", "Please verify your identity with correct Aadhaar, PAN, and security answers");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
            
            // Perform device rebinding
            boolean success = authService.rebindDevice(
                request.getUsername(), 
                newDeviceId, 
                request.getVerificationMethod()
            );
            
            if (success) {
                logger.info("Device rebind successful for user: {} to device: {}", 
                    request.getUsername(), newDeviceId);
                    
                // Generate new login token for the new device
                User user = authService.getUserByUsername(request.getUsername());
                String newToken = jwtTokenProvider.generateToken(user.getUsername(), user.getId());
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Device successfully rebound");
                response.put("token", newToken);
                response.put("deviceId", newDeviceId);
                
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Rebinding failed");
                errorResponse.put("message", "Device rebinding could not be completed");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
        } catch (Exception e) {
            logger.error("Error during device rebinding: ", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", "An unexpected error occurred during device rebinding");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Verifies user identity through multiple factors.
     * In production, this would verify against stored user data and external services.
     */
    private boolean verifyUserIdentity(DeviceRebindRequest request) {
        // Implement identity verification logic
        // This is a simplified version - in production, you would:
        // 1. Verify Aadhaar against stored data or UIDAI API
        // 2. Verify PAN against stored data or income tax database
        // 3. Verify security question answers against stored answers
        // 4. Possibly send and verify SMS/Email OTP
        
        // Check if all required verification data is provided
        if (request.getAadhaarLast4() == null || request.getAadhaarLast4().length() != 4) {
            logger.warn("Invalid Aadhaar data provided for user: {}", request.getUsername());
            return false;
        }
        
        if (request.getPanNumber() == null || !request.getPanNumber().matches("[A-Z]{5}[0-9]{4}[A-Z]")) {
            logger.warn("Invalid PAN format provided for user: {}", request.getUsername());
            return false;
        }
        
        if (request.getSecurityAnswers() == null || request.getSecurityAnswers().isEmpty()) {
            logger.warn("No security answers provided for user: {}", request.getUsername());
            return false;
        }
        
        // In a real implementation, you would:
        // 1. Fetch user's stored Aadhaar/PAN from database
        // 2. Compare provided values with stored values
        // 3. Verify security question answers
        // For demo purposes, we'll simulate successful verification
        logger.info("Identity verification successful for user: {}", request.getUsername());
        return true;
    }
    
    /**
     * Check if a user requires device rebinding.
     */
    @GetMapping("/rebinding-status/{username}")
    public ResponseEntity<?> checkRebindingStatus(@PathVariable String username) {
        try {
            boolean requiresRebinding = authService.requiresDeviceRebinding(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("requiresRebinding", requiresRebinding);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error checking rebinding status: ", e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Internal server error");
            errorResponse.put("message", "Could not check rebinding status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Auth Service");
        return ResponseEntity.ok(response);
    }
}