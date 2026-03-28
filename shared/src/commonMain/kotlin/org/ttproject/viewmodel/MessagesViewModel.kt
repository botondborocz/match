package org.ttproject.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.ttproject.data.ChatThreadDto
import org.ttproject.repository.ChatRepository

class MessagesViewModel(
    private val repository: ChatRepository
) : ViewModel() {

    private val _threads = MutableStateFlow<List<ChatThreadDto>>(emptyList())
    val threads: StateFlow<List<ChatThreadDto>> = _threads.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadConnections()
    }

    fun loadConnections() {
        viewModelScope.launch {
            _isLoading.value = true
            _threads.value = repository.getConnections()
            _isLoading.value = false
        }
    }
}