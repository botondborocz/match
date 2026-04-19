package org.ttproject.data

import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

// Pure Kotlin random ID generator to replace java.util.UUID
fun generateRandomId(): String {
    val charPool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..16).map { charPool.random() }.joinToString("")
}

data class AiChatMessage(
    val id: String,
    val content: String,
    val isMe: Boolean,
    val timestamp: Long
)

object DummyAiData {
    // Get the current time in pure Kotlin Multiplatform
    private val now = kotlin.time.Clock.System.now()

    val chatHistory = listOf(
        AiChatMessage(
            id = generateRandomId(),
            content = "Hello! I'm your AI Table Tennis Coach. How can I help you improve your game today?",
            isMe = false,
            timestamp = (now - 10.minutes).toEpochMilliseconds()
        ),
        AiChatMessage(
            id = generateRandomId(),
            content = "I'm having trouble returning fast, deep serves to my backhand.",
            isMe = true,
            timestamp = (now - 5.minutes).toEpochMilliseconds()
        ),
        AiChatMessage(
            id = generateRandomId(),
            content = "That's a common challenge! For deep backhand serves, focus on a shorter backswing and taking the ball early right off the bounce. Make sure your weight is slightly forward on your toes. Want me to suggest a specific drill?",
            isMe = false,
            timestamp = (now - 4.minutes).toEpochMilliseconds()
        ),
        AiChatMessage(
            id = generateRandomId(),
            content = "Yes, please. I need a drill to practice that.",
            isMe = true,
            timestamp = (now - 1.minutes).toEpochMilliseconds()
        ),
        AiChatMessage(
            id = generateRandomId(),
            content = "Great! Let's try the 'Multi-ball Deep Block Drill'. Have your training partner serve fast to your deep backhand, and focus entirely on a compact punch. Aim for 20 successful returns in a row.",
            isMe = false,
            timestamp = now.toEpochMilliseconds()
        )
    )
}