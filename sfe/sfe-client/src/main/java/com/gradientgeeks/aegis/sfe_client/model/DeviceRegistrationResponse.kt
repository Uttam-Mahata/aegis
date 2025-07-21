package com.gradientgeeks.aegis.sfe_client.model

import com.google.gson.annotations.SerializedName

/**
 * Response model for device registration from the Aegis Security API.
 * 
 * Contains the device credentials that are securely provisioned during
 * the registration process.
 */
data class DeviceRegistrationResponse(
    
    /**
     * Unique device identifier assigned by the Aegis Security API.
     * This ID is used to identify the device in subsequent API calls.
     */
    @SerializedName("deviceId")
    val deviceId: String,
    
    /**
     * Cryptographically secure secret key for HMAC signing.
     * This key should be immediately stored in Android Keystore for security.
     */
    @SerializedName("secretKey")
    val secretKey: String,
    
    /**
     * Registration status indicator.
     * Typically "success" for successful registrations.
     */
    @SerializedName("status")
    val status: String,
    
    /**
     * Additional message from the server (optional).
     * May contain information about the registration process.
     */
    @SerializedName("message")
    val message: String? = null
)