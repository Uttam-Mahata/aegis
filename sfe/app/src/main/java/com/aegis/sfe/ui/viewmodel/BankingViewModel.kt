package com.aegis.sfe.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.sfe.data.model.*
import com.aegis.sfe.data.repository.BankRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

class BankingViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "BankingViewModel"
        // Default user ID for demo purposes
        const val DEFAULT_USER_ID = "USER001"
    }
    
    private val repository = BankRepository()
    
    // UI State
    private val _uiState = MutableStateFlow(BankingUiState())
    val uiState: StateFlow<BankingUiState> = _uiState.asStateFlow()
    
    // User accounts
    private val _userAccounts = MutableStateFlow<List<Account>>(emptyList())
    val userAccounts: StateFlow<List<Account>> = _userAccounts.asStateFlow()
    
    // Selected account
    private val _selectedAccount = MutableStateFlow<Account?>(null)
    val selectedAccount: StateFlow<Account?> = _selectedAccount.asStateFlow()
    
    // Transaction history
    private val _transactionHistory = MutableStateFlow<TransactionHistoryResponse?>(null)
    val transactionHistory: StateFlow<TransactionHistoryResponse?> = _transactionHistory.asStateFlow()
    
    // Transfer state
    private val _transferState = MutableStateFlow(TransferState())
    val transferState: StateFlow<TransferState> = _transferState.asStateFlow()
    
    init {
        // Load accounts if user is already logged in
        viewModelScope.launch {
            if (com.aegis.sfe.UCOBankApplication.currentUser != null) {
                android.util.Log.d(TAG, "User is logged in, waiting before loading accounts...")
                // Small delay to ensure navigation and auth token are properly set
                kotlinx.coroutines.delay(500)
                loadUserAccounts()
            } else {
                android.util.Log.d(TAG, "No user logged in, skipping account load")
            }
        }
    }
    
    fun loadUserAccounts() {
        viewModelScope.launch {
            try {
                // Use the actual logged-in user ID instead of default
                val userId = com.aegis.sfe.UCOBankApplication.currentUser?.id?.toString() ?: DEFAULT_USER_ID
                Log.d(TAG, "Loading accounts for userId: $userId")
                repository.getUserAccounts(userId).collect { result ->
                    when (result) {
                        is ApiResult.Loading -> {
                            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                        }
                        is ApiResult.Success -> {
                            _userAccounts.value = result.data
                            _selectedAccount.value = result.data.firstOrNull()
                            _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                            Log.d(TAG, "Loaded ${result.data.size} user accounts")
                        }
                        is ApiResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                            Log.e(TAG, "Error loading user accounts: ${result.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in loadUserAccounts", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load accounts: ${e.message}"
                )
            }
        }
    }
    
    fun selectAccount(account: Account) {
        _selectedAccount.value = account
        loadTransactionHistory(account.accountNumber)
    }
    
    fun loadTransactionHistory(accountNumber: String, page: Int = 0) {
        viewModelScope.launch {
            repository.getTransactionHistory(accountNumber, page).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is ApiResult.Success -> {
                        _transactionHistory.value = result.data
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        Log.d(TAG, "Loaded transaction history for account $accountNumber")
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    fun validateAccount(accountNumber: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            repository.validateAccount(accountNumber).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        callback(result.data)
                    }
                    is ApiResult.Error -> {
                        callback(false)
                    }
                    is ApiResult.Loading -> {
                        // Handle loading state if needed
                    }
                }
            }
        }
    }
    
    fun transferMoney(transferRequest: TransferRequest) {
        viewModelScope.launch {
            repository.transferMoney(transferRequest).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _transferState.value = _transferState.value.copy(
                            isLoading = true,
                            error = null,
                            success = null
                        )
                    }
                    is ApiResult.Success -> {
                        _transferState.value = _transferState.value.copy(
                            isLoading = false,
                            success = result.data,
                            error = null
                        )
                        
                        // Refresh account balance after successful transfer
                        loadUserAccounts()
                        
                        Log.d(TAG, "Transfer completed: ${result.data.transactionReference}")
                    }
                    is ApiResult.Error -> {
                        _transferState.value = _transferState.value.copy(
                            isLoading = false,
                            error = result.message,
                            success = null
                        )
                        Log.e(TAG, "Transfer failed: ${result.message}")
                    }
                }
            }
        }
    }
    
    fun clearTransferState() {
        _transferState.value = TransferState()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun refreshData() {
        loadUserAccounts()
        _selectedAccount.value?.let { account ->
            loadTransactionHistory(account.accountNumber)
        }
    }
}

data class BankingUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

data class TransferState(
    val isLoading: Boolean = false,
    val success: TransferResponse? = null,
    val error: String? = null
)