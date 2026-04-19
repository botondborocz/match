package org.ttproject.screens

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ttproject.data.AiChatMessage
import org.ttproject.data.DummyAiData
import org.ttproject.data.generateRandomId

@Composable
fun DummyAiChatPlayground(onBack: () -> Unit) {
    val messages = remember { mutableStateListOf(*DummyAiData.chatHistory.toTypedArray()) }
    var isTyping by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AiChatScreen(
        messages = messages,
        isTyping = isTyping,
        onSendMessage = { text ->
            // 1. User sends message (Using Pure Kotlin ID and Time)
            messages.add(
                AiChatMessage(
                    id = generateRandomId(),
                    content = text,
                    isMe = true,
                    timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
                )
            )

            // 2. Simulate AI thinking
            coroutineScope.launch {
                isTyping = true
                delay(2000)
                isTyping = false

                // 3. AI responds (Using Pure Kotlin ID and Time)
                messages.add(
                    AiChatMessage(
                        id = generateRandomId(),
                        content = "This is a fake dummy response to test the UI! Your real Python backend will connect here later.",
                        isMe = false,
                        timestamp = kotlin.time.Clock.System.now().toEpochMilliseconds()
                    )
                )
            }
        },
        onBack = onBack
    )
}