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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
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
import org.ttproject.components.AdaptivePullToRefresh
import kotlin.time.Duration.Companion.minutes
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

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
    onNavigateToChat: (String, String, String?) -> Unit
) {
    val chatThreads by viewModel.filteredThreads.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isLoading by viewModel.isLoading.collectAsState()

    // 👇 1. Just remember the state, no need to manually check isRefreshing anymore!
    val pullToRefreshState = rememberPullToRefreshState()

    // Refresh when returning from ChatDetailScreen (or opening for the first time)
//    LaunchedEffect(Unit) {
//        if (chatThreads.isEmpty()) {
//            viewModel.loadConnections()
//        }
//    }

    PushNotificationManager { fcmToken ->
        viewModel.savePushToken(fcmToken)
    }

    val listVisibleState = remember(playAnimation) {
        MutableTransitionState(!playAnimation).apply { targetState = true }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
//            .background(AppColors.Background)
            .padding(bottom = bottomNavPadding + 0.dp)
    ) {
        MobileTopBar(
            showSearch = true,
            onSearchClick = { isSearchExpanded = true }
        )

        // ONLY show the center spinner if we have absolutely no data yet.
        if (isLoading && chatThreads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.AccentOrange)
            }
        } else {
//            AdaptivePullToRefresh(
//                isRefreshing = isLoading,
//                onRefresh = { viewModel.loadConnections() },
//                modifier = Modifier.fillMaxSize()
//            ) {
            // 👇 ONE single LazyColumn for everything!
            LazyColumn(
                contentPadding = PaddingValues(top = 0.dp, bottom = 10.dp),
                modifier = Modifier.fillMaxSize()
            ) {

                // --- ALWAYS RENDER THE SEARCH BAR ---
                item {
                    AnimatedVisibility(
                        visible = isSearchExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LaunchedEffect(Unit) {
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                }

                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    placeholder = {
                                        Text(
                                            "Search username...",
                                            color = AppColors.TextGray
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(focusRequester),
                                    singleLine = true,
                                    shape = RoundedCornerShape(24.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.AccentOrange,
                                        unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f),
                                        focusedTextColor = AppColors.TextPrimary,
                                        unfocusedTextColor = AppColors.TextPrimary
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            isSearchExpanded = false
                                            viewModel.updateSearchQuery("") // Clear the list when closing
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Search",
                                                tint = AppColors.TextPrimary
                                            )
                                        }
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // --- CONDITIONALLY RENDER THE LIST OR EMPTY STATES ---
                if (chatThreads.isEmpty()) {
                    item {
                        // 👇 Differentiate between an empty search vs a totally empty inbox!
                        if (searchQuery.isNotBlank()) {
                            EmptySearchState(searchQuery)
                        } else {
                            EmptyMessagesState()
                        }
                    }
                } else {
                    itemsIndexed(chatThreads, key = { _, thread -> thread.id }) { index, thread ->
                        Column {
                            ChatListItem(
                                thread = thread,
                                onClick = {
                                    keyboardController?.hide()
                                    isSearchExpanded = false
                                    viewModel.updateSearchQuery("")
                                    onNavigateToChat(
                                        thread.id,
                                        thread.otherUserName,
                                        thread.otherUserImageUrl
                                    )
                                }
                            )
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
    otherUserImageUrl: String?,
    bottomNavPadding: Dp,
    onBack: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var messageText by remember { mutableStateOf("") }

    // 👇 1. New State to track the reply!
    var replyingToMessageId by remember { mutableStateOf<String?>(null) }
    val replyingToMessage = remember(replyingToMessageId, messages) {
        messages.find { it.id == replyingToMessageId }
    }

    val imeInsets = if (isIosPlatform()) WindowInsets.ime else WindowInsets.ime
    val bottomNavInset = remember(bottomNavPadding) { WindowInsets(bottom = bottomNavPadding + 10.dp) }
    val focusManager = LocalFocusManager.current
    val tokenStorage: TokenStorage = koinInject()

    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    ClearChatNotificationEffect(chatId = chatId)

    LaunchedEffect(chatId) { viewModel.markMessagesAsRead() }

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
            .windowInsetsPadding(bottomNavInset.union(imeInsets))
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
                        if (!otherUserImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = otherUserImageUrl,
                                contentDescription = "Profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = getInitials(otherUsername),
                                color = AppColors.AccentOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
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
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true
        ) {
            val currentUserId = tokenStorage.getUserId() ?: ""
            val displayMessages = messages.reversed()

            itemsIndexed(displayMessages, key = { _, msg -> msg.id }) { index, msg ->
                val isMe = msg.senderId == currentUserId
                val isSelected = selectedMessageId == msg.id

                val olderMessage = displayMessages.getOrNull(index + 1)
                val newerMessage = displayMessages.getOrNull(index - 1)

                val showTimeHeader = olderMessage == null || isTimeGapGreater(olderMessage.createdAt, msg.createdAt, 30)
                val newerShowsHeader = newerMessage != null && isTimeGapGreater(msg.createdAt, newerMessage.createdAt, 30)

                val visuallyConnectToOlder = olderMessage?.senderId == msg.senderId && !showTimeHeader
                val visuallyConnectToNewer = newerMessage?.senderId == msg.senderId && !newerShowsHeader

                val shouldAnimate = !presentedMessageIds.contains(msg.id)

                LaunchedEffect(msg.id) { presentedMessageIds.add(msg.id) }

                // 👇 1. FIND THE REPLIED MESSAGE!
                val repliedMessage = msg.replyToMessageId?.let { replyId ->
                    messages.find { it.id == replyId }
                }
                val repliedText = repliedMessage?.content
                val repliedSender = if (repliedMessage?.senderId == currentUserId) "You" else otherUsername

                AnimatedMessageBubble(
                    text = msg.content,
                    isMe = isMe,
                    time = msg.createdAt,
                    playAnimation = shouldAnimate,
                    showTimeHeader = showTimeHeader,
                    isOlderSame = visuallyConnectToOlder,
                    isNewerSame = visuallyConnectToNewer,
                    isSelected = isSelected,
                    // 👇 2. Pass the preview data down!
                    repliedText = repliedText,
                    repliedSender = repliedSender,
                    onClick = { selectedMessageId = if (isSelected) null else msg.id },
                    onSwipeToReply = { replyingToMessageId = msg.id }
                )
            }
        }

        // --- THE KEYBOARD-AWARE INPUT AREA ---
        Column(modifier = Modifier.fillMaxWidth().background(AppColors.Background)) {

            // 👇 3. THE REPLY PREVIEW BANNER
            AnimatedVisibility(
                visible = replyingToMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (replyingToMessage != null) {
                    ReplyPreview(
                        messageContent = replyingToMessage.content,
                        onCancel = { replyingToMessageId = null }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                            // 👇 4. Pass BOTH the text and the reply ID to the ViewModel
                            viewModel.sendMessage(messageText, replyingToMessageId)

                            // Reset everything
                            messageText = ""
                            replyingToMessageId = null
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
}

@Composable
fun AnimatedMessageBubble(
    text: String,
    isMe: Boolean,
    time: String,
    playAnimation: Boolean,
    showTimeHeader: Boolean,
    isOlderSame: Boolean,
    isNewerSame: Boolean,
    isSelected: Boolean,
    repliedText: String?,
    repliedSender: String?,
    onClick: () -> Unit,
    onSwipeToReply: () -> Unit // 👈 New Parameter
) {
    val visibleState = remember {
        MutableTransitionState(initialState = !playAnimation).apply { targetState = true }
    }

    // 👇 Physics and Haptics for the swipe
    val offsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visibleState = visibleState,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> if (isMe) fullWidth / 2 else -fullWidth / 2 }
        ) + slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight / 2 }
        ) + fadeIn(),
    ) {
        Column {
            if (showTimeHeader) {
                Text(
                    text = formatMessageTime(time),
                    color = AppColors.TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp, bottom = 8.dp)
                )
            } else {
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
                            .padding(top = 8.dp, bottom = 8.dp),
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
                onClick = onClick,
                repliedText = repliedText,
                repliedSender = repliedSender,
                // 👇 Apply the gesture mechanics
                modifier = Modifier
                    .graphicsLayer { translationX = offsetX.value }
                    .pointerInput(Unit) {
                        var hasTriggeredHaptic = false
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX.value > 120f) {
                                    onSwipeToReply() // Trigger the reply state!
                                }
                                // Spring back to original position
                                coroutineScope.launch {
                                    offsetX.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))
                                }
                                hasTriggeredHaptic = false
                            },
                            onDragCancel = {
                                coroutineScope.launch { offsetX.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) }
                                hasTriggeredHaptic = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                // Only allow swiping to the right (coerceIn prevents left swipes)
                                val newOffset = (offsetX.value + dragAmount).coerceIn(0f, 200f)
                                coroutineScope.launch { offsetX.snapTo(newOffset) }

                                // Buzz the phone slightly when they pull far enough to trigger the reply
                                if (newOffset > 120f && !hasTriggeredHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hasTriggeredHaptic = true
                                } else if (newOffset < 120f) {
                                    hasTriggeredHaptic = false
                                }
                            }
                        )
                    }
            )
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
    // 👇 Accept the new parameters
    repliedText: String?,
    repliedSender: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val topStart = if (!isMe && isOlderSame) 4.dp else 16.dp
    val bottomStart = if (!isMe && isNewerSame) 4.dp else 16.dp
    val topEnd = if (isMe && isOlderSame) 4.dp else 16.dp
    val bottomEnd = if (isMe && isNewerSame) 4.dp else 16.dp

    val baseColor = if (isMe) AppColors.AccentOrange else AppColors.SurfaceDark
    val targetColor = if (isSelected) {
        Color(
            red = baseColor.red * 0.85f, green = baseColor.green * 0.85f,
            blue = baseColor.blue * 0.85f, alpha = baseColor.alpha
        )
    } else {
        baseColor
    }

    val animatedBackgroundColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200)
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.TopEnd else Alignment.TopStart
    ) {
        // Slightly wider max bubble width to accommodate quotes beautifully
        val maxBubbleWidth = maxWidth * 0.80f

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {

            // 👇 Change the inner box to a Column so we can stack the Quote and the Message
            Column(
                modifier = Modifier
                    .widthIn(max = maxBubbleWidth)
                    .clip(RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart))
                    .background(animatedBackgroundColor)
                    .clickable { onClick() }
                    .padding(horizontal = 12.dp, vertical = 10.dp) // Adjusted padding slightly
            ) {

                // 👇 THE QUOTE PREVIEW UI
                if (repliedText != null && repliedSender != null) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            // Darken the background slightly for the quote box
                            .background(Color.Black.copy(alpha = 0.15f))
                            .height(IntrinsicSize.Min) // Forces the Row to wrap the text height tightly
                    ) {
                        // The left accent line
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(if (isMe) Color.White else AppColors.AccentOrange)
                        )

                        // The quoted sender and text
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = repliedSender,
                                color = if (isMe) Color.White else AppColors.AccentOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = repliedText,
                                color = if (isMe) Color.White.copy(alpha = 0.85f) else AppColors.TextPrimary.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // The actual message
                Text(
                    text = text,
                    color = if (isMe) Color.White else AppColors.TextPrimary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
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

            // --- AVATAR BOX ---
            Box(modifier = Modifier.size(52.dp)) {
                // The main circular container
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(AppColors.SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    // 👇 NEW: Check if there is an image URL
                    if (!thread.otherUserImageUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thread.otherUserImageUrl,
                            contentDescription = "Profile picture of ${thread.otherUserName}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop // Keeps the image perfectly circular without squishing
                        )
                    } else {
                        // FALLBACK INITIALS
                        Text(
                            text = getInitials(thread.otherUserName),
                            color = AppColors.AccentOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Online Indicator (Stays exactly the same)
                if (thread.isOnline) {
                    Box(modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).offset(x = (-2).dp, y = (-2).dp).clip(CircleShape).background(Color(0xFF4CAF50)).padding(2.dp)) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF4CAF50)))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // --- TEXT CONTENT ---
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
fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(48.dp)) // Push it down a bit from the search bar
        Text("🔍", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No results found", color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "We couldn't find any chats matching \"$query\".",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
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

@Composable
fun ReplyPreview(
    messageContent: String,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(AppColors.SurfaceDark)
            .border(
                width = 1.dp,
                color = AppColors.TextGray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(AppColors.AccentOrange)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text("Replying to", color = AppColors.AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = messageContent,
                color = AppColors.TextPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancel Reply", tint = AppColors.TextGray)
        }
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