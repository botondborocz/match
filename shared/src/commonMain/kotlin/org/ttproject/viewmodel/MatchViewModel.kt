package org.ttproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ttproject.data.Player
import org.ttproject.repository.MatchRepository

// Represents the different states your UI can be in
sealed class MatchUiState {
    data object Loading : MatchUiState()
    data class Success(val players: List<Player>) : MatchUiState()
    data class Error(val message: String) : MatchUiState()
}

class MatchViewModel(
    // In the future, use Dependency Injection (like Koin) here!
    private val repository: MatchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchUiState>(MatchUiState.Loading)
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private val _matchedPlayer = MutableStateFlow<Player?>(null)
    val matchedPlayer: StateFlow<Player?> = _matchedPlayer.asStateFlow()

    init {
        loadPlayers()
    }

    fun loadPlayers() {
        viewModelScope.launch {
            _uiState.value = MatchUiState.Loading
            try {
                val players = repository.getNearbyPlayers()
                _uiState.value = MatchUiState.Success(players)
            } catch (e: Exception) {
                _uiState.value = MatchUiState.Error("Failed to load matches.")
            }
        }
    }

    fun onPlayerSwiped(player: Player, isLiked: Boolean) {
        // 1. Instantly update the UI stack so the card flies away smoothly
        _uiState.update { currentState ->
            if (currentState is MatchUiState.Success) {
                MatchUiState.Success(currentState.players.filterNot { it.id == player.id })
            } else currentState
        }

        // 2. Talk to the server in the background
        viewModelScope.launch {
            val isMatch = repository.recordSwipeAction(player.id, isLiked)
            if (isMatch) {
                // BOOM! Trigger the celebratory popup
                _matchedPlayer.value = player
            }
        }
    }

    // Called when the user clicks "Keep Swiping" or "Send Message"
    fun dismissMatchPopup() {
        _matchedPlayer.value = null
    }
}