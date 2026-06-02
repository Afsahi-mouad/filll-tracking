package com.example.filltracking2.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filltracking2.ui.state.LoginUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.content.Context
import com.example.filltracking2.util.PreferenceManager
import kotlinx.coroutines.flow.first

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username, error = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, error = null) }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeChange(rememberMe: Boolean) {
        _uiState.update { it.copy(rememberMe = rememberMe) }
    }

    fun login(context: Context, onSuccess: (String) -> Unit) {
        val currentState = _uiState.value
        
        // Simple Validation
        if (currentState.username.isBlank()) {
            _uiState.update { it.copy(error = "Username cannot be empty") }
            return
        }
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(error = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Get stored password
            val storedPassword = PreferenceManager.getPassword(context).first()
            
            // Fake Network/Auth Delay
            delay(1500)

            // Authentication Logic
            if (currentState.username == "admin" && currentState.password == storedPassword) {
                if (currentState.rememberMe) {
                    PreferenceManager.setLoggedIn(context, true, currentState.username)
                }
                _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                onSuccess(currentState.username)
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        error = "Invalid username or password"
                    ) 
                }
            }
        }
    }

    fun resetPassword(context: Context, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (pin == "0000") {
            viewModelScope.launch {
                PreferenceManager.setPassword(context, "admin")
                onSuccess()
            }
        } else {
            onError("Invalid PIN code")
        }
    }
}
