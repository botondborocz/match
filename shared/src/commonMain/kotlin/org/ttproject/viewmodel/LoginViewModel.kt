package org.ttproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.ttproject.repository.AuthRepository

// Notice we inject the Repository, NOT the Ktor client!
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<LoginState>(LoginState.Idle)
    val uiState: StateFlow<LoginState> = _uiState

    fun login(email: String, password: String) {
        _uiState.value = LoginState.Loading

        // viewModelScope automatically cancels if the user leaves the screen
        viewModelScope.launch {
            val result = authRepository.login(email, password)

            result.fold(
                onSuccess = { _uiState.value = LoginState.Success },
                onFailure = { error -> _uiState.value = LoginState.Error(error.message ?: "Unknown error") }
            )
        }
    }
}

// A simple sealed class to represent what the UI should draw
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}