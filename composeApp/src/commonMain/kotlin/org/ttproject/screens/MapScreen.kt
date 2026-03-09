package org.ttproject.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.stringResource
import org.ttproject.AppColors
import org.ttproject.shared.resources.free
import org.ttproject.shared.resources.indoor
import org.ttproject.shared.resources.nearby_clubs
import org.ttproject.shared.resources.outdoor
import org.ttproject.shared.resources.Res as SharedRes

data class TTClub(
    val id: String, val name: String, val distance: String, val tables: Int,
    val rating: Double, val lat: Double, val lng: Double, val tags: List<String>
)

@Composable
expect fun NativeMap(
    modifier: Modifier, locations: List<TTClub>, selectedClub: TTClub?, userLocationTrigger: Int, onMarkerClick: (TTClub) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val sheetBg = AppColors.Background
    val cardBg = AppColors.SurfaceDark
    val brandOrange = AppColors.AccentOrange

    val sampleClubs = listOf(
        TTClub("1", "Corvin Club", "200m away", 8, 4.8, 47.485, 19.071, listOf("AC", "Showers", "+1")),
        TTClub("2", "Budapest TT Center", "1.2km away", 12, 4.5, 47.497, 19.040, listOf("Coaching", "Shop")),
        TTClub("3", "Margit Island Courts", "3.1km away", 4, 4.0, 47.528, 19.046, listOf("Free", "Public Outdoor"))
    )

    val scaffoldState = rememberBottomSheetScaffoldState()
    var selectedClub by remember { mutableStateOf<TTClub?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var userLocationTrigger by remember { mutableStateOf(0) }

    val handleClubSelection: (TTClub) -> Unit = { clickedClub ->
        coroutineScope.launch {
            if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) {
                scaffoldState.bottomSheetState.partialExpand()
            }
            selectedClub = clickedClub
        }
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 👇 2. Grab the max height of the current screen (works on iOS & Android!)
        val screenHeight = this.maxHeight
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContainerColor = sheetBg,
            sheetPeekHeight = 90.dp,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetContent = {
                Box(modifier = Modifier.heightIn(max = screenHeight - 500.dp)) {
                    NearbyClubsList(
                        clubs = sampleClubs,
                        cardBg = cardBg,
                        brandOrange = brandOrange,
                        onClubClick = handleClubSelection
                    )
                }
            },
            content = { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {

                    NativeMap(
                        modifier = Modifier.fillMaxSize(),
                        locations = sampleClubs,
                        selectedClub = selectedClub,
                        userLocationTrigger = userLocationTrigger,
                        onMarkerClick = handleClubSelection
                    )

                    // FLOATING FILTER CHIPS (Top)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            // 1. Dynamically clear the status bar/notch!
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                            // 2. Add just a tiny bit of breathing room
                            .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(stringResource(SharedRes.string.indoor), false, cardBg)
                        FilterChip(stringResource(SharedRes.string.outdoor), true, brandOrange)
                        FilterChip(stringResource(SharedRes.string.free), false, cardBg)
                    }

                    // 3. THE CUSTOM "MY LOCATION" BUTTON
                    // 3. THE FLOATING ACTION BUTTONS
                    AnimatedVisibility(
                        visible = selectedClub == null, // Hide both when a club is selected
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 16.dp)
                            .graphicsLayer {
                                val sheetY = try {
                                    scaffoldState.bottomSheetState.requireOffset()
                                } catch (e: Exception) {
                                    10000f
                                }
                                translationY = sheetY - size.height - 16.dp.toPx()
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp) // Tighter gap for smaller buttons
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

                    // FLOATING SELECTED CLUB CARD (Bottom, above the peek sheet)
                    AnimatedContent(
                        targetState = selectedClub,
                        transitionSpec = {
                            if (targetState != null && initialState == null) {
                                // 1. OPENING: Slide up with a bouncy spring
                                (slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn())
                                    .togetherWith(fadeOut())
                                    .using(SizeTransform(clip = false))
                            } else if (targetState == null && initialState != null) {
                                // 2. CLOSING: Slide smoothly down and fade out
                                fadeIn().togetherWith(
                                    slideOutVertically(
                                        targetOffsetY = { it },
                                        animationSpec = tween(250)
                                    ) + fadeOut(tween(250))
                                ).using(SizeTransform(clip = false))
                            } else {
                                // 3. SWAPPING (Club A -> Club B): Smooth crossfade
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
                            // CRITICAL: An empty box gives the card something to visually
                            // collapse into during the closing animation!
                            Box(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun FloatingClubCard(club: TTClub, cardBg: Color, brandOrange: Color, onClose: () -> Unit) {
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFF1E3A4C), shape = RoundedCornerShape(6.dp)) {
                    Text("★ ${club.rating}", color = Color(0xFF4AC4F3), modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { /* Navigate */ },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = brandOrange),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Navigate", color = AppColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, bgColor: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = bgColor, contentColor = AppColors.TextPrimary) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
    }
}

// 4. Update the List to accept onClubClick and pass it down
@Composable
fun NearbyClubsList(clubs: List<TTClub>, cardBg: Color, brandOrange: Color, onClubClick: (TTClub) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(stringResource(SharedRes.string.nearby_clubs), color = AppColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 8.dp))
        }
        items(clubs) { club ->
            // Pass the specific club to the click handler
            ClubCard(club, cardBg, brandOrange, onClick = { onClubClick(club) })
        }
    }
}

// 5. Update the Card to accept onClick and make the Surface clickable
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
