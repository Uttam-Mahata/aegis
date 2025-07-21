package com.gradientgeeks.aegis.sfe_client.crypto

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import javax.crypto.spec.SecretKeySpec

/**
 * Unit tests for CryptographyService.
 * 
 * Tests the core cryptographic operations including HMAC signing,
 * verification, and utility functions.
 */
class CryptographyServiceTest {
    
    private lateinit var cryptographyService: CryptographyService
    
    @Before
    fun setUp() {
        cryptographyService = CryptographyService(null)  // Pass null for unit tests
    }
    
    @Test
    fun testHmacSha256Computation() {
        // Create a test secret key
        val keyBytes = "test-secret-key-for-hmac-256".toByteArray()
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
        
        val testData = "This is test data for HMAC computation"
        
        // Compute HMAC
        val signature = cryptographyService.computeHmacSha256(testData, secretKey)
        
        assertNotNull("HMAC signature should not be null", signature)
        assertTrue("HMAC signature should not be empty", signature!!.isNotEmpty())
        assertTrue("HMAC signature should be valid Base64", 
            cryptographyService.isValidBase64(signature))
    }
    
    @Test
    fun testHmacSha256Verification() {
        // Create a test secret key
        val keyBytes = "test-secret-key-for-hmac-256".toByteArray()
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
        
        val testData = "This is test data for HMAC verification"
        
        // Compute HMAC
        val signature = cryptographyService.computeHmacSha256(testData, secretKey)
        assertNotNull("HMAC signature should not be null", signature)
        
        // Verify HMAC
        val isValid = cryptographyService.verifyHmacSha256(testData, signature!!, secretKey)
        assertTrue("HMAC verification should succeed", isValid)
    }
    
    @Test
    fun testHmacSha256VerificationWithWrongData() {
        // Create a test secret key
        val keyBytes = "test-secret-key-for-hmac-256".toByteArray()
        val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
        
        val testData = "This is test data for HMAC verification"
        val wrongData = "This is WRONG data for HMAC verification"
        
        // Compute HMAC with original data
        val signature = cryptographyService.computeHmacSha256(testData, secretKey)
        assertNotNull("HMAC signature should not be null", signature)
        
        // Verify HMAC with wrong data
        val isValid = cryptographyService.verifyHmacSha256(wrongData, signature!!, secretKey)
        assertFalse("HMAC verification should fail with wrong data", isValid)
    }
    
    @Test
    fun testHmacSha256VerificationWithWrongKey() {
        // Create test secret keys
        val keyBytes1 = "test-secret-key-for-hmac-256".toByteArray()
        val keyBytes2 = "different-secret-key-for-hmac".toByteArray()
        val secretKey1 = SecretKeySpec(keyBytes1, "HmacSHA256")
        val secretKey2 = SecretKeySpec(keyBytes2, "HmacSHA256")
        
        val testData = "This is test data for HMAC verification"
        
        // Compute HMAC with first key
        val signature = cryptographyService.computeHmacSha256(testData, secretKey1)
        assertNotNull("HMAC signature should not be null", signature)
        
        // Verify HMAC with second key
        val isValid = cryptographyService.verifyHmacSha256(testData, signature!!, secretKey2)
        assertFalse("HMAC verification should fail with wrong key", isValid)
    }
    
    @Test
    fun testRegistrationKeyGeneration() {
        val registrationKey = cryptographyService.generateRegistrationKey()
        
        assertNotNull("Registration key should not be null", registrationKey)
        assertTrue("Registration key should not be empty", registrationKey.isNotEmpty())
        assertTrue("Registration key should be reasonable length", 
            registrationKey.length > 10)
    }
    
    @Test
    fun testRegistrationKeyGenerationUniqueness() {
        val key1 = cryptographyService.generateRegistrationKey()
        val key2 = cryptographyService.generateRegistrationKey()
        
        assertNotEquals("Registration keys should be unique", key1, key2)
    }
    
    @Test
    fun testSha256Hashing() {
        val testInput = "This is test input for SHA-256 hashing"
        val hash = cryptographyService.hashString(testInput)
        
        assertNotNull("Hash should not be null", hash)
        assertTrue("Hash should not be empty", hash.isNotEmpty())
        assertEquals("SHA-256 hash should be 64 hex characters", 64, hash.length)
        
        // Verify hash contains only hex characters
        assertTrue("Hash should contain only hex characters", 
            hash.matches(Regex("[0-9a-f]+")))
    }
    
    @Test
    fun testSha256HashingConsistency() {
        val testInput = "This is test input for SHA-256 consistency"
        val hash1 = cryptographyService.hashString(testInput)
        val hash2 = cryptographyService.hashString(testInput)
        
        assertEquals("Hash should be consistent for same input", hash1, hash2)
    }
    
    @Test
    fun testSha256HashingDifferentInputs() {
        val input1 = "This is test input 1"
        val input2 = "This is test input 2"
        
        val hash1 = cryptographyService.hashString(input1)
        val hash2 = cryptographyService.hashString(input2)
        
        assertNotEquals("Different inputs should produce different hashes", hash1, hash2)
    }
    
    @Test
    fun testBase64Validation() {
        val validBase64 = "SGVsbG8gV29ybGQ="  // "Hello World" in Base64
        val invalidBase64 = "This is not Base64!"
        
        assertTrue("Valid Base64 should pass validation", 
            cryptographyService.isValidBase64(validBase64))
        assertFalse("Invalid Base64 should fail validation", 
            cryptographyService.isValidBase64(invalidBase64))
    }
    
    @Test
    fun testBase64ValidationWithPadding() {
        val base64WithPadding = "SGVsbG8="  // "Hello" in Base64 with padding
        val base64WithoutPadding = "SGVsbG8"  // "Hello" in Base64 without padding
        
        assertTrue("Base64 with padding should pass validation", 
            cryptographyService.isValidBase64(base64WithPadding))
        assertTrue("Base64 without padding should pass validation", 
            cryptographyService.isValidBase64(base64WithoutPadding))
    }
    
    @Test
    fun testEmptyInputHandling() {
        val emptyString = ""
        val hash = cryptographyService.hashString(emptyString)
        
        assertNotNull("Hash of empty string should not be null", hash)
        assertTrue("Hash of empty string should not be empty", hash.isNotEmpty())
        assertEquals("SHA-256 hash should be 64 hex characters even for empty input", 
            64, hash.length)
    }
}