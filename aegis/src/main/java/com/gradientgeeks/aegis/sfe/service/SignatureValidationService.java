package com.gradientgeeks.aegis.sfe.service;

import com.gradientgeeks.aegis.sfe.dto.SignatureValidationRequest;
import com.gradientgeeks.aegis.sfe.dto.SignatureValidationResponse;
import com.gradientgeeks.aegis.sfe.entity.Device;
import com.gradientgeeks.aegis.sfe.entity.Policy;
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
    private final PolicyEngineService policyEngineService;
    
    @Autowired
    public SignatureValidationService(
            DeviceRepository deviceRepository,
            CryptographyService cryptographyService,
            PolicyEngineService policyEngineService) {
        this.deviceRepository = deviceRepository;
        this.cryptographyService = cryptographyService;
        this.policyEngineService = policyEngineService;
    }
    
    public SignatureValidationResponse validateSignature(SignatureValidationRequest request) {
        logger.info("Validating signature for deviceId: {}", request.getDeviceId());
        
        try {
            Optional<Device> deviceOpt = deviceRepository.findActiveByDeviceId(request.getDeviceId());
            
            if (deviceOpt.isEmpty()) {
                // Check if this might be a multi-bank device ID
                // Try to find all related device IDs for this base device
                String baseDeviceId = request.getDeviceId().contains("_") ? 
                    request.getDeviceId().substring(0, request.getDeviceId().lastIndexOf("_")) : 
                    request.getDeviceId();
                
                java.util.List<Device> relatedDevices = deviceRepository.findAllActiveByBaseDeviceId(baseDeviceId);
                
                // Find the device with matching client ID from the request
                deviceOpt = relatedDevices.stream()
                    .filter(d -> d.getDeviceId().equals(request.getDeviceId()) || 
                                (request.getClientId() != null && d.getClientId().equals(request.getClientId())))
                    .findFirst();
                
                if (deviceOpt.isEmpty()) {
                    logger.warn("Device not found or inactive: {}", request.getDeviceId());
                    return new SignatureValidationResponse(false, "Device not found or inactive");
                }
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
                
                // Evaluate policies
                PolicyContext policyContext = buildPolicyContext(request, device);
                PolicyEvaluationResult policyResult = policyEngineService.evaluatePolicies(policyContext);
                
                if (!policyResult.isPassed()) {
                    logger.warn("Policy validation failed for deviceId: {} - {}", 
                        request.getDeviceId(), policyResult.getMessage());
                    
                    SignatureValidationResponse response = new SignatureValidationResponse(false, 
                        "Request blocked by policy: " + policyResult.getMessage());
                    response.setPolicyEnforcement(policyResult.getEnforcementLevel());
                    response.setRequiresMfa(policyResult.requiresMfa());
                    return response;
                }
                
                deviceRepository.updateLastSeen(request.getDeviceId(), LocalDateTime.now());
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
    public boolean isDeviceActive(String deviceId) {
        return deviceRepository.findActiveByDeviceId(deviceId).isPresent();
    }
    
    public String generateExpectedSignature(String deviceId, String stringToSign) {
        Optional<Device> deviceOpt = deviceRepository.findActiveByDeviceId(deviceId);
        if (deviceOpt.isEmpty()) {
            throw new IllegalArgumentException("Device not found: " + deviceId);
        }
        
        Device device = deviceOpt.get();
        return cryptographyService.computeHmacSha256(device.getSecretKey(), stringToSign);
    }
    
    private PolicyContext buildPolicyContext(SignatureValidationRequest request, Device device) {
        PolicyContext.Builder builder = PolicyContext.builder()
            .deviceId(device.getDeviceId())
            .clientId(device.getClientId())
            .ipAddress(request.getIpAddress())
            .userAgent(request.getUserAgent())
            .requestInfo("POST", "/api/validate", LocalDateTime.now());
        
        // Add additional context from request if available
        if (request.getPolicyContext() != null) {
            request.getPolicyContext().forEach(builder::additionalData);
        }
        
        // Parse string to sign for additional context
        parseStringToSign(request.getStringToSign(), builder);
        
        return builder.build();
    }
    
    private void parseStringToSign(String stringToSign, PolicyContext.Builder builder) {
        // Parse METHOD|URI|TIMESTAMP|NONCE|BODY_HASH format
        String[] parts = stringToSign.split("\\|");
        if (parts.length >= 3) {
            builder.requestInfo(parts[0], parts[1], LocalDateTime.now());
        }
    }
}