package org.ttproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.ttproject.AppColors
import org.ttproject.components.MobileTopBar
import org.ttproject.components.PushNotificationManager
import org.ttproject.data.ChatThreadDto
import org.ttproject.data.TokenStorage
import org.ttproject.isIosPlatform
import org.ttproject.util.ClearChatNotificationEffect
import org.ttproject.util.formatMessageTime
import org.ttproject.viewmodel.ChatViewModel
import org.ttproject.viewmodel.MessagesViewModel
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

data class ChatThread(
    val id: String,
    val otherUserName: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(
    viewModel: MessagesViewModel = koinViewModel(),
    playAnimation: Boolean = true,
    bottomNavPadding: Dp,
    onNavigateToChat: (String, String) -> Unit
) {
    val chatThreads by viewModel.threads.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 👇 1. Just remember the state, no need to manually check isRefreshing anymore!
    val pullToRefreshState = rememberPullToRefreshState()

    // Refresh when returning from ChatDetailScreen (or opening for the first time)
    LaunchedEffect(Unit) {
        viewModel.loadConnections()
    }

    PushNotificationManager { fcmToken ->
        viewModel.savePushToken(fcmToken)
    }

    val listVisibleState = remember(playAnimation) {
        MutableTransitionState(!playAnimation).apply { targetState = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(bottom = bottomNavPadding + 10.dp)
    ) {
        MobileTopBar()

        // ONLY show the center spinner if we have absolutely no data yet.
        if (isLoading && chatThreads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.AccentOrange)
            }
        } else {
            // 👇 2. Use the new PullToRefreshBox! It handles the nested scroll automatically.
            PullToRefreshBox(
                isRefreshing = isLoading, // Binds directly to your ViewModel state!
                onRefresh = { viewModel.loadConnections() }, // Triggers when the user swipes down
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (chatThreads.isEmpty()) {
                    // We wrap the Empty State in a LazyColumn so it can still be pulled down to refresh!
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item { EmptyMessagesState() }
                    }
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

                        itemsIndexed(chatThreads, key = { _, thread -> thread.id }) { index, thread ->
                            Column {
                                AnimatedVisibility(
                                    visibleState = listVisibleState,
                                    enter = slideInVertically(
                                        initialOffsetY = { 50 },
                                        animationSpec = tween(durationMillis = 300, delayMillis = index * 40)
                                    ) + fadeIn(animationSpec = tween(durationMillis = 300, delayMillis = index * 40))
                                ) {
                                    ChatListItem(
                                        thread = thread,
                                        onClick = { onNavigateToChat(thread.id, thread.otherUserName) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatViewModel = koinViewModel<ChatViewModel>(),
    chatId: String,
    otherUsername: String,
    bottomNavPadding: Dp,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var messageText by remember { mutableStateOf("") }
    val bottomNavInset = remember(bottomNavPadding) { WindowInsets(bottom = bottomNavPadding + 10.dp) }
    val focusManager = LocalFocusManager.current
    val tokenStorage: TokenStorage = koinInject()

    var selectedMessageId by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()

    ClearChatNotificationEffect(chatId = chatId)

    LaunchedEffect(chatId) {
        viewModel.markMessagesAsRead()
    }

    val presentedMessageIds = remember { mutableSetOf<String>() }
    var isInitialLoad by remember { mutableStateOf(true) }

    if (isInitialLoad && messages.isNotEmpty()) {
        presentedMessageIds.addAll(messages.map { it.id })
        isInitialLoad = false
    }

    var previousMessageCount by remember { mutableStateOf(messages.size) }
    LaunchedEffect(messages.size) {
        if (messages.size > previousMessageCount) {
            listState.animateScrollToItem(0)
        }
        previousMessageCount = messages.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .windowInsetsPadding(WindowInsets.ime.union(bottomNavInset))
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
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
                        Text(getInitials(otherUsername), color = AppColors.AccentOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(otherUsername, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = if (isIosPlatform()) Icons.Filled.ArrowBackIosNew else Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Background)
        )

        HorizontalDivider(color = AppColors.TextGray.copy(alpha = 0.2f))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true
        ) {
            val currentUserId = tokenStorage.getUserId() ?: ""

            // 👇 1. Save the reversed list to a variable so we can look up indexes safely
            val displayMessages = messages.reversed()

            itemsIndexed(displayMessages, key = { _, msg -> msg.id }) { index, msg ->
                val isMe = msg.senderId == currentUserId
                val isSelected = selectedMessageId == msg.id

                // 👇 2. Look ahead and behind!
                // Because reverseLayout = true, the "older" message is physically ABOVE this one
                val olderMessage = displayMessages.getOrNull(index + 1)
                // The "newer" message is physically BELOW this one
                val newerMessage = displayMessages.getOrNull(index - 1)

                println(olderMessage?.createdAt)
                println(msg.createdAt)
                println(newerMessage?.createdAt)

                // 👇 1. Should we show the centered time above this specific message?
                val showTimeHeader = olderMessage == null ||
                        isTimeGapGreater(olderMessage.createdAt, msg.createdAt, 30)

                // 👇 2. Will the message BELOW this one show a time header?
                val newerShowsHeader = newerMessage != null &&
                        isTimeGapGreater(msg.createdAt, newerMessage.createdAt, 30)

                // 👇 3. Only connect the bubbles if they are the same sender AND no time header interrupts them!
                val visuallyConnectToOlder = olderMessage?.senderId == msg.senderId && !showTimeHeader
                val visuallyConnectToNewer = newerMessage?.senderId == msg.senderId && !newerShowsHeader

                val shouldAnimate = !presentedMessageIds.contains(msg.id)

                LaunchedEffect(msg.id) {
                    presentedMessageIds.add(msg.id)
                }

                // 👇 3. Pass the new grouping flags down to the bubble!
                AnimatedMessageBubble(
                    text = msg.content,
                    isMe = isMe,
                    time = msg.createdAt,
                    playAnimation = shouldAnimate,
                    showTimeHeader = showTimeHeader,
                    isOlderSame = visuallyConnectToOlder,
                    isNewerSame = visuallyConnectToNewer,
                    isSelected = isSelected, // 👈 Pass the selection state
                    onClick = {
                        // 👇 4. Toggle the selection! If it's already selected, clear it.
                        selectedMessageId = if (isSelected) null else msg.id
                    }
                )
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
                        viewModel.sendMessage(messageText)
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
    }
}

@Composable
fun AnimatedMessageBubble(
    text: String,
    isMe: Boolean,
    time: String,
    playAnimation: Boolean,
    showTimeHeader: Boolean,
    isOlderSame: Boolean, // 👈 Add this
    isNewerSame: Boolean,  // 👈 Add this
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val visibleState = remember {
        MutableTransitionState(initialState = !playAnimation).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> if (isMe) fullWidth / 2 else -fullWidth / 2 }
        ) + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 2 }
        ) + fadeIn(),
    ) {
        Column {
            // 👇 THE CENTERED TIMESTAMP!
            if (showTimeHeader) {
                Text(
                    text = formatMessageTime(time), // You can format this to say "Today 14:30" etc.
                    color = AppColors.TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp, bottom = 8.dp) // Gives it nice breathing room
                )
            } else {
                // 👇 THE TOGGLEABLE TIMESTAMP!
                // It sits right above the bubble and animates its height/fade
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Text(
                        text = formatMessageTime(time),
                        color = AppColors.TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp), // Tiny gap before the bubble
                        textAlign = TextAlign.Center
                    )
                }
            }

            ChatBubble(
                text = text,
                isMe = isMe,
                isOlderSame = isOlderSame,
                isNewerSame = isNewerSame,
                isSelected = isSelected,
                onClick = onClick
            )
            // 👇 Dynamic Spacing!
            // If the message below this one is from the same user, gap is 4dp.
            // If it's a new user (or the end of the chat), gap is 16dp.
            Spacer(modifier = Modifier.height(if (isNewerSame) 4.dp else 16.dp))
        }
    }
}

@Composable
fun ChatBubble(
    text: String,
    isMe: Boolean,
    isOlderSame: Boolean,
    isNewerSame: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 👇 1. The Corner Radius Math
    // If it's my message (right side), we flatten the right corners to connect them.
    // If it's their message (left side), we flatten the left corners to connect them.
    val topStart = if (!isMe && isOlderSame) 4.dp else 16.dp
    val bottomStart = if (!isMe && isNewerSame) 4.dp else 16.dp

    val topEnd = if (isMe && isOlderSame) 4.dp else 16.dp
    val bottomEnd = if (isMe && isNewerSame) 4.dp else 16.dp

    // 👇 Animate the background color based on selection state!
    val baseColor = if (isMe) AppColors.AccentOrange else AppColors.SurfaceDark

    // If selected, we slightly darken the bubble (or lighten it, depending on your theme)
    // Adjust the overlay color to match your design.
    val targetColor = if (isSelected) {
        // Blends a bit of black over the base color for a "pressed" look
        Color(
            red = baseColor.red * 0.85f,
            green = baseColor.green * 0.85f,
            blue = baseColor.blue * 0.85f,
            alpha = baseColor.alpha
        )
    } else {
        baseColor
    }

    val animatedBackgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200) // Smooth transition
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.TopEnd else Alignment.TopStart
    ) {
        val maxBubbleWidth = maxWidth * 0.75f

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(
                        RoundedCornerShape(
                            topStart = topStart,
                            topEnd = topEnd,
                            bottomStart = bottomStart,
                            bottomEnd = bottomEnd
                        )
                    )
                    .background(animatedBackgroundColor)
                    .clickable { onClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = text,
                    color = if (isMe) Color.White else AppColors.TextPrimary,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun ChatListItem(thread: ChatThreadDto, onClick: () -> Unit) {
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
                    Text(
                        text = formatMessageTime(thread.timestamp),
                        color = if (thread.unreadCount > 0) AppColors.AccentOrange else AppColors.TextGray,
                        fontSize = 12.sp,
                        fontWeight = if (thread.unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
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

fun getInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+"))
    return if (parts.size >= 2) "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
    else if (name.isNotEmpty()) name.take(2).uppercase() else "?"
}

fun isTimeGapGreater(olderTime: String, newerTime: String, minutes: Int): Boolean {
    return try {
        // 👇 1. Use Instant.parse() instead of .toInstant()
        val olderInstant = Instant.parse(olderTime)
        val newerInstant = Instant.parse(newerTime)

        // 2. Calculate the exact time difference
        val timeDifference = newerInstant - olderInstant

        // 3. Check if it's strictly greater than 1 hour
        timeDifference > minutes.minutes

    } catch (e: Exception) {
        true
    }
}