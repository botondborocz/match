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
import androidx.compose.material.icons.filled.MyLocation

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
    val sheetBg = Color(0xFF161A23)
    val cardBg = Color(0xFF252A36)
    val brandOrange = Color(0xFFFF7B42)

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

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = sheetBg,
        sheetPeekHeight = 76.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            // BOTTOM SHEET IS NOW ONLY FOR THE LIST
            NearbyClubsList(
                clubs = sampleClubs,
                cardBg = cardBg,
                brandOrange = brandOrange,
                onClubClick = handleClubSelection
            )
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
                    FilterChip("Indoor", false, cardBg)
                    FilterChip("Outdoor", true, brandOrange)
                    FilterChip("Free", false, cardBg)
                }

                // 3. THE CUSTOM "MY LOCATION" BUTTON
                AnimatedVisibility(
                    visible = selectedClub == null, // <-- HIDE IT WHEN A CLUB IS SELECTED!
                    enter = fadeIn() + scaleIn(),   // Pop in smoothly
                    exit = fadeOut() + scaleOut(),  // Shrink away smoothly
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        // Push it 90dp up so it sits right above the bottom sheet!
                        .padding(bottom = 90.dp, end = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = { userLocationTrigger++ },
                        containerColor = cardBg,
                        contentColor = brandOrange
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Center on me")
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
                            ) + fadeIn()).togetherWith(fadeOut())
                        } else if (targetState == null && initialState != null) {
                            // 2. CLOSING: Slide smoothly down and fade out
                            fadeIn().togetherWith(
                                slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(250)
                                ) + fadeOut(tween(250))
                            )
                        } else {
                            // 3. SWAPPING (Club A -> Club B): Smooth crossfade
                            fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp, start = 16.dp, end = 16.dp),
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

@Composable
fun FloatingClubCard(club: TTClub, cardBg: Color, brandOrange: Color, onClose: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(club.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                    Text("Navigate", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, bgColor: Color) {
    Surface(shape = RoundedCornerShape(20.dp), color = bgColor, contentColor = Color.White) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
    }
}

// 4. Update the List to accept onClubClick and pass it down
@Composable
fun NearbyClubsList(clubs: List<TTClub>, cardBg: Color, brandOrange: Color, onClubClick: (TTClub) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Nearby Clubs", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
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
                Text(club.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${club.distance} • ${club.tables} Tables", color = Color.Gray, fontSize = 14.sp)
            }
        }
    }
}
