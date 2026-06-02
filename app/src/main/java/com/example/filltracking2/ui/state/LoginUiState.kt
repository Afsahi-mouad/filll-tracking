package com.example.filltracking2.ui.state

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null
)
