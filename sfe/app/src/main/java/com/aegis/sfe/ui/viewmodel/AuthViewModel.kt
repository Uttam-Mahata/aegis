package com.aegis.sfe.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.sfe.UCOBankApplication
import com.aegis.sfe.data.api.ApiClientFactory
import com.aegis.sfe.data.model.LoginState
import com.aegis.sfe.data.repository.AuthRepository
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    private val authRepository = AuthRepository()
    
    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    fun login(username: String, password: String) {
        viewModelScope.launch {
            try {
                _loginState.value = _loginState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                // Log device provisioning status for debugging
                val deviceId = try {
                    UCOBankApplication.aegisClient.getDeviceId()
                } catch (e: Exception) {
                    null
                }
                
                Log.d(TAG, "Login attempt - deviceId: $deviceId")
                
                Log.d(TAG, "Attempting login for user: $username")
                
                // Login through repository (which handles HMAC signing)
                authRepository.login(username, password).collect { result ->
                    when (result) {
                        is com.aegis.sfe.data.model.ApiResult.Loading -> {
                            // Already set loading state
                        }
                        is com.aegis.sfe.data.model.ApiResult.Success -> {
                            Log.d(TAG, "Login successful for user: $username")
                            
                            // Store auth token
                            UCOBankApplication.authToken = result.data.accessToken
                            UCOBankApplication.currentUser = result.data.user
                            
                            _loginState.value = _loginState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                error = null,
                                user = result.data.user
                            )
                        }
                        is com.aegis.sfe.data.model.ApiResult.Error -> {
                            Log.e(TAG, "Login failed: ${result.message}")
                            _loginState.value = _loginState.value.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Login exception", e)
                _loginState.value = _loginState.value.copy(
                    isLoading = false,
                    error = "Login failed: ${e.message}"
                )
            }
        }
    }
    
    fun logout() {
        UCOBankApplication.authToken = null
        UCOBankApplication.currentUser = null
        _loginState.value = LoginState()
    }
    
    fun clearError() {
        _loginState.value = _loginState.value.copy(error = null)
    }
}