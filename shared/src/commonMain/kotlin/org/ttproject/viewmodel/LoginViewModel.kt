package org.ttproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.ttproject.repository.AuthRepository

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)
    val uiState: StateFlow<LoginState> = _uiState

    // --- STANDARD LOGIN ---
    fun login(email: String, password: String) {
        _uiState.value = LoginState.Loading

        viewModelScope.launch {
            val result = authRepository.login(email, password)

            result.fold(
                onSuccess = {
                    _uiState.value = LoginState.Success
                    // val userLang = apiResponse.user.preferredLanguage ?: "en"
                    // tokenStorage.saveLanguage(userLang)
                },
                onFailure = { error ->
                    _uiState.value = LoginState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }

    // --- REGISTRATION ---
    fun register(email: String, username: String, password: String) {
        _uiState.value = LoginState.Loading

        viewModelScope.launch {
            val result = authRepository.register(email, username, password)

            result.fold(
                onSuccess = {
                    _uiState.value = LoginState.Success
                },
                onFailure = { error ->
                    _uiState.value = LoginState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }

    // --- GOOGLE LOGIN ---
    fun googleLogin(idToken: String) {
        _uiState.value = LoginState.Loading

        viewModelScope.launch {
            val result = authRepository.googleLogin(idToken)

            result.fold(
                onSuccess = {
                    _uiState.value = LoginState.Success
                },
                onFailure = { error ->
                    _uiState.value = LoginState.Error(error.message ?: "Google login failed")
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = LoginState.Idle
    }
}

// A simple sealed class to represent what the UI should draw
sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
}