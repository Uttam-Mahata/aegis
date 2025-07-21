package com.aegis.sfe.data.api

import android.util.Log
import com.aegis.sfe.UCOBankApplication
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.StandardCharsets

class SignedRequestInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "SignedRequestInterceptor"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        
        // Skip signing for health endpoints
        if (original.url.encodedPath.contains("/health")) {
            return chain.proceed(original)
        }
        
        try {
            // Check if device is provisioned
            if (!UCOBankApplication.aegisClient.isDeviceProvisioned()) {
                Log.w(TAG, "Device not provisioned, cannot sign request")
                return chain.proceed(original)
            }
            
            // Get request body as string for signing
            val requestBody = original.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                buffer.readUtf8()
            }
            
            // Sign the request using Aegis SDK
            // Note: signRequest is a synchronous method, not a suspend function
            // Use only the path without query parameters for signing (to match backend validation)
            val signedHeaders = UCOBankApplication.aegisClient.signRequest(
                method = original.method,
                uri = original.url.encodedPath,
                body = requestBody
            )
            
            if (signedHeaders == null) {
                Log.e(TAG, "Failed to sign request")
                return chain.proceed(original)
            }
            
            // Add signed headers to request
            val requestBuilder = original.newBuilder()
                .header("X-Device-Id", signedHeaders.deviceId)
                .header("X-Signature", signedHeaders.signature)
                .header("X-Timestamp", signedHeaders.timestamp)
                .header("X-Nonce", signedHeaders.nonce)
            
            // Add auth token if available
            UCOBankApplication.authToken?.let { token ->
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            val signedRequest = requestBuilder.build()
            
            Log.d(TAG, "Request signed successfully for ${original.url.encodedPath}")
            return chain.proceed(signedRequest)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error signing request", e)
            // Proceed with unsigned request in case of error
            return chain.proceed(original)
        }
    }
}