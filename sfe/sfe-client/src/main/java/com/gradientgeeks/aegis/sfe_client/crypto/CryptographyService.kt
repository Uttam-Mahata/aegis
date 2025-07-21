package com.gradientgeeks.aegis.sfe_client.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Core cryptographic service for the Aegis SFE Client SDK.
 * 
 * Provides secure cryptographic operations including:
 * - HMAC-SHA256 signing and verification
 * - Secure key generation and storage in Android Keystore
 * - SHA-256 hashing utilities
 * 
 * All cryptographic operations follow enterprise-grade security standards
 * and use hardware-backed security when available.
 */
class CryptographyService(private val context: Context? = null) {
    
    companion object {
        private const val TAG = "CryptographyService"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val HMAC_SHA256_ALGORITHM = "HmacSHA256"
        private const val SHA256_ALGORITHM = "SHA-256"
        
        // Secure random instance for key generation
        private val SECURE_RANDOM = SecureRandom()
    }
    
    private val secureKeyStorage: SecureKeyStorage? = context?.let { SecureKeyStorage(it) }
    
    /**
     * Generates a cryptographically secure secret key and stores it in Android Keystore.
     * 
     * The key is generated with 256-bit entropy and stored securely in hardware-backed
     * storage when available. The key is bound to the app and cannot be extracted.
     * 
     * @param alias The alias to store the key under in the keystore
     * @return The generated secret key, or null if generation failed
     */
    fun generateAndStoreSecretKey(alias: String): SecretKey? {
        return try {
            Log.d(TAG, "Generating secret key with alias: $alias")
            
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256, 
                ANDROID_KEYSTORE
            )
            
            val keySpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setKeySize(256) // 256-bit key for strong security
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false) // Allow background usage
                .build()
            
            keyGenerator.init(keySpec)
            val secretKey = keyGenerator.generateKey()
            
            Log.d(TAG, "Successfully generated secret key with alias: $alias")
            secretKey
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate secret key with alias: $alias", e)
            null
        }
    }
    
    /**
     * Retrieves a secret key by alias.
     * 
     * First checks Android Keystore for generated keys, then checks
     * secure storage for external keys.
     * 
     * @param alias The alias of the key to retrieve
     * @return The secret key, or null if not found or retrieval failed
     */
    fun getSecretKey(alias: String): SecretKey? {
        return try {
            Log.d(TAG, "Retrieving secret key with alias: $alias")
            
            // First try Android Keystore for generated keys
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            val keystoreKey = keyStore.getKey(alias, null) as? SecretKey
            if (keystoreKey != null) {
                Log.d(TAG, "Successfully retrieved secret key from Android Keystore with alias: $alias")
                return keystoreKey
            }
            
            // If not found in keystore, try secure storage for external keys
            if (secureKeyStorage != null) {
                val externalKey = secureKeyStorage.getExternalKey(alias)
                if (externalKey != null) {
                    Log.d(TAG, "Successfully retrieved external key from secure storage with alias: $alias")
                    return externalKey
                }
            }
            
            Log.w(TAG, "Secret key not found with alias: $alias")
            null
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve secret key with alias: $alias", e)
            null
        }
    }
    
    /**
     * Stores an externally provided secret key securely.
     * 
     * This method is used when receiving a secret key from the Aegis API
     * during device provisioning. Uses SecureKeyStorage for secure storage.
     * 
     * @param alias The alias to store the key under
     * @param keyBytes The raw key material as byte array
     * @return True if storage was successful, false otherwise
     */
    fun storeExternalSecretKey(alias: String, keyBytes: ByteArray): Boolean {
        return try {
            Log.d(TAG, "Storing external secret key with alias: $alias")
            
            if (secureKeyStorage == null) {
                Log.e(TAG, "SecureKeyStorage not available - context required")
                return false
            }
            
            // Store the external key using secure storage
            val success = secureKeyStorage.storeExternalKey(alias, keyBytes)
            
            if (success) {
                Log.d(TAG, "Successfully stored external secret key with alias: $alias")
            } else {
                Log.e(TAG, "Failed to store external secret key with alias: $alias")
            }
            
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store external secret key with alias: $alias", e)
            false
        }
    }
    
    /**
     * Computes HMAC-SHA256 signature for the given data using the specified key.
     * 
     * @param data The data to sign
     * @param secretKey The secret key to use for signing
     * @return Base64-encoded signature, or null if signing failed
     */
    fun computeHmacSha256(data: String, secretKey: SecretKey): String? {
        return try {
            Log.d(TAG, "Computing HMAC-SHA256 signature")
            
            val mac = Mac.getInstance(HMAC_SHA256_ALGORITHM)
            mac.init(secretKey)
            
            val hashBytes = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            val signature = Base64.encodeToString(hashBytes, Base64.NO_WRAP)
            
            Log.d(TAG, "Successfully computed HMAC-SHA256 signature")
            signature
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute HMAC-SHA256 signature", e)
            null
        }
    }
    
    /**
     * Computes HMAC-SHA256 signature using a key stored in keystore by alias.
     * 
     * @param data The data to sign
     * @param keyAlias The alias of the key in keystore
     * @return Base64-encoded signature, or null if signing failed
     */
    fun computeHmacSha256WithAlias(data: String, keyAlias: String): String? {
        val secretKey = getSecretKey(keyAlias)
        return if (secretKey != null) {
            computeHmacSha256(data, secretKey)
        } else {
            Log.e(TAG, "Cannot compute HMAC: key not found with alias $keyAlias")
            null
        }
    }
    
    /**
     * Verifies an HMAC-SHA256 signature against the provided data and key.
     * 
     * Uses constant-time comparison to prevent timing attacks.
     * 
     * @param data The original data that was signed
     * @param signature The Base64-encoded signature to verify
     * @param secretKey The secret key used for verification
     * @return True if signature is valid, false otherwise
     */
    fun verifyHmacSha256(data: String, signature: String, secretKey: SecretKey): Boolean {
        return try {
            Log.d(TAG, "Verifying HMAC-SHA256 signature")
            
            val expectedSignature = computeHmacSha256(data, secretKey)
            if (expectedSignature == null) {
                Log.e(TAG, "Failed to compute expected signature for verification")
                return false
            }
            
            // Constant-time comparison to prevent timing attacks
            val isValid = MessageDigest.isEqual(
                expectedSignature.toByteArray(StandardCharsets.UTF_8),
                signature.toByteArray(StandardCharsets.UTF_8)
            )
            
            Log.d(TAG, "HMAC-SHA256 signature verification result: $isValid")
            isValid
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to verify HMAC-SHA256 signature", e)
            false
        }
    }
    
    /**
     * Verifies an HMAC-SHA256 signature using a key stored in keystore by alias.
     * 
     * @param data The original data that was signed
     * @param signature The Base64-encoded signature to verify
     * @param keyAlias The alias of the key in keystore
     * @return True if signature is valid, false otherwise
     */
    fun verifyHmacSha256WithAlias(data: String, signature: String, keyAlias: String): Boolean {
        val secretKey = getSecretKey(keyAlias)
        return if (secretKey != null) {
            verifyHmacSha256(data, signature, secretKey)
        } else {
            Log.e(TAG, "Cannot verify HMAC: key not found with alias $keyAlias")
            false
        }
    }
    
    /**
     * Generates a cryptographically secure random string for registration keys.
     * 
     * Uses the same algorithm as the backend service for consistency.
     * 
     * @param bitLength The bit length of entropy (default 256)
     * @return Base32-encoded random string
     */
    fun generateRegistrationKey(bitLength: Int = 256): String {
        Log.d(TAG, "Generating registration key with $bitLength bits of entropy")
        
        return BigInteger(bitLength, SECURE_RANDOM).toString(32)
    }
    
    /**
     * Computes SHA-256 hash of the input string.
     * 
     * @param input The string to hash
     * @return Hex-encoded SHA-256 hash
     */
    fun hashString(input: String): String {
        return try {
            Log.d(TAG, "Computing SHA-256 hash")
            
            val digest = MessageDigest.getInstance(SHA256_ALGORITHM)
            val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
            
            // Convert to hex string
            hashBytes.joinToString("") { "%02x".format(it) }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute SHA-256 hash", e)
            ""
        }
    }
    
    /**
     * Validates that a string is valid Base64 format.
     * 
     * @param base64String The string to validate
     * @return True if valid Base64, false otherwise
     */
    fun isValidBase64(base64String: String): Boolean {
        return try {
            Base64.decode(base64String, Base64.NO_WRAP)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if a key exists.
     * 
     * Checks both Android Keystore and secure storage.
     * 
     * @param alias The alias to check for
     * @return True if key exists, false otherwise
     */
    fun keyExists(alias: String): Boolean {
        return try {
            // Check Android Keystore
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            if (keyStore.containsAlias(alias)) {
                return true
            }
            
            // Check secure storage
            secureKeyStorage?.keyExists(alias) ?: false
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if key exists with alias: $alias", e)
            false
        }
    }
    
    /**
     * Deletes a key.
     * 
     * Attempts to delete from both Android Keystore and secure storage.
     * 
     * @param alias The alias of the key to delete
     * @return True if deletion was successful, false otherwise
     */
    fun deleteKey(alias: String): Boolean {
        return try {
            Log.d(TAG, "Deleting key with alias: $alias")
            
            var deleted = false
            
            // Try to delete from Android Keystore
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                Log.d(TAG, "Successfully deleted key from Android Keystore with alias: $alias")
                deleted = true
            }
            
            // Try to delete from secure storage
            if (secureKeyStorage?.deleteKey(alias) == true) {
                Log.d(TAG, "Successfully deleted key from secure storage with alias: $alias")
                deleted = true
            }
            
            if (!deleted) {
                Log.w(TAG, "Key not found for deletion with alias: $alias")
            }
            
            deleted
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete key with alias: $alias", e)
            false
        }
    }
}