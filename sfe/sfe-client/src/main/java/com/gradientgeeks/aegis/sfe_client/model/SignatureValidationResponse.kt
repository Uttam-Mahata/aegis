package com.gradientgeeks.aegis.sfe_client.model

import com.google.gson.annotations.SerializedName

/**
 * Response model for signature validation from the Aegis Security API.
 * 
 * Contains the result of signature verification performed by the backend.
 */
data class SignatureValidationResponse(
    
    /**
     * Boolean indicating whether the signature is valid.
     * True if the signature was successfully verified, false otherwise.
     */
    @SerializedName("isValid")
    val isValid: Boolean,
    
    /**
     * Human-readable message describing the validation result.
     * May contain additional details about validation failure reasons.
     */
    @SerializedName("message")
    val message: String,
    
    /**
     * Echo of the device ID that was validated.
     * Useful for request correlation and debugging.
     */
    @SerializedName("deviceId")
    val deviceId: String? = null
)