package com.aegis.sfe.data.repository

import android.util.Log
import com.aegis.sfe.data.api.ApiClientFactory
import com.aegis.sfe.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response

class BankRepository {
    
    companion object {
        private const val TAG = "BankRepository"
    }
    
    private val apiService = ApiClientFactory.bankApiService
    
    fun getAccount(accountNumber: String): Flow<ApiResult<Account>> = flow {
        emit(ApiResult.Loading("Fetching account details..."))
        try {
            val response = apiService.getAccount(accountNumber)
            if (response.isSuccessful) {
                response.body()?.let { accountResponse ->
                    emit(ApiResult.Success(accountResponse.toAccount()))
                } ?: emit(ApiResult.Error("Account data is empty"))
            } else {
                emit(ApiResult.Error(parseErrorMessage(response), response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching account", e)
            emit(ApiResult.Error("Network error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun getUserAccounts(userId: String): Flow<ApiResult<List<Account>>> = flow {
        emit(ApiResult.Loading("Fetching user accounts..."))
        try {
            val response = apiService.getUserAccounts(userId)
            if (response.isSuccessful) {
                response.body()?.let { accounts ->
                    emit(ApiResult.Success(accounts.map { it.toAccount() }))
                } ?: emit(ApiResult.Error("Accounts data is empty"))
            } else {
                emit(ApiResult.Error(parseErrorMessage(response), response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user accounts", e)
            emit(ApiResult.Error("Network error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun validateAccount(accountNumber: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading("Validating account..."))
        try {
            val response = apiService.validateAccount(accountNumber)
            if (response.isSuccessful) {
                response.body()?.let { isValid ->
                    emit(ApiResult.Success(isValid))
                } ?: emit(ApiResult.Error("Validation response is empty"))
            } else {
                emit(ApiResult.Error(parseErrorMessage(response), response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error validating account", e)
            emit(ApiResult.Error("Network error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun transferMoney(transferRequest: TransferRequest): Flow<ApiResult<TransferResponse>> = flow {
        emit(ApiResult.Loading("Processing transfer..."))
        try {
            val response = apiService.transferMoney(transferRequest)
            if (response.isSuccessful) {
                response.body()?.let { transferResponse ->
                    emit(ApiResult.Success(transferResponse))
                } ?: emit(ApiResult.Error("Transfer response is empty"))
            } else {
                emit(ApiResult.Error(parseErrorMessage(response), response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing transfer", e)
            emit(ApiResult.Error("Network error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun getTransactionHistory(
        accountNumber: String,
        page: Int = 0,
        size: Int = 20
    ): Flow<ApiResult<TransactionHistoryResponse>> = flow {
        emit(ApiResult.Loading("Fetching transaction history..."))
        try {
            val response = apiService.getTransactionHistory(accountNumber, page, size)
            if (response.isSuccessful) {
                response.body()?.let { history ->
                    emit(ApiResult.Success(history))
                } ?: emit(ApiResult.Error("Transaction history is empty"))
            } else {
                emit(ApiResult.Error(parseErrorMessage(response), response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transaction history", e)
            emit(ApiResult.Error("Network error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun getTransactionByReference(transactionReference: String): Flow<ApiResult<Transaction>> = flow {
        emit(ApiResult.Loading("Fetching transaction details..."))
        try {
            val response = apiService.getTransactionByReference(transactionReference)
            if (response.isSuccessful) {
                response.body()?.let { transactionResponse ->
                    emit(ApiResult.Success(transactionResponse.toTransaction()))
                } ?: emit(ApiResult.Error("Transaction data is empty"))
            } else {
                emit(ApiResult.Error(parseErrorMessage(response), response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transaction", e)
            emit(ApiResult.Error("Network error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    fun checkHealth(): Flow<ApiResult<Map<String, Any>>> = flow {
        emit(ApiResult.Loading("Checking service health..."))
        try {
            val response = apiService.getHealthStatus()
            if (response.isSuccessful) {
                response.body()?.let { health ->
                    emit(ApiResult.Success(health))
                } ?: emit(ApiResult.Error("Health response is empty"))
            } else {
                emit(ApiResult.Error(parseErrorMessage(response), response.code()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking health", e)
            emit(ApiResult.Error("Network error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    private fun parseErrorMessage(response: Response<*>): String {
        return when (response.code()) {
            401 -> "Unauthorized - Invalid request signature"
            403 -> "Forbidden - Access denied"
            404 -> "Resource not found"
            400 -> "Bad request - Invalid data"
            500 -> "Server error - Please try again later"
            else -> "Error ${response.code()}: ${response.message()}"
        }
    }
}