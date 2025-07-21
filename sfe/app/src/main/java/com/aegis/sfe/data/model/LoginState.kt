package com.aegis.sfe.data.model

data class LoginState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: UserInfo? = null,
    val error: String? = null
)

data class UserInfo(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String?
)