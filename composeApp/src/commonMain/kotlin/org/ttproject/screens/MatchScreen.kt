package org.ttproject.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.ttproject.AppColors
import org.ttproject.data.Player
import org.ttproject.shared.resources.find
import org.ttproject.shared.resources.Res as SharedRes
import org.ttproject.shared.resources.find_your_match
import org.ttproject.shared.resources.find_your_match_unauth_title
import org.ttproject.shared.resources.login
import org.ttproject.shared.resources.register
import org.ttproject.viewmodel.MatchUiState
import org.ttproject.viewmodel.MatchViewModel
import kotlin.math.abs

@Composable
fun MatchScreen(
    viewModel: MatchViewModel = koinInject(),
    onNavigateToLogin: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val uiState by viewModel.uiState.collectAsState()
    val matchedPlayer by viewModel.matchedPlayer.collectAsState()

    val cardGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF3B4CCA), Color(0xFF151C2C))
    )

    // 👇 1. Check if we are in an error state
    val isErrorState = uiState is MatchUiState.Error
    val errorMessage = (uiState as? MatchUiState.Error)?.message ?: ""

    // 👇 2. Make this bulletproof! If the message contains "token" or "401", it's an Unauth error.
    val isUnauth = isErrorState && (
            errorMessage.contains("token", ignoreCase = true) ||
                    errorMessage.contains("401") ||
                    errorMessage.contains("unauthorized", ignoreCase = true)
            )

    // 3. General error is anything else
    val isGeneralError = isErrorState && !isUnauth

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {

        // --- MAIN CONTENT ---
        Column(
            // 👇 2. Blur the background if ANY error occurs!
            modifier = Modifier
                .fillMaxSize()
                .then(if (isErrorState) Modifier.blur(16.dp) else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // --- HEADER ---
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(400)) + slideInVertically(initialOffsetY = { -40 }, animationSpec = tween(400))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AppColors.AccentOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(SharedRes.string.find_your_match),
                            color = AppColors.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CARD STACK AREA ---
            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                val componentWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        // 👇 3. If there is an error, render a dummy card in the background so the blur looks nice!
                        isErrorState -> {
                            MatchCard(
                                player = Player("dummy", "Table Tennis Fan", "Advanced", age = 28, elo = 1500, distanceKm = 5),
                                backgroundBrush = cardGradient,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        uiState is MatchUiState.Loading -> {
                            CircularProgressIndicator(color = AppColors.AccentOrange)
                        }

                        uiState is MatchUiState.Success -> {
                            val players = (uiState as MatchUiState.Success).players
                            val topPlayer = players.firstOrNull()
                            val offsetX = remember(topPlayer?.id) { Animatable(0f) }
                            val offsetY = remember(topPlayer?.id) { Animatable(0f) }
                            val coroutineScope = rememberCoroutineScope()
                            var showContent by remember { mutableStateOf(false) }

                            LaunchedEffect(Unit) {
                                showContent = true
                            }

                            val triggerSwipe = { directionRight: Boolean, screenWidthPx: Float ->
                                coroutineScope.launch {
                                    val targetX = if (directionRight) screenWidthPx * 1.5f else -screenWidthPx * 1.5f
                                    val targetY = 200f
                                    launch { offsetY.animateTo(targetY, tween(300)) }
                                    offsetX.animateTo(targetX, tween(300))

                                    if (topPlayer != null) {
                                        viewModel.onPlayerSwiped(topPlayer, isLiked = directionRight)
                                    }
                                }
                            }

                            Column(modifier = Modifier.align(Alignment.Center)) {
                                AnimatedVisibility(
                                    visible = showContent,
                                    enter = fadeIn(tween(500, delayMillis = 150)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + slideInVertically(initialOffsetY = { 100 }, animationSpec = tween(500, delayMillis = 150))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (players.isEmpty()) {
                                            Text("No more matches nearby!", color = AppColors.TextPrimary)
                                        } else {
                                            if (players.size > 1) {
                                                MatchCard(
                                                    player = players[1],
                                                    backgroundBrush = cardGradient,
                                                    modifier = Modifier.graphicsLayer {
                                                        scaleX = 0.95f
                                                        scaleY = 0.95f
                                                    }
                                                )
                                            }

                                            SwipeableMatchCard(
                                                player = topPlayer!!,
                                                backgroundBrush = cardGradient,
                                                offsetX = offsetX,
                                                offsetY = offsetY,
                                                componentWidthPx = componentWidthPx,
                                                onSwipeComplete = { directionRight ->
                                                    triggerSwipe(directionRight, componentWidthPx)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 32.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = showContent,
                                    enter = fadeIn(tween(300, delayMillis = 350)) + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                                ) {
                                    ActionButtonsRow(
                                        onLike = { triggerSwipe(true, componentWidthPx) },
                                        onPass = { triggerSwipe(false, componentWidthPx) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- UNAUTH OVERLAY ---
        AnimatedVisibility(
            visible = isUnauth,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.95f),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)) // Dim the blurred background
                    .clickable(enabled = false) {}, // Intercept clicks
                contentAlignment = Alignment.Center
            ) {
                // 👇 The elevated dark card from the screenshot
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E2532)) // Dark card background
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Text(
                        // Use your Hungarian stringResource here if preferred!
                        text = stringResource(SharedRes.string.find_your_match_unauth_title),
                        color = AppColors.AccentOrange,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        lineHeight = 30.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(SharedRes.string.find),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // The full-width orange button with dark text
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(AppColors.AccentOrange)
                            .clickable { onNavigateToLogin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(SharedRes.string.login) + " / " + stringResource(SharedRes.string.register),
                            color = Color(0xFF11151E), // Dark text to pop against the orange
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 👇 4. NEW GENERAL ERROR OVERLAY
        AnimatedVisibility(
            visible = isGeneralError,
            enter = fadeIn(tween(300)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF151C2C)) // Dark modal background
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Error",
                        tint = AppColors.ErrorText,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Oops!",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    // Try Again Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(AppColors.AccentOrange)
                            .clickable {
                                // 👇 Call your ViewModel refresh function here!
                                // viewModel.loadMatches()
                            }
                            .padding(horizontal = 32.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRY AGAIN", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- "IT'S A MATCH" OVERLAY ---
        AnimatedVisibility(
            visible = matchedPlayer != null,
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            matchedPlayer?.let { player ->
                MatchCelebrationOverlay(
                    player = player,
                    onKeepSwiping = { viewModel.dismissMatchPopup() },
                    onSendMessage = {
                        viewModel.dismissMatchPopup()
                    }
                )
            }
        }
    }
}

// ... SwipeableMatchCard, MatchCard, MatchCardContent, ActionButtonsRow, MatchCelebrationOverlay remain the same!

@Composable
fun SwipeableMatchCard(
    player: Player,
    backgroundBrush: Brush,
    offsetX: Animatable<Float, *>,
    offsetY: Animatable<Float, *>,
    componentWidthPx: Float,
    onSwipeComplete: (Boolean) -> Unit, // True if right, false if left
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val swipeThresholdPx = componentWidthPx / 3f

    val rotation = (offsetX.value / componentWidthPx) * 30f
    val alpha = 1f - (abs(offsetX.value) / componentWidthPx)

    Box(
        modifier = modifier
            .heightIn(max = 800.dp)
            .fillMaxHeight(0.9f)
            .padding(bottom = 120.dp) // Make room for the buttons floating over it
            .widthIn(max = 400.dp)
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = rotation
                this.alpha = alpha.coerceIn(0f, 1f)
            }
            .pointerInput(player.id) { // Reset pointer tracking per player
                detectDragGestures(
                    onDragEnd = {
                        if (abs(offsetX.value) > swipeThresholdPx) {
                            // Tell the parent to finish the swipe animation
                            onSwipeComplete(offsetX.value > 0)
                        } else {
                            // Snap back to center
                            coroutineScope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                            coroutineScope.launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    }
                )
            }
    ) {
        MatchCardContent(player, backgroundBrush)
    }
}

@Composable
fun MatchCard(
    player: Player,
    backgroundBrush: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(max = 800.dp)
            .fillMaxHeight(0.9f)
            .padding(bottom = 120.dp)
            .widthIn(max = 400.dp)
            .padding(horizontal = 24.dp)
    ) {
        MatchCardContent(player, backgroundBrush)
    }
}

@Composable
fun MatchCardContent(
    player: Player,
    backgroundBrush: Brush
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {

            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)))
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${player.username}, ",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${player.age}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(1.dp, AppColors.AccentOrange, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = AppColors.AccentOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ELO ${player.elo}",
                            color = AppColors.AccentOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${player.distanceKm} km away",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip(player.skillLevel)
                    // You could add more tags to the Player model if desired
                }
            }
        }
    }
}

@Composable
fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ActionButtonsRow modified slightly to accept callbacks
@Composable
fun ActionButtonsRow(onLike: () -> Unit, onPass: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppColors.ButtonBackground)
                .border(2.dp, AppColors.ErrorText.copy(alpha = 0.5f), CircleShape)
                .clickable { onPass() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Pass", tint = AppColors.ErrorText, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.width(24.dp))

//        Box(
//            modifier = Modifier
//                .size(48.dp)
//                .clip(CircleShape)
//                .background(Color(0xFF151C2C))
//                .border(2.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), CircleShape)
//                .clickable { /* Super Like */ },
//            contentAlignment = Alignment.Center
//        ) {
//            Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFF00D2FF), modifier = Modifier.size(24.dp))
//        }
//
//        Spacer(modifier = Modifier.width(24.dp))

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(AppColors.ButtonBackground)
                .border(2.dp, AppColors.SuccessText.copy(alpha = 0.5f), CircleShape)
                .clickable { onLike() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "Like", tint = AppColors.SuccessText, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun MatchCelebrationOverlay(
    player: Player,
    onKeepSwiping: () -> Unit,
    onSendMessage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6151C2C)) // Dark semi-transparent background
            .clickable(enabled = false) {}, // Intercepts clicks so you can't swipe cards underneath
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "IT'S A MATCH!",
                color = AppColors.AccentOrange,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = AppColors.AccentOrange,
                        blurRadius = 20f
                    )
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "You and ${player.username} liked each other.",
                color = AppColors.TextPrimary,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Two overlapping profile pictures (You and Them) would go here!
            Row(horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.Gray).border(3.dp, AppColors.AccentOrange, CircleShape))
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(AppColors.TextPrimary.copy(alpha=0.5f)).border(3.dp, AppColors.AccentOrange, CircleShape))
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Send Message Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppColors.AccentOrange)
                    .clickable { onSendMessage() }
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            ) {
                Text("SEND MESSAGE", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keep Swiping Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, AppColors.TextPrimary, RoundedCornerShape(24.dp))
                    .clickable { onKeepSwiping() }
                    .padding(horizontal = 48.dp, vertical = 16.dp)
            ) {
                Text("KEEP SWIPING", color = AppColors.TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}