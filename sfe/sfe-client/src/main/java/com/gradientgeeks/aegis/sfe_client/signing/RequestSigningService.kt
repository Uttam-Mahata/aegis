package com.gradientgeeks.aegis.sfe_client.signing

import android.util.Log
import com.gradientgeeks.aegis.sfe_client.crypto.CryptographyService
import com.gradientgeeks.aegis.sfe_client.provisioning.DeviceProvisioningService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * Service responsible for signing HTTP requests with HMAC-SHA256 signatures.
 * 
 * Implements the request signing protocol used by the Aegis Security system
 * to ensure request authenticity and integrity. All sensitive API calls
 * should be signed using this service.
 * 
 * The signing process creates a "string to sign" that includes:
 * - HTTP method (GET, POST, etc.)
 * - Request URI path
 * - ISO 8601 timestamp
 * - Cryptographic nonce
 * - SHA-256 hash of request body (if present)
 */
class RequestSigningService(
    private val cryptographyService: CryptographyService,
    private val provisioningService: DeviceProvisioningService
) {
    
    companion object {
        private const val TAG = "RequestSigningService"
        
        // HTTP headers used for signed requests
        const val HEADER_DEVICE_ID = "X-Device-Id"
        const val HEADER_SIGNATURE = "X-Signature"
        const val HEADER_TIMESTAMP = "X-Timestamp"
        const val HEADER_NONCE = "X-Nonce"
        
        // String to sign delimiter
        private const val DELIMITER = "|"
        
        // ISO 8601 date format for timestamps
        private val ISO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
    
    /**
     * Signs an HTTP request and returns the signature headers.
     * 
     * This is the main entry point for request signing. The returned headers
     * should be added to the HTTP request before sending.
     * 
     * @param method HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param uri Request URI path (e.g., "/api/v1/transfer")
     * @param body Request body as string (null for GET requests)
     * @return SignedRequestHeaders containing all necessary headers, or null if signing failed
     */
    fun signRequest(
        method: String,
        uri: String,
        body: String? = null
    ): SignedRequestHeaders? {
        
        return try {
            Log.d(TAG, "Signing request: $method $uri")
            
            // Step 1: Verify device is provisioned
            if (!provisioningService.isDeviceProvisioned()) {
                Log.e(TAG, "Cannot sign request: device is not provisioned")
                return null
            }
            
            val deviceId = provisioningService.getDeviceId()
            if (deviceId == null) {
                Log.e(TAG, "Cannot sign request: device ID not available")
                return null
            }
            
            // Step 2: Generate timestamp and nonce
            val timestamp = generateTimestamp()
            val nonce = generateNonce()
            
            // Step 3: Create string to sign
            val stringToSign = createStringToSign(method, uri, timestamp, nonce, body)
            Log.d(TAG, "String to sign: $stringToSign")
            
            // Step 4: Get device secret key
            val secretKey = provisioningService.getDeviceSecretKey()
            if (secretKey == null) {
                Log.e(TAG, "Cannot sign request: device secret key not available")
                return null
            }
            
            // Step 5: Compute HMAC signature
            val signature = cryptographyService.computeHmacSha256(stringToSign, secretKey)
            if (signature == null) {
                Log.e(TAG, "Failed to compute HMAC signature")
                return null
            }
            
            Log.d(TAG, "Successfully signed request")
            
            // Step 6: Return signed headers
            SignedRequestHeaders(
                deviceId = deviceId,
                signature = signature,
                timestamp = timestamp,
                nonce = nonce,
                stringToSign = stringToSign
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sign request", e)
            null
        }
    }
    
    /**
     * Verifies an HMAC signature for a request (for testing purposes).
     * 
     * This method can be used to verify that signature generation is working
     * correctly during development and testing.
     * 
     * @param method HTTP method
     * @param uri Request URI path
     * @param timestamp Request timestamp
     * @param nonce Request nonce
     * @param body Request body (optional)
     * @param signature Signature to verify
     * @return True if signature is valid, false otherwise
     */
    fun verifyRequestSignature(
        method: String,
        uri: String,
        timestamp: String,
        nonce: String,
        body: String?,
        signature: String
    ): Boolean {
        
        return try {
            Log.d(TAG, "Verifying request signature")
            
            // Step 1: Recreate string to sign
            val stringToSign = createStringToSign(method, uri, timestamp, nonce, body)
            
            // Step 2: Get device secret key
            val secretKey = provisioningService.getDeviceSecretKey()
            if (secretKey == null) {
                Log.e(TAG, "Cannot verify signature: device secret key not available")
                return false
            }
            
            // Step 3: Verify signature
            val isValid = cryptographyService.verifyHmacSha256(stringToSign, signature, secretKey)
            
            Log.d(TAG, "Signature verification result: $isValid")
            isValid
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify request signature", e)
            false
        }
    }
    
    /**
     * Creates the canonical string to sign for HMAC computation.
     * 
     * The string follows this format:
     * METHOD|URI|TIMESTAMP|NONCE|BODY_HASH
     * 
     * @param method HTTP method
     * @param uri Request URI path
     * @param timestamp ISO 8601 timestamp
     * @param nonce Cryptographic nonce
     * @param body Request body (optional)
     * @return The canonical string to sign
     */
    private fun createStringToSign(
        method: String,
        uri: String,
        timestamp: String,
        nonce: String,
        body: String?
    ): String {
        
        // Compute body hash if body is present
        val bodyHash = if (!body.isNullOrEmpty()) {
            cryptographyService.hashString(body)
        } else {
            ""
        }
        
        // Create canonical string
        return listOf(
            method.uppercase(),
            uri,
            timestamp,
            nonce,
            bodyHash
        ).joinToString(DELIMITER)
    }
    
    /**
     * Generates an ISO 8601 timestamp for the current time.
     * 
     * @return ISO 8601 formatted timestamp in UTC
     */
    private fun generateTimestamp(): String {
        return ISO_DATE_FORMAT.format(Date())
    }
    
    /**
     * Generates a cryptographically secure nonce.
     * 
     * @return A unique nonce string
     */
    private fun generateNonce(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }
    
    /**
     * Validates timestamp to ensure it's within acceptable time window.
     * 
     * @param timestamp ISO 8601 timestamp to validate
     * @param windowSeconds Acceptable time window in seconds (default 300 = 5 minutes)
     * @return True if timestamp is within window, false otherwise
     */
    fun isTimestampValid(timestamp: String, windowSeconds: Long = 300): Boolean {
        return try {
            val requestTime = ISO_DATE_FORMAT.parse(timestamp)?.time ?: return false
            val currentTime = System.currentTimeMillis()
            val timeDiff = Math.abs(currentTime - requestTime) / 1000
            
            val isValid = timeDiff <= windowSeconds
            
            if (!isValid) {
                Log.w(TAG, "Timestamp validation failed. Difference: ${timeDiff}s, Window: ${windowSeconds}s")
            }
            
            isValid
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate timestamp: $timestamp", e)
            false
        }
    }
    
    /**
     * Creates headers map for adding to HTTP requests.
     * 
     * @param signedHeaders The signed request headers
     * @return Map of header names to values
     */
    fun createHeadersMap(signedHeaders: SignedRequestHeaders): Map<String, String> {
        return mapOf(
            HEADER_DEVICE_ID to signedHeaders.deviceId,
            HEADER_SIGNATURE to signedHeaders.signature,
            HEADER_TIMESTAMP to signedHeaders.timestamp,
            HEADER_NONCE to signedHeaders.nonce
        )
    }
    
    /**
     * Extracts signing information from HTTP headers (for testing/validation).
     * 
     * @param headers Map of HTTP headers
     * @return ExtractedSigningInfo or null if headers are incomplete
     */
    fun extractSigningInfo(headers: Map<String, String>): ExtractedSigningInfo? {
        val deviceId = headers[HEADER_DEVICE_ID]
        val signature = headers[HEADER_SIGNATURE]
        val timestamp = headers[HEADER_TIMESTAMP]
        val nonce = headers[HEADER_NONCE]
        
        return if (deviceId != null && signature != null && timestamp != null && nonce != null) {
            ExtractedSigningInfo(deviceId, signature, timestamp, nonce)
        } else {
            Log.w(TAG, "Incomplete signing headers in request")
            null
        }
    }
}

/**
 * Data class containing all headers needed for a signed request.
 */
data class SignedRequestHeaders(
    val deviceId: String,
    val signature: String,
    val timestamp: String,
    val nonce: String,
    val stringToSign: String
)

/**
 * Data class containing signing information extracted from headers.
 */
data class ExtractedSigningInfo(
    val deviceId: String,
    val signature: String,
    val timestamp: String,
    val nonce: String
)