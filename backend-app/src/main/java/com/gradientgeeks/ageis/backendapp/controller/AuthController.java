package com.gradientgeeks.ageis.backendapp.controller;

import com.gradientgeeks.ageis.backendapp.dto.LoginRequest;
import com.gradientgeeks.ageis.backendapp.dto.LoginResponse;
import com.gradientgeeks.ageis.backendapp.exception.AuthenticationException;
import com.gradientgeeks.ageis.backendapp.service.AuthService;
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
    
    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login request received for username: {}", loginRequest.getUsername());
            LoginResponse loginResponse = authService.login(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (AuthenticationException e) {
            logger.error("Authentication failed: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Authentication failed");
            errorResponse.put("message", e.getMessage());
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
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Auth Service");
        return ResponseEntity.ok(response);
    }
}