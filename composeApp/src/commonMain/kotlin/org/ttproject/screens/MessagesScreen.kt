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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Reply
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import org.ttproject.data.ReactionDto
import kotlin.math.abs

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
    val bottomEnd: Dp,
    val initialTouch: Offset,
    val reactionBounds: Rect?
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
    var replyingToMessageId by remember { mutableStateOf<String?>(null) }
    val replyingToMessage = remember(replyingToMessageId, messages) {
        messages.find { it.id == replyingToMessageId }
    }

    // 👇 1. THEME STATE
    val defaultBg = AppColors.Background
    val defaultSurface = AppColors.SurfaceDark
    val defaultOrange = AppColors.AccentOrange

    val chatThemes = org.ttproject.ChatThemeManager.themes

    var currentTheme by remember { mutableStateOf(chatThemes[0]) }
    var isThemeSheetOpen by remember { mutableStateOf(false) }
    val themeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedReactionMessageId by remember { mutableStateOf<String?>(null) }
    val imeInsets = if (isIosPlatform()) WindowInsets.ime else WindowInsets.ime
    val bottomNavInset = remember(bottomNavPadding) { WindowInsets(bottom = bottomNavPadding + 10.dp) }
    val focusManager = LocalFocusManager.current
    val tokenStorage: TokenStorage = koinInject()

    val inputFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(replyingToMessageId) {
        if (replyingToMessageId != null) {
            inputFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    ClearChatNotificationEffect(chatId = chatId)
    LaunchedEffect(chatId) { viewModel.markMessagesAsRead() }

    var previousMessageCount by remember { mutableStateOf(messages.size) }
    LaunchedEffect(messages.size) {
        if (messages.size > previousMessageCount) {
            listState.animateScrollToItem(0)
        }
        previousMessageCount = messages.size
    }

    val emojis = remember { listOf("❤️", "😂", "😮", "😢", "🏓", "👍") }
    var reactionMenuData by remember { mutableStateOf<ReactionMenuData?>(null) }
    var reactionSheetMessageId by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var activeReactionDragPosition by remember { mutableStateOf<Offset?>(null) }
    var hoveredReactionIndex by remember { mutableStateOf(-1) }

    val haptic = LocalHapticFeedback.current
    var previousHoveredIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(hoveredReactionIndex) {
        if (hoveredReactionIndex != -1 && hoveredReactionIndex != previousHoveredIndex) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        previousHoveredIndex = hoveredReactionIndex
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 👇 2. Apply the beautiful theme gradient to the background!
            .background(currentTheme.backgroundBrush)
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
                                    model = otherUserImageUrl, contentDescription = "Profile picture",
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(getInitials(otherUsername), color = AppColors.AccentOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(otherUsername, color = AppColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(if (isIosPlatform()) Icons.Filled.ArrowBackIosNew else Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppColors.TextPrimary)
                    }
                },
                // 👇 3. ADD THE THEME PALETTE ICON HERE
                actions = {
                    IconButton(onClick = { isThemeSheetOpen = true }) {
                        Icon(Icons.Default.Palette, contentDescription = "Change Theme", tint = AppColors.TextPrimary)
                    }
                },
                // 👇 4. Make it transparent so the gradient flows behind it!
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            HorizontalDivider(color = AppColors.TextGray.copy(alpha = 0.2f))

            val knownMessageIds = remember { mutableSetOf<String>() }
            var isInitialBatchProcessed by remember { mutableStateOf(false) }

            LaunchedEffect(messages) {
                knownMessageIds.addAll(messages.map { it.id })
                isInitialBatchProcessed = true
            }

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

                    val playAnimation = remember(msg.id) {
                        if (!isInitialBatchProcessed) false else !knownMessageIds.contains(msg.id) && index < 5
                    }

                    val repliedMessage = msg.replyToMessageId?.let { replyId -> messages.find { it.id == replyId } }
                    val repliedText = repliedMessage?.content
                    val repliedSender = if (repliedMessage?.senderId == currentUserId) "You" else otherUsername

                    Box(modifier = Modifier.zIndex(displayMessages.size - index.toFloat())) {
                        AnimatedMessageBubble(
                            text = msg.content,
                            isMe = isMe,
                            time = msg.createdAt,
                            playAnimation = playAnimation,
                            showTimeHeader = showTimeHeader,
                            isOlderSame = visuallyConnectToOlder,
                            isNewerSame = visuallyConnectToNewer,
                            isSelected = isSelected,
                            repliedText = repliedText,
                            repliedSender = repliedSender,
                            reactions = msg.reactions,
                            myBubbleColor = currentTheme.myBubbleColor,       // 👈 Pass Theme Color
                            otherBubbleColor = currentTheme.otherBubbleColor, // 👈 Pass Theme Color
                            onClick = { selectedMessageId = if (isSelected) null else msg.id },
                            onReactionClick = { reactionSheetMessageId = msg.id },
                            onLongPress = { bounds, topStart, topEnd, bottomStart, bottomEnd, initialTouch, reactionBounds ->
                                reactionMenuData = ReactionMenuData(msg.id, isMe, bounds, topStart, topEnd, bottomStart, bottomEnd, initialTouch, reactionBounds)
                            },
                            onLongPressDrag = { globalPos -> activeReactionDragPosition = globalPos },
                            onLongPressEnd = { hasDragged ->
                                if (hasDragged) {
                                    if (reactionMenuData != null && hoveredReactionIndex != -1) {
                                        viewModel.sendReaction(reactionMenuData!!.messageId, emojis[hoveredReactionIndex])
                                        reactionMenuData = null
                                    }
                                    activeReactionDragPosition = null
                                    hoveredReactionIndex = -1
                                } else {
                                    activeReactionDragPosition = null
                                }
                            },
                            onSwipeToReply = { replyingToMessageId = msg.id }
                        )
                    }
                }
            }

            // --- THE KEYBOARD-AWARE INPUT AREA ---
            // 👇 THE FIX: Removed the .background(Color.Black.copy(alpha = 0.4f)) so it matches the theme
            Column(modifier = Modifier.fillMaxWidth()) {
                AnimatedVisibility(
                    visible = replyingToMessage != null,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (replyingToMessage != null) {
                        ReplyPreview(
                            messageContent = replyingToMessage.content,
                            themeColor = currentTheme.myBubbleColor, // 👈 Pass the active theme color!
                            onCancel = { replyingToMessageId = null }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 👇 1. Dynamically adjust the inset shadow!
                    val isDarkMode = org.ttproject.isDark // (Or isSystemInDarkTheme() depending on your imports)
                    val inputBgColor = if (isDarkMode) {
                        Color.Black.copy(alpha = 0.25f) // Deep shadow for Dark Mode
                    } else {
                        Color.Black.copy(alpha = 0.05f) // Extremely subtle, clean shadow for Light Mode
                    }

                    BasicTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(inputFocusRequester)
                            .clip(RoundedCornerShape(24.dp))
                            // 👇 2. Apply the dynamic color
                            .background(inputBgColor)
                            .border(
                                width = 1.dp,
                                color = if (messageText.isNotBlank()) currentTheme.myBubbleColor else AppColors.TextGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        textStyle = TextStyle(
                            color = AppColors.TextPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(currentTheme.myBubbleColor),
                        maxLines = 4,
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (messageText.isEmpty()) {
                                    Text("Type a message...", color = AppColors.TextGray, fontSize = 15.sp)
                                }
                                innerTextField()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (messageText.isNotBlank()) currentTheme.myBubbleColor else AppColors.SurfaceDark.copy(alpha = 0.5f))
                            .clickable(enabled = messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText, replyingToMessageId)
                                messageText = ""
                                replyingToMessageId = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) Color.White else AppColors.TextGray,
                            modifier = Modifier.size(18.dp).offset(x = 2.dp)
                        )
                    }
                }
            }
        }

        // 👇 6. THEME SELECTION BOTTOM SHEET
        if (isThemeSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isThemeSheetOpen = false },
                sheetState = themeSheetState,
                containerColor = AppColors.Background
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    Text(
                        "Chat Theme",
                        color = AppColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    HorizontalDivider(color = AppColors.TextGray.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(chatThemes) { theme ->
                            val isSelected = currentTheme.name == theme.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) AppColors.SurfaceDark else Color.Transparent)
                                    .clickable {
                                        currentTheme = theme
                                        coroutineScope.launch { themeSheetState.hide(); isThemeSheetOpen = false }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Theme preview circle
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(theme.backgroundBrush)
                                        .border(2.dp, if (isSelected) theme.myBubbleColor else Color.Transparent, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = theme.name,
                                    color = AppColors.TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = theme.myBubbleColor)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 👇 3. THE REACTION BOTTOM SHEET
        if (reactionSheetMessageId != null) {
            ModalBottomSheet(
                onDismissRequest = { reactionSheetMessageId = null },
                sheetState = sheetState,
                containerColor = AppColors.Background
            ) {
                // Find the specific message so we know what reactions to show
                val targetMessage = messages.find { it.id == reactionSheetMessageId }

                // 👇 Check if the list is not empty!
                if (targetMessage != null && targetMessage.reactions.isNotEmpty()) {

                    val currentUserId = tokenStorage.getUserId() ?: ""

                    // 👇 Map the real server data to your UI data class!
                    val realReactionsList = targetMessage.reactions.map { dto ->
                        val isMyReaction = dto.userId == currentUserId

                        ReactionDetail(
                            userId = dto.userId,
                            username = if (isMyReaction) "You" else otherUsername,
                            profileImageUrl = if (isMyReaction) null else otherUserImageUrl,
                            emoji = dto.emoji,
                            isMe = isMyReaction
                        )
                    }

                    ReactionsBottomSheet(
                        reactions = realReactionsList, // 👈 Pass the real list!
                        onRemoveReaction = { reaction ->
                            // 1. Tell ViewModel to delete it
                            viewModel.removeReaction(targetMessage.id)

                            // 2. Smoothly hide the sheet
                            coroutineScope.launch {
                                sheetState.hide()
                                reactionSheetMessageId = null
                            }
                        }
                    )
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
                            // 👇 2. NEW: The Reaction Badge Cutout
                            state.reactionBounds?.let { rBounds ->
                                val localReactionBounds = rBounds.translate(-overlayBounds)
                                val badgeRadius = with(density) { 14.dp.toPx() } // Standard 14.dp you used for the badge

                                addRoundRect(
                                    RoundRect(
                                        rect = localReactionBounds,
                                        cornerRadius = CornerRadius(badgeRadius)
                                    )
                                )
                            }
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

                    // 3. Dynamic Hover Calculation!
                    LaunchedEffect(activeReactionDragPosition, menuX, menuY) {
                        if (activeReactionDragPosition != null && reactionMenuData != null) {
                            val dragPos = activeReactionDragPosition!!
                            val initialPos = reactionMenuData!!.initialTouch

                            val isSpaceAbove = localBounds.top > menuHeightPx + 50f
                            val dragDistanceY = dragPos.y - initialPos.y
                            val swipeThreshold = with(density) { 10.dp.toPx() } // ~30 pixels

                            // Require them to drag 20dp towards the menu before activating
                            val hasSwipedTowardsMenu = if (isSpaceAbove) {
                                dragDistanceY < -swipeThreshold // Swiped UP
                            } else {
                                dragDistanceY > swipeThreshold  // Swiped DOWN
                            }

                            if (hasSwipedTowardsMenu) {
                                val localX = dragPos.x - menuX
                                if (localX >= -50f && localX <= menuWidthPx + 50f) {
                                    val itemWidth = menuWidthPx / emojis.size
                                    val clampedX = localX.coerceIn(0f, menuWidthPx - 1f)
                                    hoveredReactionIndex = (clampedX / itemWidth).toInt()
                                } else {
                                    hoveredReactionIndex = -1
                                }
                            } else {
                                hoveredReactionIndex = -1 // Finger hasn't moved enough yet
                            }
                        }
                    }

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
//                            .clip(RoundedCornerShape(32.dp))
//                            .background(AppColors.SurfaceDark)
                            // 4. Fallback for manual taps (if user just lifted finger without dragging)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    down.consume()
                                    val itemWidth = menuWidthPx / emojis.size
                                    hoveredReactionIndex = (down.position.x / itemWidth).toInt().coerceIn(0, emojis.size - 1)

                                    var isTracking = true
                                    while (isTracking) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()

                                        if (change.pressed) {
                                            val localY = change.position.y
                                            if (localY > -100f && localY < menuHeightPx + 100f) {
                                                hoveredReactionIndex = (change.position.x / itemWidth).toInt().coerceIn(0, emojis.size - 1)
                                            } else {
                                                hoveredReactionIndex = -1
                                            }
                                        } else {
                                            isTracking = false
                                            if (hoveredReactionIndex != -1 && hoveredReactionIndex in emojis.indices) {
                                                // 1. Emoji selected via tap/short drag! Send and close.
                                                viewModel.sendReaction(state.messageId, emojis[hoveredReactionIndex])
                                                reactionMenuData = null
                                            }
                                            hoveredReactionIndex = -1
                                        }
                                    }
                                }
                            }
                    ) {
                        // 👇 2. THE BACKGROUND LAYER
                        // This stays perfectly rounded and sits behind the emojis.
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppColors.SurfaceDark, RoundedCornerShape(32.dp))
                        )
                        // 👇 3. THE FOREGROUND LAYER (Emojis)
                        // Because the parent isn't clipped, these can grow over the edges!
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            emojis.forEachIndexed { index, emoji ->
                                val scale by animateFloatAsState(
                                    targetValue = if (hoveredReactionIndex == index) 1.6f else 1f,
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
//                                        translationY = if (hoveredReactionIndex == index) -15f else 0f
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
    reactions: List<ReactionDto>,
    myBubbleColor: Color,
    otherBubbleColor: Color,
    onClick: () -> Unit,
    onReactionClick: () -> Unit,
    onLongPress: (Rect, Dp, Dp, Dp, Dp, Offset, Rect?) -> Unit,
    onLongPressDrag: (Offset) -> Unit,
    onLongPressEnd: (Boolean) -> Unit,
    onSwipeToReply: () -> Unit
) {
    // 👇 2. Remove the 'hasAnimated' rememberSaveable entirely.
    // Instead, strictly obey the parent's command using Animatable.
    val alphaAnim = remember { androidx.compose.animation.core.Animatable(if (playAnimation) 0.01f else 1f) }
    val offsetAnim = remember { androidx.compose.animation.core.Animatable(if (playAnimation) 100f else 0f) }

    LaunchedEffect(playAnimation) {
        if (playAnimation) {
            launch { alphaAnim.animateTo(1f, tween(250)) }
            launch { offsetAnim.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) }
        } else {
            // Instantly snap to visible if scrolling history
            launch { alphaAnim.snapTo(1f) }
            launch { offsetAnim.snapTo(0f) }
        }
    }

    val swipeOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Column(
        // 👇 THE FIX: Bypass the graphicsLayer cache bug by using physical layout modifiers!
        modifier = Modifier
            // 👇 1. Move BOTH the offset and alpha into the GPU layer!
            // Do NOT use Modifier.offset() here.
            .graphicsLayer {
                // 👇 3. Use the .value of the explicit animations
                translationX = if (isMe) (offsetAnim.value / 2) else (-offsetAnim.value / 2)
                translationY = offsetAnim.value
                this.alpha = alphaAnim.value
            }
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

        // 👇 1. Wrap the ChatBubble in a Box to layer the Icon behind it
        Box(
            modifier = Modifier.fillMaxWidth(),
            // Align to the right for "Me", left for "Them"
            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {

            // 👇 2. THE REPLY ICON (Reveals dynamically based on swipe distance)
            val iconAlpha = (abs(swipeOffset.value) / 120f).coerceIn(0f, 1f)

            if (iconAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .padding(
                            start = if (isMe) 16.dp else 0.dp,
                            end = if (isMe) 0.dp else 16.dp
                        )
                        .graphicsLayer {
                            this.alpha = iconAlpha
                            // Add a subtle pop-in scaling effect
                            val scale = 0.5f + (0.5f * iconAlpha)
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AppColors.SurfaceDark), // Matches the reaction menu style
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Reply,
                            contentDescription = "Reply",
                            tint = AppColors.TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 👇 3. THE CHAT BUBBLE
            ChatBubble(
                text = text,
                isMe = isMe,
                isOlderSame = isOlderSame,
                isNewerSame = isNewerSame,
                isSelected = isSelected,
                repliedText = repliedText,
                repliedSender = repliedSender,
                reactions = reactions,
                myBubbleColor = myBubbleColor,
                otherBubbleColor = otherBubbleColor,
                onClick = onClick,
                onReactionClick = onReactionClick,
                onLongPress = onLongPress,
                onLongPressDrag = onLongPressDrag,
                onLongPressEnd = onLongPressEnd,
                modifier = Modifier
                    .graphicsLayer { translationX = swipeOffset.value }
                    .pointerInput(Unit) {
                        var hasTriggeredHaptic = false
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // 👇 Use absolute value since offset can be negative now
                                if (abs(swipeOffset.value) > 120f) onSwipeToReply()
                                coroutineScope.launch {
                                    swipeOffset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))
                                }
                                hasTriggeredHaptic = false
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    swipeOffset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy))
                                }
                                hasTriggeredHaptic = false
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()

                                // 👇 4. THE DIRECTIONAL LOGIC
                                val newOffset = if (isMe) {
                                    // Me: Swipe Right-to-Left (Negative numbers)
                                    (swipeOffset.value + dragAmount).coerceIn(-200f, 0f)
                                } else {
                                    // Them: Swipe Left-to-Right (Positive numbers)
                                    (swipeOffset.value + dragAmount).coerceIn(0f, 200f)
                                }

                                coroutineScope.launch { swipeOffset.snapTo(newOffset) }

                                val isPastThreshold = abs(newOffset) > 120f
                                if (isPastThreshold && !hasTriggeredHaptic) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hasTriggeredHaptic = true
                                } else if (!isPastThreshold) {
                                    hasTriggeredHaptic = false
                                }
                            }
                        )
                    }
            )
        }
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
    reactions: List<ReactionDto>,
    myBubbleColor: Color,
    otherBubbleColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onReactionClick: () -> Unit,
    onLongPress: (Rect, Dp, Dp, Dp, Dp, Offset, Rect?) -> Unit,
    onLongPressDrag: (Offset) -> Unit,
    onLongPressEnd: (Boolean) -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnLongPressDrag by rememberUpdatedState(onLongPressDrag)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)

    val haptic = LocalHapticFeedback.current

    val topStart = if (!isMe && isOlderSame) 4.dp else 16.dp
    val bottomStart = if (!isMe && isNewerSame) 4.dp else 16.dp
    val topEnd = if (isMe && isOlderSame) 4.dp else 16.dp
    val bottomEnd = if (isMe && isNewerSame) 4.dp else 16.dp

    val currentTopStart by rememberUpdatedState(topStart)
    val currentTopEnd by rememberUpdatedState(topEnd)
    val currentBottomStart by rememberUpdatedState(bottomStart)
    val currentBottomEnd by rememberUpdatedState(bottomEnd)

    val baseColor = if (isMe) myBubbleColor else otherBubbleColor
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
    var reactionBounds by remember { mutableStateOf<Rect?>(null) }
    val bubbleShape = RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.TopEnd else Alignment.TopStart
    ) {
        val maxBubbleWidth = maxWidth * 0.80f

        // 👇 1. A Column to hold the Bubble AND a physical Spacer below it
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {

            // 👇 2. The Box that perfectly wraps ONLY the chat bubble's size
            Box(contentAlignment = Alignment.BottomEnd) {

                // --- THE ACTUAL CHAT BUBBLE ---
                Column(
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .onGloballyPositioned { coordinates ->
                            bubbleBounds = coordinates.boundsInRoot()
                        }
                        .background(
                            color = animatedBackgroundColor,
                            shape = bubbleShape
                        )
                        .clip(bubbleShape)
                        .pointerInput(Unit) {
                            val slop = viewConfiguration.touchSlop

                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var isTap = false
                                var isLongPress = false

                                try {
                                    withTimeout(400L) {
                                        var current = down
                                        while (current.pressed) {
                                            val event = awaitPointerEvent()
                                            current = event.changes.first()
                                            val distance = (current.position - down.position).getDistance()

                                            if (distance > slop) return@withTimeout
                                        }
                                        current.consume()
                                        isTap = true
                                    }
                                } catch (e: androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException) {
                                    isLongPress = true
                                }

                                if (isTap) {
                                    currentOnClick()
                                } else if (isLongPress) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val globalTouch = bubbleBounds.topLeft + down.position
                                    currentOnLongPress(bubbleBounds, currentTopStart, currentTopEnd, currentBottomStart, currentBottomEnd, globalTouch, reactionBounds)
                                    var tracking = true
                                    var hasDragged = false

                                    while (tracking) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()

                                        if (change.pressed) {
                                            val distance = (change.position - down.position).getDistance()
                                            if (distance > slop) hasDragged = true

                                            val globalPos = bubbleBounds.topLeft + change.position
                                            currentOnLongPressDrag(globalPos)
                                            change.consume()
                                        } else {
                                            tracking = false
                                            currentOnLongPressEnd(hasDragged)
                                            change.consume()
                                        }
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    // 👇 THE QUOTE PREVIEW UI
                    if (repliedText != null && repliedSender != null) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.15f))
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    // 👇 Uses Theme Color!
                                    .background(if (isMe) Color.White else myBubbleColor)
                            )
                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    text = repliedSender,
                                    // 👇 Uses Theme Color!
                                    color = if (isMe) Color.White else myBubbleColor,
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

                // --- THE FLOATING REACTION BADGE ---
                if (reactions.isNotEmpty()) {
                    // 👇 1. Group the reactions by emoji!
                    // This creates a Map where the Key is the Emoji (String) and the Value is a List of ReactionDtos.
                    val groupedReactions = reactions.groupBy { it.emoji }

                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(0, 0) {
                                    placeable.placeRelative(
                                        x = -placeable.width + 6.dp.roundToPx(),
                                        y = -placeable.height / 2 + 4.dp.roundToPx()
                                    )
                                }
                            }
                            .zIndex(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.Background) // The cutout border
                                .padding(2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        reactionBounds = coordinates.boundsInRoot()
                                    }
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppColors.SurfaceDark)
                                    .clickable { onReactionClick() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp), // Space between different emojis
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 👇 2. Loop through the grouped map instead of the raw list
                                groupedReactions.forEach { (emoji, reactionList) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp) // Space between emoji and number
                                    ) {
                                        Text(text = emoji, fontSize = 12.sp)

                                        // 👇 3. Only show the number if more than 1 person used this emoji!
                                        if (reactionList.size > 1) {
                                            Text(
                                                text = reactionList.size.toString(),
                                                color = Color.White, // Adjust to AppColors.TextPrimary if needed
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
            } // End of Bubble & Reaction Box

            // 👇 4. We add physical spacing HERE so the hanging reaction doesn't clip into the next message
            if (reactions.isNotEmpty()) { // ✅ NEW: Check the list size!
                Spacer(modifier = Modifier.height(14.dp))
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
    themeColor: Color, // 👈 Accept the theme color
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.2f)) // 👈 Translucent to blend with gradient!
            .border(
                width = 1.dp,
                color = themeColor.copy(alpha = 0.3f), // 👈 Theme matched border
                shape = RoundedCornerShape(8.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(themeColor) // 👈 Theme matched bar
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text("Replying to", color = themeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold) // 👈 Theme matched title
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

// 👇 1. The Data Class
data class ReactionDetail(
    val userId: String,
    val username: String,
    val profileImageUrl: String?,
    val emoji: String,
    val isMe: Boolean
)

// 👇 2. The Bottom Sheet Content
@Composable
fun ReactionsBottomSheet(
    reactions: List<ReactionDetail>,
    onRemoveReaction: (ReactionDetail) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Reactions",
            color = AppColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        HorizontalDivider(color = AppColors.TextGray.copy(alpha = 0.1f))

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(reactions, key = { it.userId }) { reaction ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 👇 If it's the current user, make the row clickable to delete!
                        .clickable(enabled = reaction.isMe) {
                            onRemoveReaction(reaction)
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(AppColors.SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!reaction.profileImageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = reaction.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(getInitials(reaction.username), color = AppColors.AccentOrange, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name & "Tap to remove" hint
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (reaction.isMe) "You" else reaction.username,
                            color = AppColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (reaction.isMe) {
                            Text("Tap to remove", color = AppColors.TextGray, fontSize = 12.sp)
                        }
                    }

                    // The Emoji
                    Text(text = reaction.emoji, fontSize = 24.sp)
                }
            }
        }
    }
}