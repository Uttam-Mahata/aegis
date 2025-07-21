package com.aegis.sfe.data.api

import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    
    @POST("auth/login")
    suspend fun login(
        @Body loginRequest: Map<String, String>
    ): Response<Map<String, Any>>
    
    @POST("auth/logout")
    suspend fun logout(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>
    
    @GET("auth/health")
    suspend fun checkHealth(): Response<Map<String, Any>>
}