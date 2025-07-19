package com.gradientgeeks.aegis.sfe.service;

import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationRequest;
import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationResponse;
import com.gradientgeeks.aegis.sfe.entity.Device;
import com.gradientgeeks.aegis.sfe.entity.RegistrationKey;
import com.gradientgeeks.aegis.sfe.repository.DeviceRepository;
import com.gradientgeeks.aegis.sfe.repository.RegistrationKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class DeviceRegistrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistrationService.class);
    
    private final DeviceRepository deviceRepository;
    private final RegistrationKeyRepository registrationKeyRepository;
    private final CryptographyService cryptographyService;
    private final IntegrityValidationService integrityValidationService;
    
    @Autowired
    public DeviceRegistrationService(
            DeviceRepository deviceRepository,
            RegistrationKeyRepository registrationKeyRepository,
            CryptographyService cryptographyService,
            IntegrityValidationService integrityValidationService) {
        this.deviceRepository = deviceRepository;
        this.registrationKeyRepository = registrationKeyRepository;
        this.cryptographyService = cryptographyService;
        this.integrityValidationService = integrityValidationService;
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
            
            String deviceId = cryptographyService.generateDeviceId();
            String secretKey = cryptographyService.generateSecretKey();
            
            Device device = new Device(deviceId, request.getClientId(), secretKey);
            device.setLastSeen(LocalDateTime.now());
            
            Device savedDevice = deviceRepository.save(device);
            
            logger.info("Device registered successfully with deviceId: {} for clientId: {}", 
                deviceId, request.getClientId());
            
            return DeviceRegistrationResponse.success(savedDevice.getDeviceId(), savedDevice.getSecretKey());
            
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
}