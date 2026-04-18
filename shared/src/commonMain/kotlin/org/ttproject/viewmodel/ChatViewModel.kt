package org.ttproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ttproject.data.Location
import org.ttproject.data.MessageDto
import org.ttproject.repository.ChatEvent
import org.ttproject.repository.ChatRepository
import org.ttproject.util.NotificationEventBus
import kotlinx.coroutines.flow.update
import org.ttproject.data.ReactionDto
import org.ttproject.data.UserProfile
import org.ttproject.repository.UserRepository

class ChatViewModel(
    private val repository: ChatRepository,
    private val userRepository: UserRepository,
    private val connectionId: String,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _otherUserProfile = MutableStateFlow<UserProfile?>(null)
    val otherUserProfile: StateFlow<UserProfile?> = _otherUserProfile.asStateFlow()

    init {
        loadChat()
    }

    private fun loadChat() {
        viewModelScope.launch {
            // 1. Fetch the history first
            val history = repository.getMessageHistory(connectionId)
            _messages.value = history
            _isLoading.value = false

            // 2. Once history is loaded, open the WebSocket and listen for new ones
            repository.observeLiveMessages(connectionId).collect { event ->
                when (event) {
                    is ChatEvent.Message -> {
                        // Standard message: Add it to the top of the list
                        _messages.update { currentList ->
                            currentList + event.message
                        }                    }
                    is ChatEvent.Reaction -> {
                        _messages.update { currentList ->
                            currentList.map { msg ->
                                if (msg.id == event.messageId) {
                                    // Remove their old reaction (if any) and add the new one
                                    val updatedReactions = msg.reactions
                                        .filter { it.userId != event.userId }
                                        .toMutableList()
                                        .apply { add(ReactionDto(event.userId, event.emoji)) }

                                    msg.copy(reactions = updatedReactions)
                                } else msg
                            }
                        }
                    }
                    is ChatEvent.RemoveReaction -> {
                        _messages.update { currentList ->
                            currentList.map { msg ->
                                if (msg.id == event.messageId) {
                                    // Filter out the user's reaction
                                    val updatedReactions = msg.reactions.filter { it.userId != event.userId }
                                    msg.copy(reactions = updatedReactions)
                                } else msg
                            }
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(text: String, replyToMessageId: String? = null) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Send it to the server.
            // The server will broadcast it back, which will be caught by `observeLiveMessages`
            // and automatically added to the UI!
            repository.sendMessage(text, replyToMessageId)
            NotificationEventBus.triggerRefresh()
        }
    }

    fun sendReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            // Send it to the server.
            // The server will broadcast it back, which will be caught by `observeLiveMessages`
            // and automatically added to the UI!
            repository.sendReaction(messageId, emoji)
            NotificationEventBus.triggerRefresh()
        }
    }

    fun removeReaction(messageId: String) {
        viewModelScope.launch {
            repository.removeReaction(messageId)
            NotificationEventBus.triggerRefresh()
        }
    }

    fun markMessagesAsRead() {
        viewModelScope.launch {
            repository.markMessagesAsRead(connectionId)
            NotificationEventBus.triggerRefresh()
        }
    }

    fun updateChatTheme(connectionId: String, themeName: String) {
        viewModelScope.launch {
            repository.updateChatTheme(connectionId, themeName)
        }
    }

    fun fetchOtherUserProfile(username: String) {
        viewModelScope.launch {
            userRepository.getUserProfile(username).onSuccess { profile ->
                _otherUserProfile.value = profile
            }
        }
    }
}