package com.gradientgeeks.aegis.sfe.controller;

import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationRequest;
import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationResponse;
import com.gradientgeeks.aegis.sfe.dto.SignatureValidationRequest;
import com.gradientgeeks.aegis.sfe.dto.SignatureValidationResponse;
import com.gradientgeeks.aegis.sfe.service.DeviceRegistrationService;
import com.gradientgeeks.aegis.sfe.service.SignatureValidationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class DeviceController {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);
    
    private final DeviceRegistrationService deviceRegistrationService;
    private final SignatureValidationService signatureValidationService;
    
    @Autowired
    public DeviceController(
            DeviceRegistrationService deviceRegistrationService,
            SignatureValidationService signatureValidationService) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.signatureValidationService = signatureValidationService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<DeviceRegistrationResponse> registerDevice(
            @Valid @RequestBody DeviceRegistrationRequest request) {
        
        logger.info("Device registration request received for clientId: {}", request.getClientId());
        
        try {
            DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(request);
            
            if ("success".equals(response.getStatus())) {
                logger.info("Device registration successful for clientId: {}", request.getClientId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Device registration failed for clientId: {} - {}", 
                    request.getClientId(), response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during device registration for clientId: {}", 
                request.getClientId(), e);
            
            DeviceRegistrationResponse errorResponse = DeviceRegistrationResponse.error("Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<SignatureValidationResponse> validateSignature(
            @Valid @RequestBody SignatureValidationRequest request) {
        
        logger.info("Signature validation request received for deviceId: {}", request.getDeviceId());
        
        try {
            SignatureValidationResponse response = signatureValidationService.validateSignature(request);
            
            if (response.isValid()) {
                logger.info("Signature validation successful for deviceId: {}", request.getDeviceId());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Signature validation failed for deviceId: {} - {}", 
                    request.getDeviceId(), response.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during signature validation for deviceId: {}", 
                request.getDeviceId(), e);
            
            SignatureValidationResponse errorResponse = new SignatureValidationResponse(
                false, "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Aegis Security API is running");
    }
}