package org.ttproject.data

import kotlinx.serialization.Serializable

@Serializable
data class MessageDto(
    val id: String,
    val senderId: String,
    val content: String,
    val createdAt: String
)

@Serializable
data class ChatThreadDto(
    val id: String, // This is the connectionId
    val otherUserName: String,
    val otherUserImageUrl: String? = null,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false
)