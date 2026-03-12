package org.ttproject.viewmodel

import org.ttproject.data.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ttproject.repository.LocationRepository


sealed class LocationsUiState {
    object Loading : LocationsUiState()
    data class Success(val locations: List<Location>) : LocationsUiState()
    data class Error(val message: String) : LocationsUiState()
}

class LocationViewModel(
    private val repository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationsUiState>(LocationsUiState.Loading)
    val uiState: StateFlow<LocationsUiState> = _uiState.asStateFlow()

    init {
        fetchNearbyLocations()
    }

    fun fetchNearbyLocations() {
        viewModelScope.launch {
            _uiState.value = LocationsUiState.Loading
            try {
                val locations = repository.getNearbyLocations()
                if (locations.isEmpty()) {
                    // You might want to distinguish between "empty" and "error"
                    _uiState.value = LocationsUiState.Success(emptyList())
                } else {
                    _uiState.value = LocationsUiState.Success(locations)
                }
            } catch (e: Exception) {
                _uiState.value = LocationsUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}