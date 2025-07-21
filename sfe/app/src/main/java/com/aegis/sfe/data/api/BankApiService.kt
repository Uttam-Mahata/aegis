package com.aegis.sfe.data.api

import com.aegis.sfe.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface BankApiService {
    
    @GET("accounts/{accountNumber}")
    suspend fun getAccount(
        @Path("accountNumber") accountNumber: String
    ): Response<AccountResponse>
    
    @GET("accounts/user/{userId}")
    suspend fun getUserAccounts(
        @Path("userId") userId: String
    ): Response<List<AccountResponse>>
    
    @GET("accounts/{accountNumber}/validate")
    suspend fun validateAccount(
        @Path("accountNumber") accountNumber: String
    ): Response<Boolean>
    
    @POST("transactions/transfer")
    suspend fun transferMoney(
        @Body request: TransferRequest
    ): Response<TransferResponse>
    
    @GET("transactions/account/{accountNumber}")
    suspend fun getTransactionHistory(
        @Path("accountNumber") accountNumber: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sort") sort: String = "createdAt,desc"
    ): Response<TransactionHistoryResponse>
    
    @GET("transactions/reference/{transactionReference}")
    suspend fun getTransactionByReference(
        @Path("transactionReference") transactionReference: String
    ): Response<TransactionResponse>
    
    @GET("health")
    suspend fun getHealthStatus(): Response<Map<String, Any>>
}