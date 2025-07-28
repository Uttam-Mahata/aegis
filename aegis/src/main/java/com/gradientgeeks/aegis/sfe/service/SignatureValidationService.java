package com.gradientgeeks.aegis.sfe.service;

import com.gradientgeeks.aegis.sfe.dto.SignatureValidationRequest;
import com.gradientgeeks.aegis.sfe.dto.SignatureValidationResponse;
import com.gradientgeeks.aegis.sfe.entity.Device;
import com.gradientgeeks.aegis.sfe.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class SignatureValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(SignatureValidationService.class);
    
    private final DeviceRepository deviceRepository;
    private final CryptographyService cryptographyService;
    
    @Autowired
    public SignatureValidationService(
            DeviceRepository deviceRepository,
            CryptographyService cryptographyService) {
        this.deviceRepository = deviceRepository;
        this.cryptographyService = cryptographyService;
    }
    
    public SignatureValidationResponse validateSignature(SignatureValidationRequest request) {
        logger.info("Validating signature for deviceId: {}", request.getDeviceId());
        
        try {
            // Validate that client ID is provided
            if (request.getClientId() == null || request.getClientId().trim().isEmpty()) {
                logger.warn("Client ID not provided for device: {}", request.getDeviceId());
                return new SignatureValidationResponse(false, "Client ID is required");
            }
            
            // Find the device using composite key (deviceId + clientId)
            Optional<Device> deviceOpt = deviceRepository.findActiveByDeviceIdAndClientId(
                request.getDeviceId(), request.getClientId());
            
            if (deviceOpt.isEmpty()) {
                logger.warn("Device not found or inactive - Device: {}, Client: {}", 
                    request.getDeviceId(), request.getClientId());
                return new SignatureValidationResponse(false, "Device not found or inactive");
            }
            
            Device device = deviceOpt.get();
            
            // Log the received signature
            logger.debug("Received signature: {}", request.getSignature());
            logger.debug("String to sign: {}", request.getStringToSign());
            
            if (!cryptographyService.isValidBase64(request.getSignature())) {
                logger.warn("Invalid signature format for deviceId: {}", request.getDeviceId());
                return new SignatureValidationResponse(false, "Invalid signature format");
            }
            
            // Mask the secret key for logging (show first 8 and last 4 characters)
            String maskedKey = maskSecretKey(device.getSecretKey());
            logger.debug("Using secret key (masked): {}", maskedKey);
            
            // Compute expected signature for comparison
            String expectedSignature = cryptographyService.computeHmacSha256(
                device.getSecretKey(),
                request.getStringToSign()
            );
            logger.debug("Expected signature: {}", expectedSignature);
            
            boolean isValid = cryptographyService.verifyHmacSha256(
                device.getSecretKey(),
                request.getStringToSign(),
                request.getSignature()
            );
            
            if (isValid) {
                logger.info("Signature validation successful for deviceId: {}", request.getDeviceId());
                
                
                deviceRepository.updateLastSeen(request.getDeviceId(), device.getClientId(), LocalDateTime.now());
                return new SignatureValidationResponse(true, "Signature is valid", request.getDeviceId());
            } else {
                logger.warn("Signature validation failed for deviceId: {}", request.getDeviceId());
                logger.warn("Signature mismatch - Expected: {} but Received: {}", 
                    expectedSignature, request.getSignature());
                return new SignatureValidationResponse(false, "Signature is invalid");
            }
            
        } catch (Exception e) {
            logger.error("Error during signature validation for deviceId: {}", request.getDeviceId(), e);
            return new SignatureValidationResponse(false, "Internal server error during validation");
        }
    }
    
    private String maskSecretKey(String secretKey) {
        if (secretKey == null || secretKey.length() < 12) {
            return "***INVALID***";
        }
        return secretKey.substring(0, 8) + "..." + secretKey.substring(secretKey.length() - 4);
    }
    
    @Transactional(readOnly = true)
    public boolean isDeviceActive(String deviceId, String clientId) {
        return deviceRepository.findActiveByDeviceIdAndClientId(deviceId, clientId).isPresent();
    }
    
    public String generateExpectedSignature(String deviceId, String clientId, String stringToSign) {
        Optional<Device> deviceOpt = deviceRepository.findActiveByDeviceIdAndClientId(deviceId, clientId);
        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Device not found: " + deviceId + " for client: " + clientId);
        }
        
        Device device = deviceOpt.get();
        return cryptographyService.computeHmacSha256(device.getSecretKey(), stringToSign);
    }
    
}