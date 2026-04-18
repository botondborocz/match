package org.ttproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ttproject.repository.UserRepository

sealed class ProfileState {
    object Loading : ProfileState()
    // 👇 Added imageUrl here
    data class Success(
        val name: String?,
        val elo: Int,
        val winRate: String,
        val language: String?,
        val imageUrl: String? = null,
        val blade: String? = null,
        val rubberFh: String? = null,
        val rubberBh: String? = null,
        val bio: String? = null,
        val birthDate: String? = null,
        val skillLevel: String? = null,
        val age: Int? = null
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class UpdateState {
    object Idle : UpdateState()
    object Loading : UpdateState()
    object Success : UpdateState()
    data class Error(val message: String) : UpdateState()
}

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val uiState: StateFlow<ProfileState> = _uiState

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileState.Loading
            try {
                val user = userRepository.getMyProfile()

                _uiState.value = ProfileState.Success(
                    name = user.name, elo = user.elo, winRate = user.winRate,
                    language = user.preferredLanguage, imageUrl = user.imageUrl,
                    blade = user.blade, rubberFh = user.rubberFh, rubberBh = user.rubberBh,
                    // 👇 Map the new fields
                    bio = user.bio, birthDate = user.birthDate,
                    skillLevel = user.skillLevel, age = user.age
                )
            } catch (e: Exception) {
                _uiState.value = ProfileState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    fun updateProfile(
        name: String, blade: String, forehand: String, backhand: String,
        bio: String?, birthDate: String?, skillLevel: String? // 👈 Added
    ) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading
            // 👇 Pass them to the repo
            val result = userRepository.updateProfile(name, blade, forehand, backhand, bio, birthDate, skillLevel)

            if (result.isSuccess) {
                _updateState.value = UpdateState.Success
                fetchUserProfile() // Refresh profile data after successful update
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _updateState.value = UpdateState.Error(errorMsg)
            }
        }
    }

    // 👇 New function to handle the image upload
    fun uploadProfileImage(imageBytes: ByteArray) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            val result = userRepository.uploadProfileImage(imageBytes)

            if (result.isSuccess) {
                _updateState.value = UpdateState.Success
                // Re-fetch the profile so the UI gets the new Firebase Download URL from the server
                fetchUserProfile()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown upload error"
                _updateState.value = UpdateState.Error(errorMsg)
            }
        }
    }

    fun clearProfile() {
        _uiState.value = ProfileState.Loading
    }

    fun changeLanguage(newLanguage: String) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            val result = userRepository.updateLanguage(newLanguage)

            if (result.isSuccess) {
                _updateState.value = UpdateState.Success
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                _updateState.value = UpdateState.Error(errorMsg)
            }
        }
    }

    fun resetState() {
        _updateState.value = UpdateState.Idle
    }
}