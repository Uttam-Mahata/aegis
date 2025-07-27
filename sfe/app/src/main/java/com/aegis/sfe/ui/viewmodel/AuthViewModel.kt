package com.aegis.sfe.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.sfe.UCOBankApplication
import com.aegis.sfe.data.api.ApiClientFactory
import com.aegis.sfe.data.model.LoginState
import com.aegis.sfe.data.repository.AuthRepository
import org.json.JSONObject
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
    
    // Device blocking and rebinding states
    private val _deviceBlockedState = MutableStateFlow<DeviceBlockedState?>(null)
    val deviceBlockedState: StateFlow<DeviceBlockedState?> = _deviceBlockedState.asStateFlow()
    
    private val _rebindingState = MutableStateFlow<DeviceRebindingState?>(null)
    val rebindingState: StateFlow<DeviceRebindingState?> = _rebindingState.asStateFlow()
    
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
                            
                            // Check if this is a device blocking or rebinding scenario
                            val errorInfo = parseErrorResponse(result.message, null)
                            
                            when {
                                errorInfo.isDeviceBlocked -> {
                                    Log.w(TAG, "Device is blocked: ${errorInfo.reason}")
                                    _deviceBlockedState.value = DeviceBlockedState(
                                        reason = errorInfo.reason ?: "Device has been blocked for security reasons",
                                        isTemporary = errorInfo.isTemporary,
                                        contactSupport = !errorInfo.isTemporary
                                    )
                                }
                                errorInfo.requiresRebinding -> {
                                    Log.w(TAG, "Device rebinding required: ${errorInfo.reason}")
                                    _rebindingState.value = DeviceRebindingState(
                                        reason = errorInfo.reason ?: "Login from new device detected",
                                        username = username
                                    )
                                }
                                else -> {
                                    _loginState.value = _loginState.value.copy(
                                        isLoading = false,
                                        error = result.message
                                    )
                                }
                            }
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
    
    /**
     * Handles device rebinding process.
     */
    fun initiateDeviceRebinding(username: String, verificationMethod: String = "MANUAL_VERIFICATION") {
        viewModelScope.launch {
            try {
                _rebindingState.value = _rebindingState.value?.copy(isProcessing = true)
                
                val deviceId = UCOBankApplication.aegisClient.getDeviceId()
                if (deviceId == null) {
                    _rebindingState.value = _rebindingState.value?.copy(
                        isProcessing = false,
                        error = "Device not properly provisioned"
                    )
                    return@launch
                }
                
                Log.d(TAG, "Initiating device rebinding - User: $username, Device: $deviceId")
                
                // Call rebinding API
                val success = authRepository.rebindDevice(username, deviceId, verificationMethod)
                
                if (success) {
                    Log.i(TAG, "Device rebinding successful")
                    _rebindingState.value = null // Clear rebinding state
                    
                    // Show success message
                    _loginState.value = _loginState.value.copy(
                        error = null,
                        isLoading = false
                    )
                } else {
                    _rebindingState.value = _rebindingState.value?.copy(
                        isProcessing = false,
                        error = "Device rebinding failed. Please try again or contact support."
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during device rebinding", e)
                _rebindingState.value = _rebindingState.value?.copy(
                    isProcessing = false,
                    error = "An error occurred during device verification: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clears device blocked state.
     */
    fun clearDeviceBlockedState() {
        _deviceBlockedState.value = null
    }
    
    /**
     * Clears device rebinding state.
     */
    fun clearRebindingState() {
        _rebindingState.value = null
    }
    
    /**
     * Parses error response to determine if it's related to device blocking or rebinding.
     */
    private fun parseErrorResponse(message: String?, exception: Throwable?): ErrorInfo {
        var reason: String? = null
        var isDeviceBlocked = false
        var requiresRebinding = false
        var isTemporary = false
        
        try {
            // Try to parse JSON error response
            val errorMessage = message ?: ""
            
            // Check for device verification patterns
            when {
                errorMessage.contains("device verification", ignoreCase = true) ||
                errorMessage.contains("new device", ignoreCase = true) ||
                errorMessage.contains("DEVICE_VERIFICATION_REQUIRED", ignoreCase = true) -> {
                    requiresRebinding = true
                    reason = "Login from new device detected. Identity verification required."
                }
                
                errorMessage.contains("device has been blocked", ignoreCase = true) ||
                errorMessage.contains("device blocked", ignoreCase = true) -> {
                    isDeviceBlocked = true
                    isTemporary = errorMessage.contains("temporarily", ignoreCase = true)
                    reason = if (isTemporary) {
                        "Your device has been temporarily blocked for security reasons."
                    } else {
                        "Your device has been permanently blocked for security reasons."
                    }
                }
                
                errorMessage.contains("HMAC", ignoreCase = true) ||
                errorMessage.contains("signature", ignoreCase = true) -> {
                    isDeviceBlocked = true
                    isTemporary = true
                    reason = "Device security validation failed. Please try again or contact support."
                }
            }
            
            // Try to parse as JSON if it looks like JSON
            if (errorMessage.startsWith("{") && errorMessage.endsWith("}")) {
                try {
                    val jsonError = JSONObject(errorMessage)
                    if (jsonError.has("requiresRebinding") && jsonError.getBoolean("requiresRebinding")) {
                        requiresRebinding = true
                        reason = jsonError.optString("message", "Device verification required")
                    }
                    if (jsonError.has("errorCode")) {
                        when (jsonError.getString("errorCode")) {
                            "DEVICE_VERIFICATION_REQUIRED" -> {
                                requiresRebinding = true
                                reason = "Identity verification required for new device"
                            }
                            "DEVICE_BLOCKED" -> {
                                isDeviceBlocked = true
                                reason = jsonError.optString("message", "Device has been blocked")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Could not parse error as JSON: $errorMessage")
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing error response", e)
        }
        
        return ErrorInfo(reason, isDeviceBlocked, requiresRebinding, isTemporary)
    }
}

/**
 * Represents the state when a device is blocked.
 */
data class DeviceBlockedState(
    val reason: String,
    val isTemporary: Boolean,
    val contactSupport: Boolean
)

/**
 * Represents the state when device rebinding is required.
 */
data class DeviceRebindingState(
    val reason: String,
    val username: String,
    val isProcessing: Boolean = false,
    val error: String? = null
)

/**
 * Internal data class for parsing error information.
 */
private data class ErrorInfo(
    val reason: String?,
    val isDeviceBlocked: Boolean,
    val requiresRebinding: Boolean,
    val isTemporary: Boolean
)