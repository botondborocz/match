package org.ttproject.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.ttproject.AppColors
import org.ttproject.shared.resources.free
import org.ttproject.shared.resources.indoor
import org.ttproject.shared.resources.nearby_clubs
import org.ttproject.shared.resources.outdoor
import org.ttproject.util.LocalThemeMode
import org.ttproject.util.ThemeMode
import kotlin.math.roundToInt
import org.ttproject.shared.resources.Res as SharedRes
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Velocity
import org.koin.compose.viewmodel.koinViewModel
import org.ttproject.isIosPlatform
import org.ttproject.viewmodel.LocationViewModel
import org.ttproject.viewmodel.LocationsUiState

data class TTClub(
    val id: String, val name: String, val distance: String, val tables: Int,
    val rating: Double, val lat: Double, val lng: Double, val tags: List<String>
)

// 1. Define the states for our custom Bottom Sheet
enum class SheetState {
    Expanded,
    HalfExpanded,
    Collapsed
}

// 👇 1. Add this cross-platform data class
data class MapBounds(
    val north: Double,
    val south: Double,
    val east: Double,
    val west: Double
)

@Composable
expect fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    selectedClub: TTClub?,
    userLocationTrigger: Int,
    bottomPadding: Dp,
    isDark: Boolean,
    onMarkerClick: (TTClub) -> Unit,
    // 👇 2. Add the callback to the signature
    onBoundsChanged: (MapBounds) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MapScreen(
    viewModel: LocationViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchNearbyLocations()
    }

    val clubs = remember(state) {
        when (val s = state) {
            is LocationsUiState.Success -> s.locations.map { loc ->
                TTClub(
                    id = loc.id?.toString() ?: "",
                    name = loc.name,
                    distance = "Calculating...", // You can add distance logic later
                    tables = loc.tableCount,
                    rating = 0.0, // Add rating to DB schema later if needed
                    lat = loc.latitude,
                    lng = loc.longitude,
                    tags = listOf(loc.type.name, if (loc.isFree) "Free" else "Paid")
                )
            }
            else -> emptyList()
        }
    }

    val sheetBg = AppColors.Background
    val cardBg = AppColors.SurfaceDark
    val brandOrange = AppColors.AccentOrange
    val density = LocalDensity.current

    val isDark = when (LocalThemeMode.current) {
        ThemeMode.Dark -> true
        ThemeMode.Light -> false
        ThemeMode.System -> isSystemInDarkTheme()
    }

    val sampleClubs = listOf(
        TTClub("1", "Corvin Club", "200m away", 8, 4.8, 47.485, 19.071, listOf("AC", "Showers", "+1")),
        TTClub("2", "Budapest TT Center", "1.2km away", 12, 4.5, 47.497, 19.040, listOf("Coaching", "Shop")),
        TTClub("3", "Margit Island Courts", "3.1km away", 4, 4.0, 47.528, 19.046, listOf("Free", "Public Outdoor"))
    )

    var selectedClub by remember { mutableStateOf<TTClub?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var userLocationTrigger by remember { mutableStateOf(0) }

    // 👇 1. Add state to hold the current screen edges
    var mapBounds by remember { mutableStateOf<MapBounds?>(null) }

    // 👇 2. Automatically filter the list when mapBounds OR clubs change
    val visibleClubs = remember(clubs, mapBounds) {
        val bounds = mapBounds ?: return@remember clubs // If bounds unknown, show all

        clubs.filter { club ->
            val isInsideLat = club.lat in bounds.south..bounds.north
            // Handle standard longitude check (with fallback for crossing the antimeridian)
            val isInsideLng = if (bounds.west <= bounds.east) {
                club.lng in bounds.west..bounds.east
            } else {
                club.lng >= bounds.west || club.lng <= bounds.east
            }

            isInsideLat && isInsideLng
        }
    }

    var isIndoorSelected by remember { mutableStateOf(false) }
    var isOutdoorSelected by remember { mutableStateOf(true) }
    var isFreeSelected by remember { mutableStateOf(false) }

    // 2. Initialize the AnchoredDraggable state (Compose 1.7+ style)
    val sheetState = remember {
        AnchoredDraggableState(
            initialValue = SheetState.Collapsed,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = spring(),
            decayAnimationSpec = exponentialDecay() // <-- Added this!
        )
    }

    // 🚦 The Gesture Traffic Cop
    val nestedScrollConnection = remember(sheetState) {
        object : NestedScrollConnection {

            // 1. Intercept BEFORE the list scrolls (Swiping UP)
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // If swiping up (negative delta), the sheet gets to move first!
                return if (delta < 0 && source == NestedScrollSource.UserInput) {
                    Offset(0f, sheetState.dispatchRawDelta(delta))
                } else {
                    Offset.Zero
                }
            }

            // 2. Intercept AFTER the list finishes scrolling (Swiping DOWN)
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                // If swiping down (positive delta) and the list couldn't scroll anymore, drag the sheet!
                return if (delta > 0 && source == NestedScrollSource.UserInput) {
                    Offset(0f, sheetState.dispatchRawDelta(delta))
                } else {
                    Offset.Zero
                }
            }

            // 3. Handle Flings (Fast Swipes)
            override suspend fun onPreFling(available: Velocity): Velocity {
                val toFling = available.y
                // If flinging up and the sheet isn't at the top (0f) yet, snap the sheet!
                return if (toFling < 0 && sheetState.offset > 0f) {
                    sheetState.settle(toFling)
                    Velocity(0f, toFling)
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // If flinging down and the list is at the top, snap the sheet!
                sheetState.settle(available.y)
                return Velocity(0f, available.y)
            }
        }
    }

    val handleClubSelection: (TTClub) -> Unit = { clickedClub ->
        coroutineScope.launch {
            if (sheetState.currentValue != SheetState.Collapsed) {
                sheetState.animateTo(SheetState.Collapsed)
            }
            selectedClub = clickedClub
        }
    }

    // 3. Wrap everything in BoxWithConstraints to get screen dimensions
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeight = this.maxHeight
        val layoutHeightPx = constraints.maxHeight.toFloat()
        val peekHeightPx = with(density) { 90.dp.toPx() }
        val topInsetPx = WindowInsets.systemBars.getTop(density).toFloat()
        val expandedTopPx = topInsetPx + with(density) { 12.dp.toPx() }

        // 4. Update anchors dynamically based on screen size
        SideEffect {
            val anchors = DraggableAnchors {
                SheetState.Expanded at expandedTopPx // Top of screen
                SheetState.HalfExpanded at layoutHeightPx * 0.67f // Middle of screen
                SheetState.Collapsed at layoutHeightPx - peekHeightPx // Bottom of screen (leaves 90dp visible)
            }
            sheetState.updateAnchors(anchors)
        }

        // Calculate dynamic map padding based on target state
        val targetBottomPadding = when (sheetState.targetValue) {
            SheetState.Expanded -> screenHeight
            SheetState.HalfExpanded -> screenHeight * 0.33f
            SheetState.Collapsed -> 90.dp
        }

        val mapBottomPadding by animateDpAsState(
            targetValue = targetBottomPadding,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "MapPadding"
        )

        // BACKGROUND MAP
        NativeMap(
            modifier = Modifier.fillMaxSize(),
            locations = clubs,
            selectedClub = selectedClub,
            userLocationTrigger = userLocationTrigger,
            bottomPadding = mapBottomPadding,
            isDark = isDark,
            onMarkerClick = handleClubSelection,
            onBoundsChanged = { newBounds -> mapBounds = newBounds }
        )

        // FLOATING FILTER CHIPS (Top)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                text = stringResource(SharedRes.string.indoor),
                isSelected = isIndoorSelected,
                activeColor = brandOrange,
                inactiveColor = cardBg,
                onClick = { isIndoorSelected = !isIndoorSelected } // Toggles the state!
            )

            FilterChip(
                text = stringResource(SharedRes.string.outdoor),
                isSelected = isOutdoorSelected,
                activeColor = brandOrange,
                inactiveColor = cardBg,
                onClick = { isOutdoorSelected = !isOutdoorSelected }
            )

            FilterChip(
                text = stringResource(SharedRes.string.free),
                isSelected = isFreeSelected,
                activeColor = brandOrange,
                inactiveColor = cardBg,
                onClick = { isFreeSelected = !isFreeSelected }
            )
        }

        // THE FLOATING ACTION BUTTONS
        AnimatedVisibility(
            visible = selectedClub == null && sheetState.targetValue != SheetState.Expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp)
                .graphicsLayer {
                    // 5. Connect FAB position to the custom sheet's Y offset
                    val sheetY = if (sheetState.offset.isNaN()) layoutHeightPx else sheetState.offset
                    translationY = sheetY - size.height - 16.dp.toPx()
                }
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { /* TODO: Add new club action */ },
                    containerColor = cardBg,
                    contentColor = brandOrange,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Club")
                }

                FloatingActionButton(
                    onClick = { userLocationTrigger++ },
                    containerColor = cardBg,
                    contentColor = brandOrange,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center on me")
                }
            }
        }

        // FLOATING SELECTED CLUB CARD
        AnimatedContent(
            targetState = selectedClub,
            transitionSpec = {
                if (targetState != null && initialState == null) {
                    (slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn())
                        .togetherWith(fadeOut())
                        .using(SizeTransform(clip = false))
                } else if (targetState == null && initialState != null) {
                    fadeIn().togetherWith(
                        slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(250)
                        ) + fadeOut(tween(250))
                    ).using(SizeTransform(clip = false))
                } else {
                    fadeIn(tween(300))
                        .togetherWith(fadeOut(tween(300)))
                        .using(SizeTransform(clip = false))
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 104.dp, start = 16.dp, end = 16.dp)
                .zIndex(1f)
                .graphicsLayer(clip = false),
            label = "ClubCardAnimation"
        ) { currentClub ->
            if (currentClub != null) {
                FloatingClubCard(
                    club = currentClub,
                    cardBg = cardBg,
                    brandOrange = brandOrange,
                    onClose = { selectedClub = null }
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth())
            }
        }
        // 6. THE CUSTOM BOTTOM SHEET
        val maxOffsetPx = layoutHeightPx - peekHeightPx

        val sheetOffsetY = if (sheetState.offset.isNaN()) {
            (layoutHeightPx - peekHeightPx).roundToInt()
        } else {
            sheetState.offset.roundToInt().coerceIn(0, maxOffsetPx.roundToInt())
        }

        // Calculate the corner radius: 0.dp when expanding/expanded, 24.dp otherwise
        val cornerRadius by animateDpAsState(
            targetValue = if (sheetState.targetValue == SheetState.Expanded) 0.dp else 24.dp,
            label = "SheetCornerRadius"
        )

        Surface(
            modifier = Modifier
                .zIndex(10f) // Force Compose to put this at the absolute front
                .offset { IntOffset(x = 0, y = sheetOffsetY) }
                .anchoredDraggable(
                    state = sheetState,
                    orientation = Orientation.Vertical
                )
                .nestedScroll(nestedScrollConnection)
                .fillMaxWidth()
                .height(screenHeight),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // Handles corner animation safely
            color = sheetBg, // Replaces .background(sheetBg)
            shadowElevation = 0.dp // This is the magic bullet that forces MIUI to draw it OVER the map!
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Gray.copy(alpha = 0.5f))
                        .align(Alignment.CenterHorizontally)
                )

                NearbyClubsList(
                    clubs = visibleClubs,
                    cardBg = cardBg,
                    brandOrange = brandOrange,
                    onClubClick = handleClubSelection
                )
            }
        }
    }
}

@Composable
fun FloatingClubCard(club: TTClub, cardBg: Color, brandOrange: Color, onClose: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var showMapChoice by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(club.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${club.distance} • ${club.tables} Tables", color = Color.Gray, fontSize = 14.sp)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // --- BOTTOM ROW (Conditional UI) ---
            if (showMapChoice) {
                // THE iOS CHOICE MENU
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Cancel Button
                    TextButton(
                        onClick = { showMapChoice = false },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text("Cancel", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Apple Maps Button (Dark Gray)
                        Button(
                            onClick = {
                                // TODO: Save preference to DataStore/Settings here
                                uriHandler.openUri("https://maps.apple.com/?q=${club.lat},${club.lng}")
                                showMapChoice = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2D34)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Apple", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        // Google Maps Button (Brand Orange)
                        Button(
                            onClick = {
                                // TODO: Save preference to DataStore/Settings here
                                uriHandler.openUri("https://maps.google.com/?q=${club.lat},${club.lng}")
                                showMapChoice = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("Google", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // THE STANDARD MENU (Rating + Navigate)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF1E3A4C), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = "★ ${club.rating}",
                            color = Color(0xFF4AC4F3),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            // 👇 2. iOS checks for preference, Android forces Google!
                            if (isIosPlatform()) {
                                // In the future, check your local storage here first!
                                showMapChoice = true
                            } else {
                                uriHandler.openUri("https://maps.google.com/?q=${club.lat},${club.lng}")
                            }
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Navigate", color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    // Smoothly animate the background color when the state changes
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) activeColor else inactiveColor,
        label = "ChipColorAnimation"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        contentColor = AppColors.TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp)) // Ensures the ripple effect stays inside the rounded corners
            .clickable { onClick() }         // 👇 THE FIX: Makes it tappable!
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 14.sp
        )
    }
}

@Composable
fun NearbyClubsList(clubs: List<TTClub>, cardBg: Color, brandOrange: Color, onClubClick: (TTClub) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(stringResource(SharedRes.string.nearby_clubs), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp))
        }
        items(clubs) { club ->
            ClubCard(club, cardBg, brandOrange, onClick = { onClubClick(club) })
        }
    }
}

@Composable
fun ClubCard(club: TTClub, cardBg: Color, brandOrange: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(Color(0xFF333947), RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, color = AppColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${club.distance} • ${club.tables} Tables", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}