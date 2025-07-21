package com.gradientgeeks.aegis.sfe_client.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure Vault Service for encrypted data storage at rest.
 * 
 * Provides AES-256 envelope encryption where:
 * 1. Data is encrypted with a random AES-256 key
 * 2. The AES key is encrypted with an RSA key stored in Android Keystore
 * 3. Both encrypted data and encrypted key are stored together
 * 
 * This approach ensures maximum security - even if device storage is compromised,
 * the data remains protected by hardware-backed encryption.
 */
class SecureVaultService(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureVaultService"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val RSA_KEY_ALIAS = "aegis_vault_master_key"
        private const val VAULT_DIR = "aegis_vault"
        
        // Encryption constants
        private const val AES_ALGORITHM = "AES"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING"
        private const val AES_KEY_SIZE = 256
        private const val GCM_IV_SIZE = 12
        private const val GCM_TAG_SIZE = 128
        
        private val SECURE_RANDOM = SecureRandom()
    }
    
    private val gson = Gson()
    private val vaultDir: File by lazy {
        File(context.filesDir, VAULT_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    init {
        initializeMasterKey()
    }
    
    /**
     * Stores data securely in the vault using envelope encryption.
     * 
     * @param key The identifier for the stored data
     * @param data The data to store (any serializable object)
     * @return True if storage was successful, false otherwise
     */
    fun <T> storeSecurely(key: String, data: T): Boolean {
        return try {
            Log.d(TAG, "Storing data securely with key: $key")
            
            // Step 1: Serialize data to JSON
            val jsonData = gson.toJson(data)
            val dataBytes = jsonData.toByteArray(StandardCharsets.UTF_8)
            
            // Step 2: Generate random AES key
            val aesKey = generateAESKey()
            
            // Step 3: Encrypt data with AES
            val encryptedData = encryptWithAES(dataBytes, aesKey)
            if (encryptedData == null) {
                Log.e(TAG, "Failed to encrypt data with AES")
                return false
            }
            
            // Step 4: Encrypt AES key with RSA master key
            val encryptedAESKey = encryptAESKeyWithRSA(aesKey)
            if (encryptedAESKey == null) {
                Log.e(TAG, "Failed to encrypt AES key with RSA")
                return false
            }
            
            // Step 5: Create envelope structure
            val envelope = SecureEnvelope(
                encryptedData = Base64.encodeToString(encryptedData.encryptedBytes, Base64.NO_WRAP),
                iv = Base64.encodeToString(encryptedData.iv, Base64.NO_WRAP),
                encryptedAESKey = Base64.encodeToString(encryptedAESKey, Base64.NO_WRAP),
                timestamp = System.currentTimeMillis()
            )
            
            // Step 6: Save envelope to file
            val file = File(vaultDir, "$key.vault")
            val envelopeJson = gson.toJson(envelope)
            
            FileOutputStream(file).use { output ->
                output.write(envelopeJson.toByteArray(StandardCharsets.UTF_8))
            }
            
            Log.d(TAG, "Successfully stored data securely with key: $key")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store data securely with key: $key", e)
            false
        }
    }
    
    /**
     * Retrieves and decrypts data from the vault.
     * 
     * @param key The identifier for the stored data
     * @param type The type of data to deserialize
     * @return The decrypted data or null if not found or decryption failed
     */
    fun <T> retrieveSecurely(key: String, type: Class<T>): T? {
        return try {
            Log.d(TAG, "Retrieving data securely with key: $key")
            
            // Step 1: Load envelope from file
            val file = File(vaultDir, "$key.vault")
            if (!file.exists()) {
                Log.w(TAG, "Vault file not found for key: $key")
                return null
            }
            
            val envelopeJson = FileInputStream(file).use { input ->
                input.readBytes().toString(StandardCharsets.UTF_8)
            }
            
            val envelope = gson.fromJson(envelopeJson, SecureEnvelope::class.java)
            
            // Step 2: Decrypt AES key with RSA master key
            val encryptedAESKeyBytes = Base64.decode(envelope.encryptedAESKey, Base64.NO_WRAP)
            val aesKeyBytes = decryptAESKeyWithRSA(encryptedAESKeyBytes)
            if (aesKeyBytes == null) {
                Log.e(TAG, "Failed to decrypt AES key with RSA")
                return null
            }
            
            val aesKey = SecretKeySpec(aesKeyBytes, AES_ALGORITHM)
            
            // Step 3: Decrypt data with AES key
            val encryptedDataBytes = Base64.decode(envelope.encryptedData, Base64.NO_WRAP)
            val iv = Base64.decode(envelope.iv, Base64.NO_WRAP)
            
            val decryptedBytes = decryptWithAES(
                EncryptedData(encryptedDataBytes, iv),
                aesKey
            )
            
            if (decryptedBytes == null) {
                Log.e(TAG, "Failed to decrypt data with AES")
                return null
            }
            
            // Step 4: Deserialize data from JSON
            val jsonData = String(decryptedBytes, StandardCharsets.UTF_8)
            val data = gson.fromJson(jsonData, type)
            
            Log.d(TAG, "Successfully retrieved data securely with key: $key")
            data
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve data securely with key: $key", e)
            null
        }
    }
    
    /**
     * Checks if data exists in the vault for the given key.
     * 
     * @param key The identifier to check
     * @return True if data exists, false otherwise
     */
    fun exists(key: String): Boolean {
        val file = File(vaultDir, "$key.vault")
        return file.exists()
    }
    
    /**
     * Removes data from the vault.
     * 
     * @param key The identifier of the data to remove
     * @return True if removal was successful, false otherwise
     */
    fun remove(key: String): Boolean {
        return try {
            val file = File(vaultDir, "$key.vault")
            val deleted = file.delete()
            
            if (deleted) {
                Log.d(TAG, "Successfully removed data with key: $key")
            } else {
                Log.w(TAG, "File not found for key: $key")
            }
            
            deleted
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove data with key: $key", e)
            false
        }
    }
    
    /**
     * Lists all keys stored in the vault.
     * 
     * @return List of stored keys
     */
    fun listKeys(): List<String> {
        return try {
            vaultDir.listFiles()
                ?.filter { it.name.endsWith(".vault") }
                ?.map { it.name.removeSuffix(".vault") }
                ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list vault keys", e)
            emptyList()
        }
    }
    
    /**
     * Clears all data from the vault.
     * 
     * @return True if clearing was successful, false otherwise
     */
    fun clearAll(): Boolean {
        return try {
            val files = vaultDir.listFiles() ?: emptyArray()
            var allDeleted = true
            
            for (file in files) {
                if (!file.delete()) {
                    allDeleted = false
                    Log.w(TAG, "Failed to delete file: ${file.name}")
                }
            }
            
            if (allDeleted) {
                Log.d(TAG, "Successfully cleared all vault data")
            } else {
                Log.w(TAG, "Some files could not be deleted during vault clear")
            }
            
            allDeleted
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear vault", e)
            false
        }
    }
    
    /**
     * Initializes the RSA master key in Android Keystore.
     * This key is used to encrypt/decrypt AES keys in envelope encryption.
     */
    private fun initializeMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            if (!keyStore.containsAlias(RSA_KEY_ALIAS)) {
                Log.d(TAG, "Generating RSA master key for vault")
                
                val keyPairGenerator = java.security.KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA,
                    ANDROID_KEYSTORE
                )
                
                val keySpec = KeyGenParameterSpec.Builder(
                    RSA_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    .setUserAuthenticationRequired(false)
                    .build()
                
                keyPairGenerator.initialize(keySpec)
                keyPairGenerator.generateKeyPair()
                
                Log.d(TAG, "Successfully generated RSA master key")
            } else {
                Log.d(TAG, "RSA master key already exists")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize master key", e)
            throw RuntimeException("Failed to initialize secure vault master key", e)
        }
    }
    
    /**
     * Generates a random AES-256 key for data encryption.
     */
    private fun generateAESKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGenerator.init(AES_KEY_SIZE, SECURE_RANDOM)
        return keyGenerator.generateKey()
    }
    
    /**
     * Encrypts data using AES-GCM encryption.
     */
    private fun encryptWithAES(data: ByteArray, key: SecretKey): EncryptedData? {
        return try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data)
            
            EncryptedData(encryptedBytes, iv)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt data with AES", e)
            null
        }
    }
    
    /**
     * Decrypts data using AES-GCM decryption.
     */
    private fun decryptWithAES(encryptedData: EncryptedData, key: SecretKey): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_SIZE, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            cipher.doFinal(encryptedData.encryptedBytes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt data with AES", e)
            null
        }
    }
    
    /**
     * Encrypts an AES key using the RSA master key from Android Keystore.
     */
    private fun encryptAESKeyWithRSA(aesKey: SecretKey): ByteArray? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            val publicKey = keyStore.getCertificate(RSA_KEY_ALIAS).publicKey
            
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            
            cipher.doFinal(aesKey.encoded)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt AES key with RSA", e)
            null
        }
    }
    
    /**
     * Decrypts an AES key using the RSA master key from Android Keystore.
     */
    private fun decryptAESKeyWithRSA(encryptedAESKey: ByteArray): ByteArray? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            
            val privateKey = keyStore.getKey(RSA_KEY_ALIAS, null) as java.security.PrivateKey
            
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            
            cipher.doFinal(encryptedAESKey)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt AES key with RSA", e)
            null
        }
    }
}

/**
 * Data class representing encrypted data with its initialization vector.
 */
private data class EncryptedData(
    val encryptedBytes: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as EncryptedData
        
        if (!encryptedBytes.contentEquals(other.encryptedBytes)) return false
        if (!iv.contentEquals(other.iv)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = encryptedBytes.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

/**
 * Data class representing the secure envelope structure stored in files.
 */
private data class SecureEnvelope(
    val encryptedData: String,
    val iv: String,
    val encryptedAESKey: String,
    val timestamp: Long
)