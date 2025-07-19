package com.gradientgeeks.aegis.sfe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CryptographyServiceTest {
    
    private CryptographyService cryptographyService;
    
    @BeforeEach
    void setUp() {
        cryptographyService = new CryptographyService();
    }
    
    @Test
    void testGenerateSecretKey() {
        String secretKey1 = cryptographyService.generateSecretKey();
        String secretKey2 = cryptographyService.generateSecretKey();
        
        assertNotNull(secretKey1);
        assertNotNull(secretKey2);
        assertNotEquals(secretKey1, secretKey2);
        assertTrue(secretKey1.length() > 30);
    }
    
    @Test
    void testGenerateRegistrationKey() {
        String regKey1 = cryptographyService.generateRegistrationKey();
        String regKey2 = cryptographyService.generateRegistrationKey();
        
        assertNotNull(regKey1);
        assertNotNull(regKey2);
        assertNotEquals(regKey1, regKey2);
        assertTrue(regKey1.length() > 30);
    }
    
    @Test
    void testGenerateDeviceId() {
        String deviceId1 = cryptographyService.generateDeviceId();
        String deviceId2 = cryptographyService.generateDeviceId();
        
        assertNotNull(deviceId1);
        assertNotNull(deviceId2);
        assertNotEquals(deviceId1, deviceId2);
        assertTrue(deviceId1.startsWith("dev_"));
        assertTrue(deviceId2.startsWith("dev_"));
    }
    
    @Test
    void testComputeHmacSha256() {
        String secretKey = "test-secret-key";
        String data = "test-data";
        
        String signature1 = cryptographyService.computeHmacSha256(secretKey, data);
        String signature2 = cryptographyService.computeHmacSha256(secretKey, data);
        
        assertNotNull(signature1);
        assertNotNull(signature2);
        assertEquals(signature1, signature2);
        
        String differentData = "different-data";
        String signature3 = cryptographyService.computeHmacSha256(secretKey, differentData);
        assertNotEquals(signature1, signature3);
    }
    
    @Test
    void testVerifyHmacSha256Valid() {
        String secretKey = "test-secret-key";
        String data = "test-data";
        String signature = cryptographyService.computeHmacSha256(secretKey, data);
        
        boolean isValid = cryptographyService.verifyHmacSha256(secretKey, data, signature);
        assertTrue(isValid);
    }
    
    @Test
    void testVerifyHmacSha256Invalid() {
        String secretKey = "test-secret-key";
        String data = "test-data";
        String wrongSignature = "wrong-signature";
        
        boolean isValid = cryptographyService.verifyHmacSha256(secretKey, data, wrongSignature);
        assertFalse(isValid);
    }
    
    @Test
    void testVerifyHmacSha256WithDifferentKey() {
        String secretKey = "test-secret-key";
        String data = "test-data";
        String signature = cryptographyService.computeHmacSha256(secretKey, data);
        
        String differentKey = "different-secret-key";
        boolean isValid = cryptographyService.verifyHmacSha256(differentKey, data, signature);
        assertFalse(isValid);
    }
    
    @Test
    void testHashString() {
        String input = "test-input";
        
        String hash1 = cryptographyService.hashString(input);
        String hash2 = cryptographyService.hashString(input);
        
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(hash1, hash2);
        
        String differentInput = "different-input";
        String hash3 = cryptographyService.hashString(differentInput);
        assertNotEquals(hash1, hash3);
    }
    
    @Test
    void testIsValidBase64() {
        String validBase64 = "SGVsbG8gV29ybGQ=";
        assertTrue(cryptographyService.isValidBase64(validBase64));
        
        String invalidBase64 = "not-base64!@#";
        assertFalse(cryptographyService.isValidBase64(invalidBase64));
        
        String emptyString = "";
        assertTrue(cryptographyService.isValidBase64(emptyString));
    }
}