package org.ttproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.ttproject.AppColors
import org.ttproject.data.AiChatMessage

// 👇 Look at these clean, stateless parameters!
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    messages: List<AiChatMessage>,
    isTyping: Boolean,
    onSendMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(AppColors.Background)) {
        // --- Top Bar ---
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(AppColors.AccentOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("AI", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("AI Coach", color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.TextPrimary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.SurfaceDark)
        )

        // --- Chat List ---
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true // Important for chat apps!
        ) {
            // Show typing indicator at the bottom (index 0 in reverse layout)
            if (isTyping) {
                item {
                    AiTypingIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // We reverse the list here because LazyColumn is set to reverseLayout=true
            items(messages.reversed()) { msg ->
                AiChatBubble(text = msg.content, isMe = msg.isMe)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // --- Input Area ---
        Row(
            modifier = Modifier.fillMaxWidth().background(AppColors.SurfaceDark).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about technique, drills, or tactics...", color = AppColors.TextGray) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.AccentOrange,
                    unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = "" // Clear input after sending
                    }
                },
                modifier = Modifier.clip(CircleShape).background(AppColors.AccentOrange)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun AiChatBubble(text: String, isMe: Boolean) {
    val bgColor = if (isMe) AppColors.AccentOrange else AppColors.SurfaceDark
    val textColor = if (isMe) Color.White else AppColors.TextPrimary
    val shape = if (isMe) {
        RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(text = text, color = textColor, fontSize = 15.sp)
        }
    }
}

@Composable
fun AiTypingIndicator() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)).background(AppColors.SurfaceDark).padding(12.dp)
        ) {
            Text("Coach is typing...", color = AppColors.TextGray, fontSize = 14.sp)
        }
    }
}