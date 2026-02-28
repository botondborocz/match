package org.ttproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TTClub(
    val id: String,
    val name: String,
    val distance: String,
    val tables: Int,
    val rating: Double,
    val lat: Double,
    val lng: Double,
    val tags: List<String>
)

@Composable
expect fun NativeMap(
    modifier: Modifier,
    locations: List<TTClub>,
    onMarkerClick: (TTClub) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val darkBg = Color(0xFF1A1F2B)
    val sheetBg = Color(0xFF161A23)
    val cardBg = Color(0xFF252A36)
    val brandOrange = Color(0xFFFF7B42)

    val sampleClubs = listOf(
        TTClub("1", "Corvin Club", "200m away", 8, 4.8, 47.485, 19.071, listOf("AC", "Showers", "+1")),
        TTClub("2", "Budapest TT Center", "1.2km away", 12, 4.5, 47.497, 19.040, listOf("Coaching", "Shop")),
        TTClub("3", "Pest County TT Hall", "2.5km away", 16, 4.2, 47.510, 19.080, listOf("Tournament", "Parking")),
        TTClub("4", "Margit Island Courts", "3.1km away", 4, 4.0, 47.528, 19.046, listOf("Free", "Public Outdoor"))
    )

    val scaffoldState = rememberBottomSheetScaffoldState()
    var selectedClub by remember { mutableStateOf<TTClub?>(null) }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = sheetBg,
        sheetPeekHeight = 240.dp, // How much of the sheet peeks up by default
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetContent = {
            // BOTTOM SHEET CONTENT
            Column(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(horizontal = 16.dp)
            ) {
                if (selectedClub != null) {
                    ClubDetailView(
                        club = selectedClub!!,
                        brandOrange = brandOrange,
                        onBackClick = { selectedClub = null } // Go back to list
                    )
                } else {
                    NearbyClubsList(clubs = sampleClubs, cardBg = cardBg, brandOrange = brandOrange)
                }
            }
        },
        content = { innerPadding ->
            // FULL SCREEN BACKGROUND (THE MAP)
            Box(modifier = Modifier.fillMaxSize()) {

                NativeMap(
                    // We let the map fill the whole screen behind the bottom sheet
                    modifier = Modifier.fillMaxSize(),
                    locations = sampleClubs,
                    onMarkerClick = { clickedClub ->
                        selectedClub = clickedClub
                    }
                )

                // FLOATING FILTER CHIPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        // Push down to avoid phone status bar (notch/island)
                        .padding(top = 56.dp, start = 16.dp, end = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip("Indoor", false, cardBg)
                    FilterChip("Outdoor", true, brandOrange)
                    FilterChip("Free", false, cardBg)
                    FilterChip("Open Now", false, cardBg)
                }
            }
        }
    )
}

// --- UI Components ---

@Composable
fun FilterChip(text: String, isSelected: Boolean, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        contentColor = Color.White
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 14.sp)
    }
}

@Composable
fun NearbyClubsList(clubs: List<TTClub>, cardBg: Color, brandOrange: Color) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Nearby Clubs", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
        }
        items(clubs) { club ->
            ClubCard(club, cardBg, brandOrange)
        }
    }
}

@Composable
fun ClubCard(club: TTClub, cardBg: Color, brandOrange: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = cardBg, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(50.dp).background(Color(0xFF333947), RoundedCornerShape(8.dp)))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(club.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(brandOrange, RoundedCornerShape(50)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${club.distance} • ${club.tables} Tables", color = Color.Gray, fontSize = 14.sp)
                }
            }
            Surface(color = Color(0xFF1E3A4C), shape = RoundedCornerShape(4.dp)) {
                Text("★ ${club.rating}", color = Color(0xFF4AC4F3), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClubDetailView(club: TTClub, brandOrange: Color, onBackClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(club.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Text("${club.distance} • ${club.tables} Tables Available", color = Color.Gray, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            club.tags.forEach { tag ->
                Surface(color = Color(0xFF252A36), shape = RoundedCornerShape(8.dp)) {
                    Text(tag, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { /* Navigate logic */ },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = brandOrange)
        ) {
            Text("Navigate", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}