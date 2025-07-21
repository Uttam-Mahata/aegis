package com.gradientgeeks.aegis.sfe_client.api

import com.gradientgeeks.aegis.sfe_client.model.DeviceRegistrationRequest
import com.gradientgeeks.aegis.sfe_client.model.DeviceRegistrationResponse
import com.gradientgeeks.aegis.sfe_client.model.SignatureValidationRequest
import com.gradientgeeks.aegis.sfe_client.model.SignatureValidationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit service interface for communicating with the Aegis Security API.
 * 
 * Defines the HTTP endpoints for device registration and signature validation
 * operations with the backend security service.
 */
interface AegisApiService {
    
    /**
     * Registers a new device with the Aegis Security API.
     * 
     * This endpoint is called during the initial device provisioning process
     * to establish a secure identity for the device.
     * 
     * @param request The device registration request containing client ID,
     *                registration key, and integrity token
     * @return Response containing device credentials (device ID and secret key)
     */
    @POST("v1/register")
    suspend fun registerDevice(
        @Body request: DeviceRegistrationRequest
    ): Response<DeviceRegistrationResponse>
    
    /**
     * Validates an HMAC signature for a request.
     * 
     * This endpoint is used by backend services to verify that requests
     * are authentic and have not been tampered with.
     * 
     * @param request The signature validation request containing device ID,
     *                signature, and string to sign
     * @return Response indicating whether the signature is valid
     */
    @POST("v1/validate")
    suspend fun validateSignature(
        @Body request: SignatureValidationRequest
    ): Response<SignatureValidationResponse>
    
    /**
     * Health check endpoint to verify API connectivity.
     * 
     * @return Response with health status
     */
    @GET("v1/health")
    suspend fun healthCheck(): Response<Map<String, String>>
}