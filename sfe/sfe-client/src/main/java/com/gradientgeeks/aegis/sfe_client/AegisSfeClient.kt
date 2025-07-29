package com.gradientgeeks.aegis.sfe_client

import android.content.Context
import android.util.Log
import com.gradientgeeks.aegis.sfe_client.crypto.CryptographyService
import com.gradientgeeks.aegis.sfe_client.network.ApiClientFactory
import com.gradientgeeks.aegis.sfe_client.provisioning.DeviceProvisioningService
import com.gradientgeeks.aegis.sfe_client.provisioning.ProvisioningInfo
import com.gradientgeeks.aegis.sfe_client.provisioning.ProvisioningResult
import com.gradientgeeks.aegis.sfe_client.security.EnvironmentSecurityService
import com.gradientgeeks.aegis.sfe_client.security.IntegrityValidationService
import com.gradientgeeks.aegis.sfe_client.security.SecurityCheckResult
import com.gradientgeeks.aegis.sfe_client.security.DeviceFingerprintingService
import com.gradientgeeks.aegis.sfe_client.signing.RequestSigningService
import com.gradientgeeks.aegis.sfe_client.signing.SignedRequestHeaders
import com.gradientgeeks.aegis.sfe_client.storage.SecureVaultService
import com.gradientgeeks.aegis.sfe_client.session.SessionKeyManager
import com.gradientgeeks.aegis.sfe_client.session.KeyExchangeService
import com.gradientgeeks.aegis.sfe_client.encryption.PayloadEncryptionService
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main facade class for the Aegis SFE (Secure Frontend Environment) Client SDK.
 * 
 * This is the primary interface that client applications use to interact with
 * the Aegis Security Environment. It provides a simplified, high-level API
 * that encapsulates all the security functionality.
 * 
 * Key Features:
 * - Device provisioning and registration
 * - HMAC request signing
 * - Secure data storage (vault)
 * - Hardware-backed cryptographic operations
 * - Session-based encryption with key exchange
 * 
 * Usage:
 * ```kotlin
 * val aegisClient = AegisSfeClient.initialize(context, "https://api.aegis.example.com")
 * 
 * // Provision device (first time only)
 * val result = aegisClient.provisionDevice("CLIENT_ID", "REGISTRATION_KEY")
 * 
 * // Sign requests
 * val headers = aegisClient.signRequest("POST", "/api/transfer", requestBody)
 * 
 * // Store data securely
 * aegisClient.storeSecurely("user_preferences", userPrefs)
 * ```
 */
class AegisSfeClient private constructor(
    private val context: Context,
    private val baseUrl: String,
    private val enableLogging: Boolean
) {
    
    companion object {
        private const val TAG = "AegisSfeClient"
        
        /**
         * Initializes the Aegis SFE Client SDK.
         * 
         * This method should be called once during application startup to
         * configure the SDK with the appropriate API endpoint.
         * 
         * @param context Application context
         * @param baseUrl Base URL of the Aegis Security API
         * @param enableLogging Whether to enable HTTP request/response logging
         * @return Configured AegisSfeClient instance
         */
        @JvmStatic
        @JvmOverloads
        fun initialize(
            context: Context,
            baseUrl: String,
            enableLogging: Boolean = false
        ): AegisSfeClient {
            Log.i(TAG, "Initializing Aegis SFE Client SDK")
            Log.d(TAG, "Base URL: $baseUrl")
            Log.d(TAG, "Logging enabled: $enableLogging")
            
            return AegisSfeClient(
                context.applicationContext,
                baseUrl,
                enableLogging
            )
        }
    }
    
    // Core services
    private val cryptographyService = CryptographyService(context)
    private val apiService = ApiClientFactory.createAegisApiService(baseUrl, enableLogging)
    private val integrityService = IntegrityValidationService(context)
    private val fingerprintingService = DeviceFingerprintingService(context)
    private val provisioningService = DeviceProvisioningService(
        context, apiService, cryptographyService, integrityService, fingerprintingService
    )
    private val signingService = RequestSigningService(cryptographyService, provisioningService)
    private val vaultService = SecureVaultService(context)
    private val environmentSecurityService = EnvironmentSecurityService(context)
    private val sessionKeyManager = SessionKeyManager(context)
    private val payloadEncryptionService = PayloadEncryptionService()
    
    /**
     * Checks if the device is already provisioned with valid credentials.
     * 
     * @return True if device is provisioned, false otherwise
     */
    fun isDeviceProvisioned(): Boolean {
        return provisioningService.isDeviceProvisioned()
    }
    
    /**
     * Provisions the device with the Aegis Security API.
     * 
     * This establishes a secure identity for the device and should only be
     * called once per app installation. The registration key must be provided
     * by the organization's administrators.
     * 
     * @param clientId Client identifier (e.g., "UCOBANK_PROD_ANDROID")
     * @param registrationKey Shared registration key from administrators
     * @return ProvisioningResult indicating success or failure
     */
    suspend fun provisionDevice(
        clientId: String,
        registrationKey: String
    ): ProvisioningResult {
        Log.i(TAG, "Starting device provisioning")
        
        // Security check is commented out - focusing on HMAC validation and key exchange
        /*
        val securityCheck = performSecurityCheck()
        if (!securityCheck.isSecure) {
            Log.w(TAG, "Security concerns detected during provisioning")
            Log.w(TAG, "Root: ${securityCheck.rootDetected}, Emulator: ${securityCheck.emulatorDetected}")
            // In production, you might want to fail here or prompt user
        }
        */
        
        return provisioningService.provisionDevice(clientId, registrationKey)
    }
    
    /**
     * Gets current device provisioning information.
     * 
     * @return ProvisioningInfo with current status and details
     */
    fun getProvisioningInfo(): ProvisioningInfo {
        return provisioningService.getProvisioningInfo()
    }
    
    /**
     * Clears all provisioning data (for testing/reset purposes).
     * 
     * @return True if cleanup was successful, false otherwise
     */
    fun clearProvisioningData(): Boolean {
        Log.w(TAG, "Clearing device provisioning data")
        return provisioningService.clearProvisioningData()
    }
    
    /**
     * Signs an HTTP request with HMAC-SHA256 signature.
     * 
     * This is the core security function that ensures request authenticity
     * and integrity. The returned headers should be added to the HTTP request.
     * 
     * @param method HTTP method (GET, POST, PUT, DELETE, etc.)
     * @param uri Request URI path (e.g., "/api/v1/transfer")
     * @param body Request body as string (null for GET requests)
     * @return SignedRequestHeaders with all necessary headers, or null if signing failed
     */
    fun signRequest(
        method: String,
        uri: String,
        body: String? = null
    ): SignedRequestHeaders? {
        if (!isDeviceProvisioned()) {
            Log.e(TAG, "Cannot sign request: device is not provisioned")
            return null
        }
        
        return signingService.signRequest(method, uri, body)
    }
    
    /**
     * Creates a map of HTTP headers for the signed request.
     * 
     * Convenience method that converts SignedRequestHeaders to a Map
     * for easier integration with HTTP libraries.
     * 
     * @param signedHeaders The signed request headers
     * @return Map of header names to values
     */
    fun createHeadersMap(signedHeaders: SignedRequestHeaders): Map<String, String> {
        return signingService.createHeadersMap(signedHeaders)
    }
    
    /**
     * Stores data securely in the encrypted vault.
     * 
     * Uses AES-256 envelope encryption with hardware-backed keys to protect
     * sensitive data at rest. Data is automatically serialized to JSON.
     * 
     * @param key Identifier for the stored data
     * @param data Data to store (any serializable object)
     * @return True if storage was successful, false otherwise
     */
    fun <T> storeSecurely(key: String, data: T): Boolean {
        Log.d(TAG, "Storing data securely with key: $key")
        return vaultService.storeSecurely(key, data)
    }
    
    /**
     * Retrieves and decrypts data from the secure vault.
     * 
     * @param key Identifier for the stored data
     * @param type Class type of the data to retrieve
     * @return Decrypted data or null if not found or decryption failed
     */
    fun <T> retrieveSecurely(key: String, type: Class<T>): T? {
        Log.d(TAG, "Retrieving data securely with key: $key")
        return vaultService.retrieveSecurely(key, type)
    }
    
    /**
     * Checks if data exists in the secure vault.
     * 
     * @param key Identifier to check
     * @return True if data exists, false otherwise
     */
    fun vaultContains(key: String): Boolean {
        return vaultService.exists(key)
    }
    
    /**
     * Removes data from the secure vault.
     * 
     * @param key Identifier of data to remove
     * @return True if removal was successful, false otherwise
     */
    fun removeFromVault(key: String): Boolean {
        Log.d(TAG, "Removing data from vault with key: $key")
        return vaultService.remove(key)
    }
    
    /**
     * Lists all keys stored in the secure vault.
     * 
     * @return List of stored keys
     */
    fun listVaultKeys(): List<String> {
        return vaultService.listKeys()
    }
    
    /**
     * Clears all data from the secure vault.
     * 
     * @return True if clearing was successful, false otherwise
     */
    fun clearVault(): Boolean {
        Log.w(TAG, "Clearing all vault data")
        return vaultService.clearAll()
    }
    
    /**
     * Performs a security check of the runtime environment.
     * 
     * Note: Classical security checks (root, emulator, debug mode) have been 
     * disabled to focus on innovative solutions like HMAC validation and key exchange.
     * 
     * @return SecurityCheckResult with default secure status
     */
    fun performSecurityCheck(): SecurityCheckResult {
        Log.d(TAG, "Performing runtime security check")
        return environmentSecurityService.performSecurityCheck()
    }
    
    /**
     * Checks if Google Play Integrity API is available on this device.
     * 
     * @return True if Integrity API is available, false otherwise
     */
    fun isIntegrityApiAvailable(): Boolean {
        return integrityService.isIntegrityApiAvailable()
    }
    
    /**
     * Gets information about integrity validation capabilities.
     * 
     * @return IntegrityInfo with current status and capabilities
     */
    fun getIntegrityInfo() = integrityService.getIntegrityInfo()
    
    /**
     * Validates an HMAC signature (for testing purposes).
     * 
     * This method can be used during development to verify that signature
     * generation is working correctly.
     * 
     * @param method HTTP method
     * @param uri Request URI path
     * @param timestamp Request timestamp
     * @param nonce Request nonce
     * @param body Request body (optional)
     * @param signature Signature to verify
     * @return True if signature is valid, false otherwise
     */
    fun validateSignature(
        method: String,
        uri: String,
        timestamp: String,
        nonce: String,
        body: String?,
        signature: String
    ): Boolean {
        return signingService.verifyRequestSignature(
            method, uri, timestamp, nonce, body, signature
        )
    }
    
    /**
     * Gets the current device ID if provisioned.
     * 
     * @return Device ID or null if not provisioned
     */
    fun getDeviceId(): String? {
        return provisioningService.getDeviceId()
    }
    
    /**
     * Gets the client ID used during provisioning.
     * 
     * @return Client ID or null if not provisioned
     */
    fun getClientId(): String? {
        return provisioningService.getClientId()
    }
    
    /**
     * Gets SDK version and build information.
     * 
     * @return SdkInfo with version details
     */
    fun getSdkInfo(): SdkInfo {
        return SdkInfo(
            version = "1.0.0",
            buildNumber = "2025.01.19.001",
            apiVersion = "v1",
            supportedAndroidVersion = "API 28+",
            features = listOf(
                "Device Provisioning",
                "HMAC-SHA256 Request Signing",
                "AES-256 Secure Storage",
                "Hardware-backed Keystore",
                // "Runtime Security Checks",  // Classical checks commented out
                // "Root Detection",           // Classical checks commented out
                // "Emulator Detection",       // Classical checks commented out
                "Session-based Encryption",
                "ECDH Key Exchange",
                "AES-256-GCM Payload Encryption"
            )
        )
    }
    
    /**
     * Gets access to the provisioning service for advanced operations.
     * 
     * @return DeviceProvisioningService instance
     */
    fun getProvisioningService(): DeviceProvisioningService {
        return provisioningService
    }
    
    // Session-based encryption methods
    
    /**
     * Checks if there's an active session for encrypted communication.
     * 
     * @return True if active session exists, false otherwise
     */
    fun hasActiveSession(): Boolean {
        return sessionKeyManager.hasActiveSession()
    }
    
    /**
     * Initiates a new session key exchange.
     * This should be called after device provisioning and authentication.
     * 
     * @return Key exchange request to send to server, or null on failure
     */
    suspend fun initiateSession(): KeyExchangeService.KeyExchangeRequest? {
        if (!isDeviceProvisioned()) {
            Log.e(TAG, "Cannot initiate session: device is not provisioned")
            return null
        }
        
        Log.i(TAG, "Initiating new session key exchange")
        return sessionKeyManager.initiateSession()
    }
    
    /**
     * Establishes a session using the server's key exchange response.
     * 
     * @param response Server's key exchange response
     * @return True if session was established successfully
     */
    suspend fun establishSession(response: KeyExchangeService.KeyExchangeResponse): Boolean {
        Log.i(TAG, "Establishing session: ${response.sessionId}")
        return sessionKeyManager.establishSession(response)
    }
    
    /**
     * Gets the current session ID.
     * 
     * @return Current session ID or null if no active session
     */
    fun getCurrentSessionId(): String? {
        return sessionKeyManager.getCurrentSessionId()
    }
    
    /**
     * Encrypts a payload using the current session key.
     * 
     * @param payload Data to encrypt
     * @param associatedData Optional additional authenticated data
     * @return Encrypted payload or null on failure
     */
    fun encryptPayload(
        payload: String,
        associatedData: String? = null
    ): PayloadEncryptionService.EncryptedPayload? {
        val sessionKey = sessionKeyManager.getCurrentSessionKey()
        if (sessionKey == null) {
            Log.e(TAG, "Cannot encrypt: no active session")
            return null
        }
        
        return payloadEncryptionService.encryptPayload(payload, sessionKey, associatedData)
    }
    
    /**
     * Decrypts a payload using the current session key.
     * 
     * @param encryptedPayload Encrypted payload to decrypt
     * @return Decrypted data or null on failure
     */
    fun decryptPayload(
        encryptedPayload: PayloadEncryptionService.EncryptedPayload
    ): String? {
        val sessionKey = sessionKeyManager.getCurrentSessionKey()
        if (sessionKey == null) {
            Log.e(TAG, "Cannot decrypt: no active session")
            return null
        }
        
        return payloadEncryptionService.decryptPayload(encryptedPayload, sessionKey)
    }
    
    /**
     * Creates a secure request with encrypted payload.
     * 
     * @param payload Request payload to encrypt
     * @param method HTTP method
     * @param uri Request URI
     * @return Secure request or null on failure
     */
    fun createSecureRequest(
        payload: String,
        method: String,
        uri: String
    ): PayloadEncryptionService.SecureRequest? {
        val sessionKey = sessionKeyManager.getCurrentSessionKey()
        val sessionId = sessionKeyManager.getCurrentSessionId()
        
        if (sessionKey == null || sessionId == null) {
            Log.e(TAG, "Cannot create secure request: no active session")
            return null
        }
        
        val metadata = PayloadEncryptionService.RequestMetadata(
            method = method,
            uri = uri,
            timestamp = System.currentTimeMillis(),
            sessionId = sessionId
        )
        
        return payloadEncryptionService.createSecureRequest(payload, sessionKey, metadata)
    }
    
    /**
     * Extracts payload from a secure response.
     * 
     * @param secureResponse Secure response from server
     * @param expectedMetadata Optional metadata for validation
     * @return Decrypted payload or null on failure
     */
    fun extractSecureResponse(
        secureResponse: PayloadEncryptionService.SecureResponse,
        expectedMetadata: PayloadEncryptionService.ResponseMetadata? = null
    ): String? {
        val sessionKey = sessionKeyManager.getCurrentSessionKey()
        if (sessionKey == null) {
            Log.e(TAG, "Cannot extract response: no active session")
            return null
        }
        
        return payloadEncryptionService.extractSecureResponse(
            secureResponse, sessionKey, expectedMetadata
        )
    }
    
    /**
     * Clears the current session and all associated keys.
     */
    fun clearSession() {
        Log.w(TAG, "Clearing current session")
        sessionKeyManager.clearSession()
    }
    
    /**
     * Checks if the current session needs refresh.
     * 
     * @return True if session needs refresh (expiring soon)
     */
    fun sessionNeedsRefresh(): Boolean {
        return sessionKeyManager.needsRefresh()
    }
    
    /**
     * Gets remaining time for current session in milliseconds.
     * 
     * @return Remaining time or 0 if no active session
     */
    fun getSessionRemainingTime(): Long {
        return sessionKeyManager.getSessionRemainingTime()
    }
}

/**
 * SDK information for debugging and support purposes.
 */
data class SdkInfo(
    val version: String,
    val buildNumber: String,
    val apiVersion: String,
    val supportedAndroidVersion: String,
    val features: List<String>
)