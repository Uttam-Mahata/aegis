package com.gradientgeeks.aegis.sfe.service;

import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationRequest;
import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationResponse;
import com.gradientgeeks.aegis.sfe.entity.Device;
import com.gradientgeeks.aegis.sfe.entity.DeviceFingerprint;
import com.gradientgeeks.aegis.sfe.entity.RegistrationKey;
import com.gradientgeeks.aegis.sfe.repository.DeviceFingerprintRepository;
import com.gradientgeeks.aegis.sfe.repository.DeviceRepository;
import com.gradientgeeks.aegis.sfe.repository.RegistrationKeyRepository;
import com.gradientgeeks.aegis.sfe.dto.RegistrationKeyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DeviceRegistrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrationService.class);
    
    private final DeviceRepository deviceRepository;
    private final DeviceFingerprintRepository fingerprintRepository;
    private final RegistrationKeyRepository registrationKeyRepository;
    private final CryptographyService cryptographyService;
    private final IntegrityValidationService integrityValidationService;
    private final DeviceFraudDetectionService fraudDetectionService;
    private final PolicyEngineService policyEngineService;
    private final RegistrationKeyService registrationKeyService;
    
    @Autowired
    public DeviceRegistrationService(
            DeviceRepository deviceRepository,
            DeviceFingerprintRepository fingerprintRepository,
            RegistrationKeyRepository registrationKeyRepository,
            CryptographyService cryptographyService,
            IntegrityValidationService integrityValidationService,
            DeviceFraudDetectionService fraudDetectionService,
            PolicyEngineService policyEngineService,
            RegistrationKeyService registrationKeyService) {
        this.deviceRepository = deviceRepository;
        this.fingerprintRepository = fingerprintRepository;
        this.registrationKeyRepository = registrationKeyRepository;
        this.cryptographyService = cryptographyService;
        this.integrityValidationService = integrityValidationService;
        this.fraudDetectionService = fraudDetectionService;
        this.policyEngineService = policyEngineService;
        this.registrationKeyService = registrationKeyService;
    }
    
    public DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request) {
        logger.info("Starting device registration for clientId: {}", request.getClientId());
        
        try {
            if (!validateIntegrityToken(request.getIntegrityToken())) {
                logger.warn("Integrity token validation failed for clientId: {}", request.getClientId());
                return DeviceRegistrationResponse.error("Integrity validation failed");
            }
            
            Optional<RegistrationKey> registrationKeyOpt = registrationKeyRepository
                    .findActiveByRegistrationKey(request.getRegistrationKey());
            
            if (registrationKeyOpt.isEmpty()) {
                logger.warn("Invalid or inactive registration key for clientId: {}", request.getClientId());
                return DeviceRegistrationResponse.error("Invalid registration key");
            }
            
            RegistrationKey registrationKey = registrationKeyOpt.get();
            
            if (!registrationKey.getClientId().equals(request.getClientId())) {
                logger.warn("Client ID mismatch for registration key. Expected: {}, Got: {}", 
                    registrationKey.getClientId(), request.getClientId());
                return DeviceRegistrationResponse.error("Client ID mismatch");
            }
            
            if (registrationKey.isExpired()) {
                logger.warn("Registration key has expired for clientId: {}", request.getClientId());
                return DeviceRegistrationResponse.error("Registration key has expired");
            }
            
            // Step 6: Fraud detection using device fingerprint
            if (request.getDeviceFingerprint() == null) {
                logger.warn("Device fingerprint missing for clientId: {}", request.getClientId());
                return DeviceRegistrationResponse.error("Device fingerprint is required");
            }
            
            // First check if a device with this fingerprint already exists
            // Use hardware hash for consistent device identification across app reinstalls
            String hardwareHash = request.getDeviceFingerprint().getHardware().getHash();
            logger.info("Checking for existing device with hardware hash: {}", hardwareHash);
            
            // Look for existing device fingerprint by hardware hash
            Optional<DeviceFingerprint> existingFingerprint = fingerprintRepository.findByHardwareHash(hardwareHash);
            String deviceId;
            
            if (existingFingerprint.isPresent()) {
                // Device fingerprint already exists - reuse the existing device ID
                deviceId = existingFingerprint.get().getDeviceId();
                logger.info("Found existing device with ID: {} for hardware hash: {}", deviceId, hardwareHash);
                
                // Update the composite hash if it changed (due to network changes etc)
                if (!existingFingerprint.get().getCompositeHash().equals(request.getDeviceFingerprint().getCompositeHash())) {
                    logger.info("Updating composite hash for device: {} from {} to {}", 
                        deviceId, 
                        existingFingerprint.get().getCompositeHash(), 
                        request.getDeviceFingerprint().getCompositeHash());
                    existingFingerprint.get().setCompositeHash(request.getDeviceFingerprint().getCompositeHash());
                    existingFingerprint.get().setUpdatedAt(LocalDateTime.now());
                    fingerprintRepository.save(existingFingerprint.get());
                }
            } else {
                // No existing device found - generate new device ID from hardware hash only
                deviceId = cryptographyService.generatePersistentDeviceIdFromHardware(hardwareHash);
                logger.info("No existing device found, generated new device ID: {} for hardware hash: {}", deviceId, hardwareHash);
            }
            
            // Analyze device fingerprint for fraud indicators
            logger.info("Performing fraud detection for device: {}", deviceId);
            FraudDetectionResult fraudResult = fraudDetectionService.analyzeFingerprint(
                deviceId, request.getDeviceFingerprint());
            
            if (fraudResult.isBlocked()) {
                logger.warn("Device registration blocked due to fraud detection - Device: {}, Reason: {}, Similarity: {}", 
                    deviceId, fraudResult.getReason(), fraudResult.getSimilarityScore());
                return DeviceRegistrationResponse.error("Device registration denied: " + fraudResult.getReason());
            }
            
            if (fraudResult.isFlagged()) {
                logger.warn("Device flagged for review - Device: {}, Reason: {}, Similarity: {}", 
                    deviceId, fraudResult.getReason(), fraudResult.getSimilarityScore());
                // Continue with registration but log for manual review
            }
            
            // Check if device already exists (same device re-registering)
            Optional<Device> existingDevice = deviceRepository.findByDeviceId(deviceId);
            Device savedDevice;
            
            if (existingDevice.isPresent()) {
                // Device already exists - check if it's blocked before allowing re-registration
                logger.info("Device already exists, checking status: {}", deviceId);
                savedDevice = existingDevice.get();
                
                // Check if device is blocked
                if (savedDevice.getStatus() == Device.DeviceStatus.TEMPORARILY_BLOCKED || 
                    savedDevice.getStatus() == Device.DeviceStatus.PERMANENTLY_BLOCKED) {
                    logger.warn("Blocked device attempting to re-register - Device: {}, Status: {}", 
                        deviceId, savedDevice.getStatus());
                    return DeviceRegistrationResponse.error(
                        "Device is blocked and cannot be re-registered. Status: " + savedDevice.getStatus()
                    );
                }
                
                // Check if this is the same client ID or a different bank app
                if (savedDevice.getClientId().equals(request.getClientId())) {
                    // Same bank app re-registering - update as before
                    if (savedDevice.getStatus() == Device.DeviceStatus.ACTIVE) {
                        savedDevice.setLastSeen(LocalDateTime.now());
                        savedDevice.setIsActive(true);
                        // Generate new secret key for security
                        String newSecretKey = cryptographyService.generateSecretKey();
                        savedDevice.setSecretKey(newSecretKey);
                        savedDevice.setUpdatedAt(LocalDateTime.now());
                        savedDevice = deviceRepository.save(savedDevice);
                        logger.info("Active device re-registered with same bank app: {}", deviceId);
                    } else {
                        logger.warn("Device in unexpected status attempting to re-register - Device: {}, Status: {}", 
                            deviceId, savedDevice.getStatus());
                        return DeviceRegistrationResponse.error(
                            "Device cannot be re-registered in current status: " + savedDevice.getStatus()
                        );
                    }
                } else {
                    // Different bank app on same device - this is allowed for multi-banking
                    logger.info("Same device registering with different bank app - Device: {}, Current: {}, New: {}", 
                        deviceId, savedDevice.getClientId(), request.getClientId());
                    
                    // Create a new device entry for the new bank app
                    // This allows tracking device-bank relationships while maintaining security
                    String secretKey = cryptographyService.generateSecretKey();
                    Device newBankDevice = new Device(deviceId + "_" + request.getClientId(), request.getClientId(), secretKey);
                    newBankDevice.setLastSeen(LocalDateTime.now());
                    savedDevice = deviceRepository.save(newBankDevice);
                    logger.info("Device registered with new bank app: {} for client: {}", deviceId, request.getClientId());
                    
                    // Note: We keep the original device entry for the first bank
                    // This allows tracking cross-bank device usage for fraud detection
                }
            } else {
                // New device registration
                String secretKey = cryptographyService.generateSecretKey();
                Device device = new Device(deviceId, request.getClientId(), secretKey);
                device.setLastSeen(LocalDateTime.now());
                savedDevice = deviceRepository.save(device);
                logger.info("New device registered: {}", deviceId);
            }
            
            // Step 7: Save or update device fingerprint for future fraud detection
            // Only save if no existing fingerprint was found earlier
            if (!existingFingerprint.isPresent()) {
                try {
                    fraudDetectionService.saveFingerprint(savedDevice.getDeviceId(), request.getDeviceFingerprint());
                    logger.info("Device fingerprint saved successfully for device: {}", savedDevice.getDeviceId());
                } catch (Exception e) {
                    logger.error("Failed to save device fingerprint for device: {}", savedDevice.getDeviceId(), e);
                    // Don't fail registration if fingerprint save fails, but log for investigation
                }
            } else {
                logger.debug("Fingerprint already exists for device: {}, skipping save", savedDevice.getDeviceId());
            }
            
            logger.info("Device registered successfully with deviceId: {} for clientId: {}", 
                deviceId, request.getClientId());
            
            // Encode the secret key as Base64 for the response
            // The Android SDK expects the secret key to be Base64 encoded
            String base64SecretKey = java.util.Base64.getEncoder()
                .encodeToString(savedDevice.getSecretKey().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            return DeviceRegistrationResponse.success(savedDevice.getDeviceId(), base64SecretKey);
            
        } catch (Exception e) {
            logger.error("Error during device registration for clientId: {}", request.getClientId(), e);
            return DeviceRegistrationResponse.error("Internal server error during registration");
        }
    }
    
    private boolean validateIntegrityToken(String integrityToken) {
        if (integrityToken == null || integrityToken.trim().isEmpty()) {
            logger.debug("No integrity token provided - proceeding for hackathon demo");
            return true;
        }
        
        return integrityValidationService.validatePlayIntegrityToken(integrityToken);
    }
    
    @Transactional(readOnly = true)
    public boolean isDeviceRegistered(String deviceId) {
        return deviceRepository.existsByDeviceId(deviceId);
    }
    
    @Transactional(readOnly = true)
    public Optional<Device> getActiveDevice(String deviceId) {
        return deviceRepository.findActiveByDeviceId(deviceId);
    }
    
    public void updateDeviceLastSeen(String deviceId) {
        deviceRepository.updateLastSeen(deviceId, LocalDateTime.now());
    }
    
    public void deactivateDevice(String deviceId) {
        logger.info("Deactivating device: {}", deviceId);
        deviceRepository.deactivateDevice(deviceId);
    }
    
    /**
     * Marks a device and its fingerprint as fraudulent.
     * This will prevent similar devices from registering in the future.
     * 
     * @param deviceId The device identifier
     * @param reason Reason for marking as fraudulent
     * @return true if successfully marked, false otherwise
     */
    public boolean markDeviceAsFraudulent(String deviceId, String reason) {
        logger.warn("Marking device as fraudulent: {} - Reason: {}", deviceId, reason);
        
        try {
            // Deactivate the device
            deactivateDevice(deviceId);
            
            // Mark fingerprint as fraudulent for future detection
            return fraudDetectionService.markDeviceAsFraudulent(deviceId, reason);
            
        } catch (Exception e) {
            logger.error("Error marking device as fraudulent: {}", deviceId, e);
            return false;
        }
    }
    
    /**
     * Finds a device by its device ID.
     * 
     * @param deviceId The device identifier
     * @return Optional containing the device if found
     */
    @Transactional(readOnly = true)
    public Optional<Device> findDeviceByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId);
    }
    
    /**
     * Finds a device fingerprint by device ID.
     * 
     * @param deviceId The device identifier
     * @return Optional containing the fingerprint if found
     */
    @Transactional(readOnly = true)
    public Optional<DeviceFingerprint> findFingerprintByDeviceId(String deviceId) {
        return fingerprintRepository.findByDeviceId(deviceId);
    }
    
    /**
     * Searches devices based on various criteria.
     * 
     * @param deviceId Optional device ID filter
     * @param clientId Optional client ID filter
     * @param status Optional status filter
     * @param pageable Pagination parameters
     * @return Page of devices matching the criteria
     */
    @Transactional(readOnly = true)
    public Page<Device> searchDevices(String deviceId, String clientId, String status, Pageable pageable) {
        Specification<Device> spec = (root, query, cb) -> cb.conjunction();
        
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("deviceId"), deviceId));
        }
        
        if (clientId != null && !clientId.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("clientId"), clientId));
        }
        
        if (status != null && !status.trim().isEmpty()) {
            try {
                Device.DeviceStatus deviceStatus = Device.DeviceStatus.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), deviceStatus));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid device status filter: {}", status);
            }
        }
        
        return deviceRepository.findAll(spec, pageable);
    }
    
    /**
     * Updates a device's status.
     * 
     * @param deviceId The device identifier
     * @param newStatus The new status (ACTIVE, TEMPORARILY_BLOCKED, PERMANENTLY_BLOCKED)
     * @param reason Reason for the status change
     * @return true if successfully updated, false otherwise
     */
    public boolean updateDeviceStatus(String deviceId, String newStatus, String reason) {
        logger.info("Updating device status: {} to {} - Reason: {}", deviceId, newStatus, reason);
        
        try {
            // First try to find the exact device
            Optional<Device> deviceOpt = deviceRepository.findByDeviceId(deviceId);
            
            // If not found, check if it's a base device ID and update all related devices
            if (deviceOpt.isEmpty()) {
                List<Device> relatedDevices = deviceRepository.findAllByBaseDeviceId(deviceId);
                if (relatedDevices.isEmpty()) {
                    logger.warn("Device not found for status update: {}", deviceId);
                    return false;
                }
                
                // Update all related devices (all bank apps for this physical device)
                Device.DeviceStatus status;
                try {
                    status = Device.DeviceStatus.valueOf(newStatus.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid device status: {}", newStatus);
                    return false;
                }
                
                for (Device device : relatedDevices) {
                    device.setStatus(status);
                    device.setUpdatedAt(LocalDateTime.now());
                    
                    // If blocking, deactivate the device
                    if (status == Device.DeviceStatus.TEMPORARILY_BLOCKED || 
                        status == Device.DeviceStatus.PERMANENTLY_BLOCKED) {
                        device.setIsActive(false);
                    } else if (status == Device.DeviceStatus.ACTIVE) {
                        device.setIsActive(true);
                    }
                    
                    deviceRepository.save(device);
                    logger.info("Related device status updated: {} -> {}", device.getDeviceId(), newStatus);
                }
                
                logger.info("All related devices status updated successfully for base device: {} -> {}", deviceId, newStatus);
                return true;
            }
            
            // Single device update
            Device device = deviceOpt.get();
            Device.DeviceStatus status;
            
            try {
                status = Device.DeviceStatus.valueOf(newStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("Invalid device status: {}", newStatus);
                return false;
            }
            
            device.setStatus(status);
            device.setUpdatedAt(LocalDateTime.now());
            
            // If blocking, deactivate the device
            if (status == Device.DeviceStatus.TEMPORARILY_BLOCKED || 
                status == Device.DeviceStatus.PERMANENTLY_BLOCKED) {
                device.setIsActive(false);
            } else if (status == Device.DeviceStatus.ACTIVE) {
                device.setIsActive(true);
            }
            
            deviceRepository.save(device);
            logger.info("Device status updated successfully: {} -> {}", deviceId, newStatus);
            
            // If this is a multi-bank device, also update the base device
            String baseDeviceId = deviceId.contains("_") ? 
                deviceId.substring(0, deviceId.lastIndexOf("_")) : null;
            if (baseDeviceId != null) {
                Optional<Device> baseDeviceOpt = deviceRepository.findByDeviceId(baseDeviceId);
                if (baseDeviceOpt.isPresent()) {
                    Device baseDevice = baseDeviceOpt.get();
                    baseDevice.setStatus(status);
                    baseDevice.setUpdatedAt(LocalDateTime.now());
                    baseDevice.setIsActive(device.getIsActive());
                    deviceRepository.save(baseDevice);
                    logger.info("Base device status also updated: {} -> {}", baseDeviceId, newStatus);
                }
            }
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error updating device status: {}", deviceId, e);
            return false;
        }
    }
    
    /**
     * Checks if a device has interacted with a specific bank/organization.
     * This is used to verify that a bank can only block devices they have transaction history with.
     * 
     * @param deviceId The device identifier
     * @param organization The bank's organization name
     * @return true if the device has interacted with the bank
     */
    @Transactional(readOnly = true)
    public boolean hasDeviceInteractedWithBank(String deviceId, String organization) {
        // Get all client IDs for this organization
        List<String> organizationClientIds = getClientIdsByOrganization(organization);
        if (organizationClientIds.isEmpty()) {
            return false;
        }
        
        // Check if the device was registered with any of this organization's client IDs
        Optional<Device> device = deviceRepository.findByDeviceId(deviceId);
        if (device.isPresent() && organizationClientIds.contains(device.get().getClientId())) {
            return true;
        }
        
        // In a full implementation, you would also check transaction/validation history
        // For now, we'll just check device registration
        logger.debug("Checking device interaction - Device: {}, Organization: {}, Has interaction: {}", 
            deviceId, organization, device.isPresent() && organizationClientIds.contains(device.get().getClientId()));
        
        return device.isPresent() && organizationClientIds.contains(device.get().getClientId());
    }
    
    /**
     * Processes a fraud report from a bank according to the bank's configured policies.
     * 
     * @param deviceId The device identifier
     * @param bankClientId The bank's client ID
     * @param bankTransactionId The bank's transaction ID for audit
     * @param reasonCode The reason code for the fraud report
     * @param description Additional description
     * @return true if successfully processed
     */
    public boolean processFraudReport(String deviceId, String bankClientId, String bankTransactionId, 
                                    String reasonCode, String description) {
        logger.warn("Processing fraud report - Device: {}, Bank: {}, Transaction: {}, Reason: {}", 
            deviceId, bankClientId, bankTransactionId, reasonCode);
        
        try {
            // Get the bank's policy configuration for this type of fraud report
            PolicyContext context = new PolicyContext();
            context.setDeviceId(deviceId);
            context.setClientId(bankClientId);
            context.setBankTransactionId(bankTransactionId);
            context.setReasonCode(reasonCode);
            context.setDescription(description);
            
            // Evaluate the bank's custom rules for this fraud report
            PolicyEvaluationResult result = policyEngineService.evaluateFraudReport(bankClientId, context);
            
            // Apply the action determined by the bank's policy
            if (result.getAction() != null) {
                switch (result.getAction().toUpperCase()) {
                    case "TEMPORARILY_BLOCK":
                        updateDeviceStatus(deviceId, "TEMPORARILY_BLOCKED", 
                            "Fraud report: " + reasonCode + " - " + description);
                        break;
                    case "PERMANENTLY_BLOCK":
                        updateDeviceStatus(deviceId, "PERMANENTLY_BLOCKED", 
                            "Fraud report: " + reasonCode + " - " + description);
                        markDeviceAsFraudulent(deviceId, "Bank fraud report: " + reasonCode);
                        break;
                    case "FLAG_FOR_REVIEW":
                        // In a full implementation, this would create a review case
                        logger.warn("Device flagged for review: {} by bank: {}", deviceId, bankClientId);
                        break;
                    case "IGNORE":
                        logger.info("Fraud report ignored per bank policy: {} for device: {}", bankClientId, deviceId);
                        break;
                    default:
                        logger.warn("Unknown policy action: {} for device: {}", result.getAction(), deviceId);
                }
            }
            
            // In a full implementation, you would also:
            // 1. Store the fraud report for audit trails
            // 2. Check for automatic escalation rules (e.g., 3 temp blocks = permanent block)
            // 3. Send notifications to relevant parties
            
            logger.info("Fraud report processed successfully - Action taken: {}", result.getAction());
            return true;
            
        } catch (Exception e) {
            logger.error("Error processing fraud report for device: {}", deviceId, e);
            return false;
        }
    }
    
    /**
     * Get total device count across all organizations
     */
    @Transactional(readOnly = true)
    public long getTotalDeviceCount() {
        return deviceRepository.count();
    }
    
    /**
     * Get total device count for a specific organization
     */
    @Transactional(readOnly = true)
    public long getTotalDeviceCountByOrganization(String organization) {
        List<String> organizationClientIds = getClientIdsByOrganization(organization);
        if (organizationClientIds.isEmpty()) {
            return 0;
        }
        
        // Count devices for all client IDs of the organization
        return organizationClientIds.stream()
            .mapToLong(clientId -> deviceRepository.countByClientId(clientId))
            .sum();
    }
    
    /**
     * Get blocked device count across all organizations
     */
    @Transactional(readOnly = true)
    public long getBlockedDeviceCount() {
        return deviceRepository.countByStatusIn(List.of(
            Device.DeviceStatus.TEMPORARILY_BLOCKED,
            Device.DeviceStatus.PERMANENTLY_BLOCKED
        ));
    }
    
    /**
     * Get blocked device count for a specific organization
     */
    @Transactional(readOnly = true)
    public long getBlockedDeviceCountByOrganization(String organization) {
        List<String> organizationClientIds = getClientIdsByOrganization(organization);
        if (organizationClientIds.isEmpty()) {
            return 0;
        }
        
        // Count blocked devices for all client IDs of the organization
        return organizationClientIds.stream()
            .mapToLong(clientId -> deviceRepository.countByClientIdAndStatusIn(clientId, List.of(
                Device.DeviceStatus.TEMPORARILY_BLOCKED,
                Device.DeviceStatus.PERMANENTLY_BLOCKED
            )))
            .sum();
    }
    
    /**
     * Get device count breakdown by organization
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDeviceCountByOrganization() {
        // This would typically be implemented with a custom query
        // For now, return an empty list
        return new ArrayList<>();
    }
    
    /**
     * Get all client IDs for an organization
     */
    @Transactional(readOnly = true)
    public List<String> getClientIdsByOrganization(String organization) {
        List<RegistrationKeyResponse> keys = registrationKeyService.getRegistrationKeysByOrganization(organization);
        return keys.stream()
            .map(RegistrationKeyResponse::getClientId)
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Search devices by organization (for non-admin users)
     * This method finds all devices that belong to any client ID registered under the organization
     */
    @Transactional(readOnly = true)
    public Page<Device> searchDevicesByOrganization(String deviceId, String organization, 
                                                   String status, Pageable pageable) {
        // Get all client IDs for this organization
        List<String> organizationClientIds = getClientIdsByOrganization(organization);
        
        if (organizationClientIds.isEmpty()) {
            logger.info("No client IDs found for organization: {}", organization);
            return Page.empty(pageable);
        }
        
        Specification<Device> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Filter by organization's client IDs
            predicates.add(root.get("clientId").in(organizationClientIds));
            
            // Filter by deviceId if provided
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("deviceId")), 
                    "%" + deviceId.toLowerCase() + "%"
                ));
            }
            
            // Filter by status if provided
            if (status != null && !status.trim().isEmpty()) {
                try {
                    Device.DeviceStatus deviceStatus = Device.DeviceStatus.valueOf(status);
                    predicates.add(criteriaBuilder.equal(root.get("status"), deviceStatus));
                } catch (IllegalArgumentException e) {
                    logger.warn("Invalid device status filter: {}", status);
                }
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        
        return deviceRepository.findAll(spec, pageable);
    }
}