package org.ttproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ttproject.AppColors

// --- MOCK DATA MODEL ---
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
    onNavigateToChat: (String) -> Unit = { _ -> } // Placeholder lambda, replace with actual navigation logic
) {
    // Dummy Data - Replace with your actual ViewModel data later
    val chatThreads = remember {
        listOf(
            ChatThread("1", "Gábor Kovács", "Are we still on for 6 PM at Corvin?", "10:42 AM", 2, true),
            ChatThread("2", "Anna Németh", "Haha yeah, my backhand was terrible today \uD83D\uDE2D", "Yesterday", 0, false),
            ChatThread("3", "Péter Szabó", "Let me know when you add the new table!", "Tuesday", 1, true),
            ChatThread("4", "Table Tennis Fan", "Thanks for the game!", "Mar 18", 0, false)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
    ) {
        if (chatThreads.isEmpty()) {
            EmptyMessagesState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp), // Bottom padding for NavBar
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
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

                // Chat List
                items(chatThreads, key = { it.id }) { thread ->
                    ChatListItem(
                        thread = thread,
                        onClick = { onNavigateToChat(thread.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    thread: ChatThread,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent, // Transparent so it blends with Background, but gives us the ripple!
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- AVATAR & ONLINE STATUS ---
            Box(
                modifier = Modifier.size(52.dp)
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getInitials(thread.otherUserName),
                        color = AppColors.AccentOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Online Indicator Badge
                if (thread.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50)) // Green online dot
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- MESSAGE CONTENT ---
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.otherUserName,
                        color = AppColors.TextPrimary,
                        fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = thread.timestamp,
                        color = if (thread.unreadCount > 0) AppColors.AccentOrange else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.lastMessage,
                        color = if (thread.unreadCount > 0) AppColors.TextPrimary else Color.Gray,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Unread Badge
                    if (thread.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(AppColors.AccentOrange),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = thread.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
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
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // You can replace this emoji with an actual Lucide/Vector icon (like MessageSquare) if you prefer!
        Text("💬", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Messages Yet",
            color = AppColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Swipe on players nearby or join a table to start a conversation.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// Helper to extract initials
private fun getInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+"))
    return if (parts.size >= 2) {
        "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
    } else if (name.isNotEmpty()) {
        name.take(2).uppercase()
    } else {
        "?"
    }
}