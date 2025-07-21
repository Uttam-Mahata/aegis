package com.aegis.sfe.data.api

import android.util.Log
import com.aegis.sfe.UCOBankApplication
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that handles authentication errors and triggers reprovisioning if needed.
 * 
 * This interceptor detects 401/403 errors that indicate signature validation failures
 * and automatically handles reprovisioning with fresh credentials.
 */
class AuthErrorInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "AuthErrorInterceptor"
        @Volatile
        private var isReprovisioning = false
        private val reprovisioningLock = Any()
        private var lastReprovisioningTime = 0L
        private const val REPROVISIONING_COOLDOWN_MS = 10000L // 10 seconds cooldown
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Check for authentication errors
        if (response.code == 401 || response.code == 403) {
            Log.w(TAG, "Authentication error detected: ${response.code}")
            
            // Check if this is a signature validation error
            val errorBody = response.peekBody(2048).string()
            if (errorBody.contains("Invalid request signature", ignoreCase = true) ||
                errorBody.contains("signature", ignoreCase = true)) {
                
                Log.w(TAG, "Signature validation error detected")
                
                // Avoid recursive reprovisioning with thread safety
                synchronized(reprovisioningLock) {
                    val currentTime = System.currentTimeMillis()
                    if (!isReprovisioning && (currentTime - lastReprovisioningTime) > REPROVISIONING_COOLDOWN_MS) {
                        isReprovisioning = true
                        lastReprovisioningTime = currentTime
                    } else {
                        Log.d(TAG, "Skipping reprovisioning - already in progress or in cooldown")
                        return response
                    }
                }
                
                try {
                    // Check if we should reprovision
                    val provisioningService = UCOBankApplication.aegisClient.getProvisioningService()
                    
                    if (provisioningService.shouldReprovisionOnAuthFailure()) {
                        Log.i(TAG, "Triggering device reprovisioning due to auth failure")
                            
                            // Force reprovision with new credentials
                            var retryResponse: Response? = null
                            runBlocking {
                                val result = provisioningService.provisionDevice(
                                    UCOBankApplication.CLIENT_ID,
                                    UCOBankApplication.REGISTRATION_KEY,
                                    forceReprovisioning = true
                                )
                                
                                when (result) {
                                    is com.gradientgeeks.aegis.sfe_client.provisioning.ProvisioningResult.Success -> {
                                        Log.i(TAG, "Reprovisioning successful, retrying request")
                                        
                                        // Retry the original request with new credentials
                                        val newRequest = UCOBankApplication.aegisClient.signRequest(
                                            request.method,
                                            request.url.encodedPath,
                                            request.body?.let { body ->
                                                val buffer = okio.Buffer()
                                                body.writeTo(buffer)
                                                buffer.readUtf8()
                                            }
                                        )?.let { headers ->
                                            request.newBuilder()
                                                .header("X-Device-Id", headers.deviceId)
                                                .header("X-Signature", headers.signature)
                                                .header("X-Timestamp", headers.timestamp)
                                                .header("X-Nonce", headers.nonce)
                                                .build()
                                        } ?: request
                                        
                                        retryResponse = chain.proceed(newRequest)
                                    }
                                    else -> {
                                        Log.e(TAG, "Reprovisioning failed: $result")
                                    }
                                }
                            }
                            
                            if (retryResponse != null) {
                                // Close the original response and return the retry response
                                response.close()
                                return retryResponse!!
                            }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during reprovisioning", e)
                } finally {
                    isReprovisioning = false
                }
            }
        }
        
        return response
    }
}