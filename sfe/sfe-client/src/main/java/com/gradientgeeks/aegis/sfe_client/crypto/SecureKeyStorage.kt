package com.gradientgeeks.aegis.sfe_client.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure storage for external HMAC keys using EncryptedSharedPreferences.
 * 
 * This class handles the storage of external keys (like those received from Aegis API)
 * that cannot be directly imported into Android Keystore. It uses a two-tier approach:
 * 1. A master key stored in Android Keystore for encryption
 * 2. EncryptedSharedPreferences to store the actual external keys
 * 
 * This provides hardware-backed security for the encryption key while allowing
 * storage of arbitrary external key material.
 */
class SecureKeyStorage(private val context: Context) {
    
    companion object {
        private const val TAG = "SecureKeyStorage"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "aegis_master_key"
        private const val PREFS_FILE_NAME = "aegis_secure_keys"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12
        
        // Prefixes for stored data
        private const val KEY_PREFIX = "key_"
        private const val IV_PREFIX = "iv_"
    }
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Stores an external HMAC key securely.
     * 
     * The key is encrypted using AES-256-GCM with a hardware-backed master key
     * and stored in EncryptedSharedPreferences for additional security.
     * 
     * @param alias The alias to store the key under
     * @param keyBytes The raw HMAC key bytes
     * @return True if storage was successful, false otherwise
     */
    fun storeExternalKey(alias: String, keyBytes: ByteArray): Boolean {
        return try {
            Log.d(TAG, "Storing external key with alias: $alias")
            
            // Store the key directly in EncryptedSharedPreferences
            // The encryption is handled automatically by EncryptedSharedPreferences
            val keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
            
            encryptedPrefs.edit()
                .putString(KEY_PREFIX + alias, keyBase64)
                .apply()
            
            Log.d(TAG, "Successfully stored external key with alias: $alias")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store external key with alias: $alias", e)
            false
        }
    }
    
    /**
     * Retrieves an external HMAC key from secure storage.
     * 
     * @param alias The alias of the key to retrieve
     * @return The key as SecretKey, or null if not found or retrieval failed
     */
    fun getExternalKey(alias: String): SecretKey? {
        return try {
            Log.d(TAG, "Retrieving external key with alias: $alias")
            
            val keyBase64 = encryptedPrefs.getString(KEY_PREFIX + alias, null)
            if (keyBase64 == null) {
                Log.w(TAG, "Key not found with alias: $alias")
                return null
            }
            
            val keyBytes = Base64.decode(keyBase64, Base64.NO_WRAP)
            val secretKey = SecretKeySpec(keyBytes, "HmacSHA256")
            
            Log.d(TAG, "Successfully retrieved external key with alias: $alias")
            secretKey
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve external key with alias: $alias", e)
            null
        }
    }
    
    /**
     * Checks if an external key exists in storage.
     * 
     * @param alias The alias to check for
     * @return True if key exists, false otherwise
     */
    fun keyExists(alias: String): Boolean {
        return try {
            encryptedPrefs.contains(KEY_PREFIX + alias)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check if key exists with alias: $alias", e)
            false
        }
    }
    
    /**
     * Deletes an external key from storage.
     * 
     * @param alias The alias of the key to delete
     * @return True if deletion was successful, false otherwise
     */
    fun deleteKey(alias: String): Boolean {
        return try {
            Log.d(TAG, "Deleting external key with alias: $alias")
            
            val success = encryptedPrefs.edit()
                .remove(KEY_PREFIX + alias)
                .commit()
            
            if (success) {
                Log.d(TAG, "Successfully deleted external key with alias: $alias")
            }
            
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete external key with alias: $alias", e)
            false
        }
    }
    
    /**
     * Clears all stored external keys.
     * 
     * @return True if clearing was successful, false otherwise
     */
    fun clearAllKeys(): Boolean {
        return try {
            Log.d(TAG, "Clearing all external keys")
            
            val success = encryptedPrefs.edit().clear().commit()
            
            if (success) {
                Log.d(TAG, "Successfully cleared all external keys")
            }
            
            success
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all external keys", e)
            false
        }
    }
    
    /**
     * Lists all stored key aliases.
     * 
     * @return List of key aliases
     */
    fun listKeyAliases(): List<String> {
        return try {
            encryptedPrefs.all.keys
                .filter { it.startsWith(KEY_PREFIX) }
                .map { it.removePrefix(KEY_PREFIX) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list key aliases", e)
            emptyList()
        }
    }
}