package com.gradientgeeks.aegis.sfe.service;

import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationRequest;
import com.gradientgeeks.aegis.sfe.dto.DeviceRegistrationResponse;
import com.gradientgeeks.aegis.sfe.entity.Device;
import com.gradientgeeks.aegis.sfe.entity.RegistrationKey;
import com.gradientgeeks.aegis.sfe.repository.DeviceRepository;
import com.gradientgeeks.aegis.sfe.repository.RegistrationKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceRegistrationServiceTest {
    
    @Mock
    private DeviceRepository deviceRepository;
    
    @Mock
    private RegistrationKeyRepository registrationKeyRepository;
    
    @Mock
    private CryptographyService cryptographyService;
    
    @Mock
    private IntegrityValidationService integrityValidationService;
    
    @InjectMocks
    private DeviceRegistrationService deviceRegistrationService;
    
    private DeviceRegistrationRequest validRequest;
    private RegistrationKey validRegistrationKey;
    
    @BeforeEach
    void setUp() {
        validRequest = new DeviceRegistrationRequest(
            "UCOBANK_PROD_ANDROID",
            "valid-registration-key",
            null
        );
        
        validRegistrationKey = new RegistrationKey(
            "UCOBANK_PROD_ANDROID",
            "valid-registration-key",
            "UCO Bank Android App"
        );
        validRegistrationKey.setId(1L);
        validRegistrationKey.setIsActive(true);
    }
    
    @Test
    void testRegisterDeviceSuccess() {
        when(registrationKeyRepository.findActiveByRegistrationKey(anyString()))
            .thenReturn(Optional.of(validRegistrationKey));
        when(cryptographyService.generateDeviceId()).thenReturn("dev_12345");
        when(cryptographyService.generateSecretKey()).thenReturn("secret-key-12345");
        
        Device savedDevice = new Device("dev_12345", "UCOBANK_PROD_ANDROID", "secret-key-12345");
        savedDevice.setId(1L);
        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);
        
        DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(validRequest);
        
        assertEquals("success", response.getStatus());
        assertEquals("dev_12345", response.getDeviceId());
        assertEquals("secret-key-12345", response.getSecretKey());
        assertNotNull(response.getMessage());
        
        verify(registrationKeyRepository).findActiveByRegistrationKey("valid-registration-key");
        verify(cryptographyService).generateDeviceId();
        verify(cryptographyService).generateSecretKey();
        verify(deviceRepository).save(any(Device.class));
    }
    
    @Test
    void testRegisterDeviceInvalidRegistrationKey() {
        when(registrationKeyRepository.findActiveByRegistrationKey(anyString()))
            .thenReturn(Optional.empty());
        
        DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(validRequest);
        
        assertEquals("error", response.getStatus());
        assertEquals("Invalid registration key", response.getMessage());
        assertNull(response.getDeviceId());
        assertNull(response.getSecretKey());
        
        verify(registrationKeyRepository).findActiveByRegistrationKey("valid-registration-key");
        verify(cryptographyService, never()).generateDeviceId();
        verify(deviceRepository, never()).save(any(Device.class));
    }
    
    @Test
    void testRegisterDeviceClientIdMismatch() {
        validRegistrationKey.setClientId("DIFFERENT_CLIENT");
        when(registrationKeyRepository.findActiveByRegistrationKey(anyString()))
            .thenReturn(Optional.of(validRegistrationKey));
        
        DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(validRequest);
        
        assertEquals("error", response.getStatus());
        assertEquals("Client ID mismatch", response.getMessage());
        
        verify(registrationKeyRepository).findActiveByRegistrationKey("valid-registration-key");
        verify(cryptographyService, never()).generateDeviceId();
        verify(deviceRepository, never()).save(any(Device.class));
    }
    
    @Test
    void testRegisterDeviceExpiredKey() {
        validRegistrationKey.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(registrationKeyRepository.findActiveByRegistrationKey(anyString()))
            .thenReturn(Optional.of(validRegistrationKey));
        
        DeviceRegistrationResponse response = deviceRegistrationService.registerDevice(validRequest);
        
        assertEquals("error", response.getStatus());
        assertEquals("Registration key has expired", response.getMessage());
        
        verify(registrationKeyRepository).findActiveByRegistrationKey("valid-registration-key");
        verify(cryptographyService, never()).generateDeviceId();
        verify(deviceRepository, never()).save(any(Device.class));
    }
    
    @Test
    void testIsDeviceRegistered() {
        when(deviceRepository.existsByDeviceId("dev_12345")).thenReturn(true);
        when(deviceRepository.existsByDeviceId("dev_unknown")).thenReturn(false);
        
        assertTrue(deviceRegistrationService.isDeviceRegistered("dev_12345"));
        assertFalse(deviceRegistrationService.isDeviceRegistered("dev_unknown"));
        
        verify(deviceRepository).existsByDeviceId("dev_12345");
        verify(deviceRepository).existsByDeviceId("dev_unknown");
    }
    
    @Test
    void testGetActiveDevice() {
        Device activeDevice = new Device("dev_12345", "UCOBANK_PROD_ANDROID", "secret-key");
        when(deviceRepository.findActiveByDeviceId("dev_12345"))
            .thenReturn(Optional.of(activeDevice));
        when(deviceRepository.findActiveByDeviceId("dev_unknown"))
            .thenReturn(Optional.empty());
        
        Optional<Device> result1 = deviceRegistrationService.getActiveDevice("dev_12345");
        Optional<Device> result2 = deviceRegistrationService.getActiveDevice("dev_unknown");
        
        assertTrue(result1.isPresent());
        assertEquals("dev_12345", result1.get().getDeviceId());
        assertFalse(result2.isPresent());
        
        verify(deviceRepository).findActiveByDeviceId("dev_12345");
        verify(deviceRepository).findActiveByDeviceId("dev_unknown");
    }
    
    @Test
    void testUpdateDeviceLastSeen() {
        deviceRegistrationService.updateDeviceLastSeen("dev_12345");
        
        verify(deviceRepository).updateLastSeen(eq("dev_12345"), any(LocalDateTime.class));
    }
    
    @Test
    void testDeactivateDevice() {
        deviceRegistrationService.deactivateDevice("dev_12345");
        
        verify(deviceRepository).deactivateDevice("dev_12345");
    }
}