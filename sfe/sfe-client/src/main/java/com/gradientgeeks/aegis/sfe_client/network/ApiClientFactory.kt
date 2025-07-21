package com.gradientgeeks.aegis.sfe_client.network

import com.gradientgeeks.aegis.sfe_client.api.AegisApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory for creating API clients with proper configuration.
 * 
 * Handles the creation of Retrofit instances and OkHttp clients
 * with appropriate timeouts, logging, and interceptors.
 */
object ApiClientFactory {
    
    private const val DEFAULT_TIMEOUT_SECONDS = 30L
    private const val DEFAULT_READ_TIMEOUT_SECONDS = 60L
    private const val DEFAULT_WRITE_TIMEOUT_SECONDS = 60L
    
    /**
     * Creates an AegisApiService instance for the given base URL.
     * 
     * @param baseUrl The base URL of the Aegis Security API
     * @param enableLogging Whether to enable HTTP request/response logging
     * @return Configured AegisApiService instance
     */
    fun createAegisApiService(
        baseUrl: String,
        enableLogging: Boolean = false
    ): AegisApiService {
        
        val okHttpClient = createOkHttpClient(enableLogging)
        
        val retrofit = Retrofit.Builder()
            .baseUrl(ensureTrailingSlash(baseUrl))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(AegisApiService::class.java)
    }
    
    /**
     * Creates a configured OkHttp client.
     * 
     * @param enableLogging Whether to enable request/response logging
     * @return Configured OkHttpClient
     */
    private fun createOkHttpClient(enableLogging: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        
        // Add logging interceptor if enabled
        if (enableLogging) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    /**
     * Ensures the base URL has a trailing slash.
     * 
     * @param url The URL to check
     * @return URL with trailing slash
     */
    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}