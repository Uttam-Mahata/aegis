package com.gradientgeeks.aegis.sfe_client.signing

import com.gradientgeeks.aegis.sfe_client.crypto.CryptographyService
import com.gradientgeeks.aegis.sfe_client.provisioning.DeviceProvisioningService
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.ArgumentMatchers.anyString
import javax.crypto.spec.SecretKeySpec

/**
 * Unit tests for RequestSigningService.
 * 
 * Tests the request signing functionality including string-to-sign creation,
 * signature generation, and header management.
 */
class RequestSigningServiceTest {
    
    @Mock
    private lateinit var mockCryptographyService: CryptographyService
    
    @Mock
    private lateinit var mockProvisioningService: DeviceProvisioningService
    
    private lateinit var requestSigningService: RequestSigningService
    private lateinit var testSecretKey: javax.crypto.SecretKey
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        requestSigningService = RequestSigningService(mockCryptographyService, mockProvisioningService)
        
        // Create test secret key
        val keyBytes = "test-secret-key-for-hmac-256".toByteArray()
        testSecretKey = SecretKeySpec(keyBytes, "HmacSHA256")
    }
    
    @Test
    fun testSignRequestSuccess() {
        // Mock provisioning service
        `when`(mockProvisioningService.isDeviceProvisioned()).thenReturn(true)
        `when`(mockProvisioningService.getDeviceId()).thenReturn("test-device-123")
        `when`(mockProvisioningService.getDeviceSecretKey()).thenReturn(testSecretKey)
        
        // Mock cryptography service
        `when`(mockCryptographyService.computeHmacSha256(anyString(), eq(testSecretKey)))
            .thenReturn("mock-signature-base64")
        `when`(mockCryptographyService.hashString(anyString())).thenReturn("mock-body-hash")
        
        // Test request signing
        val signedHeaders = requestSigningService.signRequest(
            method = "POST",
            uri = "/api/v1/transfer",
            body = "{\"amount\": 100}"
        )
        
        assertNotNull("Signed headers should not be null", signedHeaders)
        assertEquals("Device ID should match", "test-device-123", signedHeaders!!.deviceId)
        assertEquals("Signature should match", "mock-signature-base64", signedHeaders.signature)
        assertNotNull("Timestamp should not be null", signedHeaders.timestamp)
        assertNotNull("Nonce should not be null", signedHeaders.nonce)
        assertNotNull("String to sign should not be null", signedHeaders.stringToSign)
        
        // Verify interactions
        verify(mockProvisioningService).isDeviceProvisioned()
        verify(mockProvisioningService).getDeviceId()
        verify(mockProvisioningService).getDeviceSecretKey()
        verify(mockCryptographyService).computeHmacSha256(any(), eq(testSecretKey))
        verify(mockCryptographyService).hashString("{\"amount\": 100}")
    }
    
    @Test
    fun testSignRequestNotProvisioned() {
        // Mock provisioning service - not provisioned
        `when`(mockProvisioningService.isDeviceProvisioned()).thenReturn(false)
        
        // Test request signing
        val signedHeaders = requestSigningService.signRequest(
            method = "POST",
            uri = "/api/v1/transfer",
            body = "{\"amount\": 100}"
        )
        
        assertNull("Signed headers should be null when not provisioned", signedHeaders)
        
        // Verify interactions
        verify(mockProvisioningService).isDeviceProvisioned()
        verify(mockProvisioningService, never()).getDeviceId()
        verify(mockProvisioningService, never()).getDeviceSecretKey()
    }
    
    @Test
    fun testSignRequestNoDeviceId() {
        // Mock provisioning service - provisioned but no device ID
        `when`(mockProvisioningService.isDeviceProvisioned()).thenReturn(true)
        `when`(mockProvisioningService.getDeviceId()).thenReturn(null)
        
        // Test request signing
        val signedHeaders = requestSigningService.signRequest(
            method = "POST",
            uri = "/api/v1/transfer",
            body = "{\"amount\": 100}"
        )
        
        assertNull("Signed headers should be null when device ID is not available", signedHeaders)
        
        // Verify interactions
        verify(mockProvisioningService).isDeviceProvisioned()
        verify(mockProvisioningService).getDeviceId()
        verify(mockProvisioningService, never()).getDeviceSecretKey()
    }
    
    @Test
    fun testSignRequestNoSecretKey() {
        // Mock provisioning service - provisioned but no secret key
        `when`(mockProvisioningService.isDeviceProvisioned()).thenReturn(true)
        `when`(mockProvisioningService.getDeviceId()).thenReturn("test-device-123")
        `when`(mockProvisioningService.getDeviceSecretKey()).thenReturn(null)
        
        // Test request signing
        val signedHeaders = requestSigningService.signRequest(
            method = "POST",
            uri = "/api/v1/transfer",
            body = "{\"amount\": 100}"
        )
        
        assertNull("Signed headers should be null when secret key is not available", signedHeaders)
        
        // Verify interactions
        verify(mockProvisioningService).isDeviceProvisioned()
        verify(mockProvisioningService).getDeviceId()
        verify(mockProvisioningService).getDeviceSecretKey()
    }
    
    @Test
    fun testSignRequestWithNullBody() {
        // Mock provisioning service
        `when`(mockProvisioningService.isDeviceProvisioned()).thenReturn(true)
        `when`(mockProvisioningService.getDeviceId()).thenReturn("test-device-123")
        `when`(mockProvisioningService.getDeviceSecretKey()).thenReturn(testSecretKey)
        
        // Mock cryptography service
        `when`(mockCryptographyService.computeHmacSha256(anyString(), eq(testSecretKey)))
            .thenReturn("mock-signature-base64")
        
        // Test request signing with null body (GET request)
        val signedHeaders = requestSigningService.signRequest(
            method = "GET",
            uri = "/api/v1/balance",
            body = null
        )
        
        assertNotNull("Signed headers should not be null", signedHeaders)
        assertEquals("Device ID should match", "test-device-123", signedHeaders!!.deviceId)
        assertEquals("Signature should match", "mock-signature-base64", signedHeaders.signature)
        
        // Verify that body hash was not computed for null body
        verify(mockCryptographyService, never()).hashString(anyString())
    }
    
    @Test
    fun testCreateHeadersMap() {
        val signedHeaders = SignedRequestHeaders(
            deviceId = "test-device-123",
            signature = "test-signature",
            timestamp = "2025-01-19T12:00:00Z",
            nonce = "test-nonce-123",
            stringToSign = "POST|/api|timestamp|nonce|hash"
        )
        
        val headersMap = requestSigningService.createHeadersMap(signedHeaders)
        
        assertEquals("Device ID header should match", "test-device-123", 
            headersMap[RequestSigningService.HEADER_DEVICE_ID])
        assertEquals("Signature header should match", "test-signature", 
            headersMap[RequestSigningService.HEADER_SIGNATURE])
        assertEquals("Timestamp header should match", "2025-01-19T12:00:00Z", 
            headersMap[RequestSigningService.HEADER_TIMESTAMP])
        assertEquals("Nonce header should match", "test-nonce-123", 
            headersMap[RequestSigningService.HEADER_NONCE])
        assertEquals("Headers map should have correct size", 4, headersMap.size)
    }
    
    @Test
    fun testExtractSigningInfo() {
        val headers = mapOf(
            RequestSigningService.HEADER_DEVICE_ID to "test-device-123",
            RequestSigningService.HEADER_SIGNATURE to "test-signature",
            RequestSigningService.HEADER_TIMESTAMP to "2025-01-19T12:00:00Z",
            RequestSigningService.HEADER_NONCE to "test-nonce-123"
        )
        
        val extractedInfo = requestSigningService.extractSigningInfo(headers)
        
        assertNotNull("Extracted info should not be null", extractedInfo)
        assertEquals("Device ID should match", "test-device-123", extractedInfo!!.deviceId)
        assertEquals("Signature should match", "test-signature", extractedInfo.signature)
        assertEquals("Timestamp should match", "2025-01-19T12:00:00Z", extractedInfo.timestamp)
        assertEquals("Nonce should match", "test-nonce-123", extractedInfo.nonce)
    }
    
    @Test
    fun testExtractSigningInfoIncompleteHeaders() {
        val incompleteHeaders = mapOf(
            RequestSigningService.HEADER_DEVICE_ID to "test-device-123",
            RequestSigningService.HEADER_SIGNATURE to "test-signature"
            // Missing timestamp and nonce
        )
        
        val extractedInfo = requestSigningService.extractSigningInfo(incompleteHeaders)
        
        assertNull("Extracted info should be null for incomplete headers", extractedInfo)
    }
    
    @Test
    fun testVerifyRequestSignature() {
        // Mock provisioning service
        `when`(mockProvisioningService.getDeviceSecretKey()).thenReturn(testSecretKey)
        
        // Mock cryptography service
        `when`(mockCryptographyService.verifyHmacSha256(anyString(), eq("test-signature"), eq(testSecretKey)))
            .thenReturn(true)
        `when`(mockCryptographyService.hashString(anyString())).thenReturn("mock-body-hash")
        
        val isValid = requestSigningService.verifyRequestSignature(
            method = "POST",
            uri = "/api/v1/transfer",
            timestamp = "2025-01-19T12:00:00Z",
            nonce = "test-nonce-123",
            body = "{\"amount\": 100}",
            signature = "test-signature"
        )
        
        assertTrue("Signature verification should succeed", isValid)
        
        // Verify interactions
        verify(mockProvisioningService).getDeviceSecretKey()
        verify(mockCryptographyService).verifyHmacSha256(anyString(), eq("test-signature"), eq(testSecretKey))
    }
    
    @Test
    fun testTimestampValidation() {
        // Test with current timestamp (should be valid)
        val currentTimestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())
        
        val isCurrentValid = requestSigningService.isTimestampValid(currentTimestamp)
        assertTrue("Current timestamp should be valid", isCurrentValid)
        
        // Test with old timestamp (should be invalid)
        val oldTimestamp = "2020-01-01T00:00:00Z"
        val isOldValid = requestSigningService.isTimestampValid(oldTimestamp)
        assertFalse("Old timestamp should be invalid", isOldValid)
        
        // Test with invalid format
        val invalidTimestamp = "invalid-timestamp"
        val isInvalidValid = requestSigningService.isTimestampValid(invalidTimestamp)
        assertFalse("Invalid timestamp format should be invalid", isInvalidValid)
    }
}