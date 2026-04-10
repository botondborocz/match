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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.alpha

data class ChatThread(
    val id: String,
    val otherUserName: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int,
    val isOnline: Boolean = false
)

data class ReactionMenuData(
    val messageId: String,
    val isMe: Boolean,
    val bounds: Rect,
    val topStart: Dp,
    val topEnd: Dp,
    val bottomStart: Dp,
    val bottomEnd: Dp
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

    var selectedReactionMessageId by remember { mutableStateOf<String?>(null) }

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

    // 👇 1. Use our new data class instead of just a String ID
    var reactionMenuData by remember { mutableStateOf<ReactionMenuData?>(null) }

    // 👇 2. Wrap the whole screen in a Box so the overlay can sit on top of the TopBar!
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .windowInsetsPadding(bottomNavInset.union(imeInsets))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

                // ... inside ChatDetailScreen's LazyColumn ...

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

                    val repliedMessage = msg.replyToMessageId?.let { replyId -> messages.find { it.id == replyId } }
                    val repliedText = repliedMessage?.content
                    val repliedSender = if (repliedMessage?.senderId == currentUserId) "You" else otherUsername

                    // 👇 THE FIX: Wrap the bubble in a Box and reverse the Z-Index!
                    // This forces index 0 (newest message) to have the highest Z-Index,
                    // guaranteeing it draws ON TOP of the older messages.
                    Box(
                        modifier = Modifier.zIndex(displayMessages.size - index.toFloat())
                    ) {
                        AnimatedMessageBubble(
                            text = msg.content,
                            isMe = isMe,
                            time = msg.createdAt,
                            playAnimation = shouldAnimate,
                            showTimeHeader = showTimeHeader,
                            isOlderSame = visuallyConnectToOlder,
                            isNewerSame = visuallyConnectToNewer,
                            isSelected = isSelected,
                            repliedText = repliedText,
                            repliedSender = repliedSender,
                            reactionEmoji = msg.reactionEmoji,
                            onClick = { selectedMessageId = if (isSelected) null else msg.id },
                            onLongPress = { bounds, topStart, topEnd, bottomStart, bottomEnd ->
                                reactionMenuData = ReactionMenuData(
                                    messageId = msg.id,
                                    isMe = isMe,
                                    bounds = bounds,
                                    topStart = topStart,
                                    topEnd = topEnd,
                                    bottomStart = bottomStart,
                                    bottomEnd = bottomEnd
                                )
                            },
                            onSwipeToReply = { replyingToMessageId = msg.id }
                        )
                    }
                }
            }

            // --- THE KEYBOARD-AWARE INPUT AREA ---
            Column(modifier = Modifier.fillMaxWidth().background(AppColors.Background)) {
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Type a message...", color = AppColors.TextGray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.AccentOrange, unfocusedBorderColor = AppColors.TextGray.copy(alpha = 0.5f),
                            focusedTextColor = AppColors.TextPrimary, unfocusedTextColor = AppColors.TextPrimary
                        ),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(if (messageText.isNotBlank()) AppColors.AccentOrange else AppColors.SurfaceDark)
                            .clickable(enabled = messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText, replyingToMessageId)
                                messageText = ""
                                replyingToMessageId = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (messageText.isNotBlank()) Color.White else AppColors.TextGray, modifier = Modifier.size(20.dp).offset(x = 2.dp))
                    }
                }
            }
        }

        // 👇 4. THE MAGIC HIGHLIGHT OVERLAY
        AnimatedVisibility(
            visible = reactionMenuData != null,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.zIndex(100f)
        ) {
            reactionMenuData?.let { state ->
                val density = LocalDensity.current

                var overlayBounds by remember { mutableStateOf(Offset.Zero) }
                var overlaySize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            overlayBounds = coordinates.boundsInRoot().topLeft
                            overlaySize = coordinates.size
                        }
                ) {
                    // --- THE SCRIM (WITH A HOLE CUT OUT) ---
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { reactionMenuData = null })
                            }
                    ) {
                        val localBounds = state.bounds.translate(-overlayBounds)

                        val cornerRadiusPath = Path().apply {
                            addRoundRect(
                                RoundRect(
                                    rect = localBounds,
                                    topLeft = CornerRadius(state.topStart.toPx()),
                                    topRight = CornerRadius(state.topEnd.toPx()),
                                    bottomRight = CornerRadius(state.bottomEnd.toPx()),
                                    bottomLeft = CornerRadius(state.bottomStart.toPx())
                                )
                            )
                        }

                        clipPath(cornerRadiusPath, clipOp = ClipOp.Difference) {
                            drawRect(Color.Black.copy(alpha = 0.65f))
                        }
                    }

                    // --- THE REACTION PILL ---
                    // 1. Lock in the dimensions to calculate boundaries perfectly
                    val menuWidthDp = 270.dp
                    val menuHeightDp = 56.dp
                    val menuWidthPx = with(density) { menuWidthDp.toPx() }
                    val menuHeightPx = with(density) { menuHeightDp.toPx() }

                    val screenWidthPx = overlaySize.width.toFloat()
                    val localBounds = state.bounds.translate(-overlayBounds)

                    // 2. The Clipping Fix! Hard clamp the X value between safe screen margins
                    val safeMarginPx = with(density) { 16.dp.toPx() }

                    // 👇 THE FIX: Use maxOf() to guarantee maxMenuX never drops below safeMarginPx
                    val minMenuX = safeMarginPx
                    val maxMenuX = maxOf(minMenuX, screenWidthPx - menuWidthPx - safeMarginPx)

                    val idealX = if (state.isMe) localBounds.right - menuWidthPx else localBounds.left
                    val menuX = idealX.coerceIn(minMenuX, maxMenuX)

                    val isSpaceAbove = localBounds.top > menuHeightPx + 50f
                    val menuY = if (isSpaceAbove) localBounds.top - menuHeightPx - 20f else localBounds.bottom + 20f

                    val transformOrigin = if (state.isMe) {
                        TransformOrigin(1f, if (isSpaceAbove) 1f else 0f)
                    } else {
                        TransformOrigin(0f, if (isSpaceAbove) 1f else 0f)
                    }

                    // 3. The WhatsApp Drag States
                    val emojis = listOf("❤️", "😂", "😮", "😢", "🙏", "👍")
                    var hoveredIndex by remember { mutableStateOf(-1) }

                    Box(
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(menuX.toInt(), menuY.toInt()) }
                            .animateEnterExit(
                                enter = androidx.compose.animation.scaleIn(
                                    transformOrigin = transformOrigin,
                                    animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)
                                ),
                                exit = androidx.compose.animation.scaleOut(transformOrigin = transformOrigin)
                            )
                            .width(menuWidthDp)
                            .height(menuHeightDp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(AppColors.SurfaceDark)
                            // 4. The Magic Gesture Tracker!
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val itemWidth = menuWidthPx / emojis.size

                                    // Calculate which emoji we touched initially
                                    hoveredIndex = (down.position.x / itemWidth).toInt().coerceIn(0, emojis.size - 1)

                                    var isTracking = true
                                    while (isTracking) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()

                                        if (change.pressed) {
                                            // Finger is dragging: update the hovered index!
                                            hoveredIndex = (change.position.x / itemWidth).toInt().coerceIn(0, emojis.size - 1)
                                        } else {
                                            // Finger lifted: Trigger the reaction!
                                            isTracking = false
                                            if (hoveredIndex in emojis.indices) {
                                                viewModel.sendReaction(state.messageId, emojis[hoveredIndex])
                                            }
                                            reactionMenuData = null
                                            hoveredIndex = -1
                                        }
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            emojis.forEachIndexed { index, emoji ->
                                // 5. Smoothly animate the scale of the emoji under the finger!
                                val scale by animateFloatAsState(
                                    targetValue = if (hoveredIndex == index) 1.6f else 1f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                    ),
                                    label = "emojiScale"
                                )

                                Text(
                                    text = emoji,
                                    fontSize = 26.sp,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        // Slight lift effect when hovered
                                        translationY = if (hoveredIndex == index) -15f else 0f
                                    }
                                )
                            }
                        }
                    }
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
    reactionEmoji: String?,
    onClick: () -> Unit,
    onLongPress: (Rect, Dp, Dp, Dp, Dp) -> Unit,
    onSwipeToReply: () -> Unit
) {
    // 1. Simple, predictable state
    var targetAlpha by remember { mutableFloatStateOf(if (playAnimation) 0f else 1f) }
    var targetOffset by remember { mutableFloatStateOf(if (playAnimation) 100f else 0f) }

    // 2. Trigger animation immediately if it's a new message
    LaunchedEffect(Unit) {
        if (playAnimation) {
            targetAlpha = 1f
            targetOffset = 0f
        }
    }

    // 3. Fallback: If it gets pushed down the list, instantly snap to visible
    LaunchedEffect(playAnimation) {
        if (!playAnimation) {
            targetAlpha = 1f
            targetOffset = 0f
        }
    }

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(250),
        label = "alpha"
    )
    val offset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy),
        label = "offset"
    )

    val swipeOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(
        // 👇 THE FIX: Bypass the graphicsLayer cache bug by using physical layout modifiers!
        modifier = Modifier
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    x = if (isMe) (offset / 2).toInt() else (-offset / 2).toInt(),
                    y = offset.toInt()
                )
            }
            .alpha(alpha)
    ) {
        // --- THE CENTERED TIMESTAMP ---
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
            repliedText = repliedText,
            repliedSender = repliedSender,
            reactionEmoji = reactionEmoji,
            onClick = onClick,
            onLongPress = onLongPress,
            modifier = Modifier
                .graphicsLayer { translationX = swipeOffset.value }
                .pointerInput(Unit) {
                    var hasTriggeredHaptic = false
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (swipeOffset.value > 120f) onSwipeToReply()
                            coroutineScope.launch { swipeOffset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) }
                            hasTriggeredHaptic = false
                        },
                        onDragCancel = {
                            coroutineScope.launch { swipeOffset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) }
                            hasTriggeredHaptic = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (swipeOffset.value + dragAmount).coerceIn(0f, 200f)
                            coroutineScope.launch { swipeOffset.snapTo(newOffset) }

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
    reactionEmoji: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: (Rect, Dp, Dp, Dp, Dp) -> Unit
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

    // 👇 1. Track the coordinates!
    var bubbleBounds by remember { mutableStateOf(Rect.Zero) }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.TopEnd else Alignment.TopStart
    ) {
        val maxBubbleWidth = maxWidth * 0.80f

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {

            Box {
                val innerBoxScope = this

                Column(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        // 👇 2. Grab the bounds right here, before the clip!
                        .onGloballyPositioned { coordinates ->
                            bubbleBounds = coordinates.boundsInRoot()
                        }
                        .clip(RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart))
                        .background(animatedBackgroundColor)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onClick() },
                                // 👇 3. Pass the bounds and the corner radii upward!
                                onLongPress = {
                                    onLongPress(bubbleBounds, topStart, topEnd, bottomStart, bottomEnd)
                                }
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
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
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                Text(
                                    text = repliedSender,
                                    color = if (isMe) Color.White else AppColors.AccentOrange,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = repliedText,
                                    color = if (isMe) Color.White.copy(alpha = 0.85f) else AppColors.TextPrimary.copy(
                                        alpha = 0.85f
                                    ),
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

                    // 👇 THE REACTION BADGE
                    if (reactionEmoji != null) {
                        Box(
                            // 👇 2. Explicitly tell the compiler to use the inner scope!
                            modifier = innerBoxScope.run { Modifier.matchParentSize() },
                            contentAlignment = if (isMe) Alignment.BottomStart else Alignment.BottomEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = if (isMe) (-8).dp else 8.dp,
                                        y = 12.dp
                                    )
                                    .clip(CircleShape)
                                    .background(AppColors.Background)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AppColors.SurfaceDark)
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(text = reactionEmoji, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            // Add extra spacing at the bottom so the badge doesn't overlap the next message
            if (reactionEmoji != null) {
                Spacer(modifier = Modifier.height(12.dp))
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