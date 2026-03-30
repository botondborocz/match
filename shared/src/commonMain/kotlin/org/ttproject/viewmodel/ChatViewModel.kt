package org.ttproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ttproject.data.Location
import org.ttproject.data.MessageDto
import org.ttproject.repository.ChatRepository

class ChatViewModel(
    private val repository: ChatRepository,
    private val connectionId: String,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

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
            repository.observeLiveMessages(connectionId).collect { newMessage ->
                _messages.value = _messages.value + newMessage
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Send it to the server.
            // The server will broadcast it back, which will be caught by `observeLiveMessages`
            // and automatically added to the UI!
            repository.sendMessage(text)
        }
    }

    fun markMessagesAsRead() {
        viewModelScope.launch {
            repository.markMessagesAsRead(connectionId)
        }
    }
}