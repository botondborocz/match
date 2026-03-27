package org.ttproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ttproject.AppColors
// 👇 Make sure to import your top bar!
import org.ttproject.components.MobileTopBar
import org.ttproject.isIosPlatform

data class ChatThread(
    val id: String,
    val otherUserName: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean = false
)

@Composable
fun MessagesScreen(
    playAnimation: Boolean = true,
    onNavigateToChat: (String) -> Unit
) {
    val chatThreads = remember {
        listOf(
            ChatThread("1", "Gábor Kovács", "Are we still on for 6 PM at Corvin?", "10:42 AM", 2, true),
            ChatThread("2", "Anna Németh", "Haha yeah, my backhand was terrible today \uD83D\uDE2D", "Yesterday", 0, false),
            ChatThread("3", "Péter Szabó", "Let me know when you add the new table!", "Tuesday", 1, true),
            ChatThread("4", "Table Tennis Fan", "Thanks for the game!", "Mar 18", 0, false),
            ChatThread("5", "Player 5", "Hey, want to play?", "Mar 10", 0, false),
            ChatThread("6", "Player 6", "Hey, want to play?", "Mar 8", 0, false),
            ChatThread("7", "Player 7", "Hey, want to play?", "Mar 5", 0, false),
            ChatThread("8", "Player 8", "Hey, want to play?", "Mar 1", 0, false),
            ChatThread("9", "Player 9", "Hey, want to play?", "Feb 25", 0, false),
            ChatThread("10", "Player 10", "Hey, want to play?", "Feb 20", 0, false)
        )
    }

    // 👇 THE MAGIC STATE:
    // This tells Compose: "Start invisible, but immediately animate to visible."
    // Because we use `remember` (not rememberSaveable), it resets when switching tabs,
    // but stays "completed" when you hit the back button from a chat!
    val listVisibleState = remember(playAnimation) {
        MutableTransitionState(!playAnimation).apply { targetState = true }
    }

    // 👇 Changed this from a Box to a Column to stack the local TopBar and the Content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        // 👇 Local Top Bar! This will now slide left smoothly with the screen.
        MobileTopBar()

        if (chatThreads.isEmpty()) {
            EmptyMessagesState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 0.dp, bottom = 10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Text(
                        text = "Messages",
                        color = AppColors.TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 👇 2. Use itemsIndexed to get the row number (index)
                itemsIndexed(chatThreads, key = { _, thread -> thread.id }) { index, thread ->

                    // 👇 3. Wrap the item in AnimatedVisibility
                    AnimatedVisibility(
                        visibleState = listVisibleState,
                        enter = slideInVertically(
                            // Start slightly lower (50px) and slide up to 0
                            initialOffsetY = { 50 },
                            // Stagger the delay by 40ms per item
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 40
                            )
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 40
                            )
                        )
                    ) {
                        ChatListItem(
                            thread = thread,
                            onClick = { onNavigateToChat(thread.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chatId: String,
    bottomNavPadding: Dp,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }
    val chatPartnerName = if (chatId == "1") "Gábor Kovács" else "Player $chatId"
    val bottomNavInset = remember(bottomNavPadding) { WindowInsets(bottom = bottomNavPadding + 10.dp) }
    val focusManager = LocalFocusManager.current
//    val customBottomNavInset = WindowInsets(bottom = bottomNavPadding + 10.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // --- ANCHORED TOP BAR ---
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(AppColors.SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getInitials(chatPartnerName), color = AppColors.AccentOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(chatPartnerName, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    // 👇 Swap the icon dynamically based on the platform!
                    Icon(
                        imageVector = if (isIosPlatform()) {
                            Icons.Filled.ArrowBackIosNew // ⬅️ iOS Chevron
                        } else {
                            Icons.AutoMirrored.Filled.ArrowBack // ⬅️ Android Arrow
                        },
                        contentDescription = "Back",
                        tint = AppColors.TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        HorizontalDivider(color = AppColors.TextGray.copy(alpha = 0.2f))

        // This takes all remaining space. When keyboard opens, this shrinks!
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true
        ) {
            item {
                ChatBubble(text = "Are we still on for 6 PM at Corvin?", isMe = false, time = "10:42 AM")
                Spacer(modifier = Modifier.height(16.dp))
                ChatBubble(text = "Sounds good!", isMe = true, time = "10:40 AM")
                Spacer(modifier = Modifier.height(16.dp))
                ChatBubble(text = "Hey! Want to play today?", isMe = false, time = "10:35 AM")
            }
        }

        // --- THE KEYBOARD-AWARE INPUT ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.Background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a message...", color = AppColors.TextGray) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.AccentOrange,
                    unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f),
                    focusedTextColor = AppColors.TextPrimary,
                    unfocusedTextColor = AppColors.TextPrimary
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (messageText.isNotBlank()) AppColors.AccentOrange else AppColors.SurfaceDark)
                    .clickable(enabled = messageText.isNotBlank()) {
                        messageText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (messageText.isNotBlank()) Color.White else AppColors.TextGray,
                    modifier = Modifier.size(20.dp).offset(x = 2.dp)
                )
            }
        }

        Spacer(
            Modifier.windowInsetsBottomHeight(
                WindowInsets.ime.union(bottomNavInset)
            )
        )
    }
}

@Composable
fun ChatBubble(text: String, isMe: Boolean, time: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) AppColors.AccentOrange else Color(0xFF1E2532))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = text,
                color = if (isMe) Color.White else AppColors.TextPrimary,
                fontSize = 15.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = time, color = AppColors.TextGray, fontSize = 11.sp)
    }
}

@Composable
fun ChatListItem(thread: ChatThread, onClick: () -> Unit) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(52.dp)) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(AppColors.SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(getInitials(thread.otherUserName), color = AppColors.AccentOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (thread.isOnline) {
                    Box(modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).offset(x = (-2).dp, y = (-2).dp).clip(CircleShape).background(Color(0xFF4CAF50)).padding(2.dp)) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF4CAF50)))
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(thread.otherUserName, color = AppColors.TextPrimary, fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Medium, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(thread.timestamp, color = if (thread.unreadCount > 0) AppColors.AccentOrange else Color.Gray, fontSize = 12.sp, fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(thread.lastMessage, color = if (thread.unreadCount > 0) AppColors.TextPrimary else Color.Gray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (thread.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(AppColors.AccentOrange), contentAlignment = Alignment.Center) {
                            Text(thread.unreadCount.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyMessagesState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💬", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No Messages Yet", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Swipe on players nearby or join a table to start a conversation.", color = Color.Gray, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

private fun getInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+"))
    return if (parts.size >= 2) "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
    else if (name.isNotEmpty()) name.take(2).uppercase() else "?"
}