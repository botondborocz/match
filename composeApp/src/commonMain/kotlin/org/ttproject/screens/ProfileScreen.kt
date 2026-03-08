package org.ttproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.koin.compose.viewmodel.koinViewModel
import org.ttproject.AppColors
import org.ttproject.data.UserProfile
import org.ttproject.viewmodel.ProfileState
import org.ttproject.viewmodel.ProfileViewModel

private val DarkBackground = AppColors.Background
private val TextGray = AppColors.TextGray

@Composable
fun ProfileScreen(
    currentLanguage: String = "en",
    onLogoutClick: () -> Unit = {},
    onChangeLanguage: (String) -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
//    var currentLanguage: String by remember { mutableStateOf(currentLanguage) }

    LaunchedEffect(Unit) {
        if (viewModel.uiState.value !is ProfileState.Success) {
            viewModel.fetchUserProfile()
        }
    }

    when (uiState) {
        is ProfileState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.AccentOrange)
            }
        }

        is ProfileState.Error -> {
            val error = (uiState as ProfileState.Error).message
            Text(text = error, color = Color.Red)
            // Add a retry button here
        }

        is ProfileState.Success -> {
            val userData = uiState as ProfileState.Success
            var animateTrigger by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                animateTrigger = true
            }
            println("asd")
            println(userData)
            LaunchedEffect(userData.language) {
                // Only trigger the change if the database language is different from the current UI language
                if (userData.language != null && userData.language != currentLanguage) {
                    onChangeLanguage(userData.language!!)
                }
            }

            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkBackground) // Use AppColors.Background if available
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = animateTrigger,
                    enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 50 }
                ) {
                    Column {
                        ProfileHeader(userData.name)
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

//                StatsGrid()
//
//                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = animateTrigger,
                    enter = fadeIn(tween(400, delayMillis = 150)) + slideInVertically(tween(400, delayMillis = 150)) { 50 }
                ) {
                    Column {
                        GearSection()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                AnimatedVisibility(
                    visible = animateTrigger,
                    enter = fadeIn(tween(400, delayMillis = 300)) + slideInVertically(tween(400, delayMillis = 300)) { 50 }
                ) {
                    Column {
                        SettingsAndLogout(
                            currentLanguage = currentLanguage,
                            onLogoutClick = {
                                viewModel.clearProfile()
                                onLogoutClick()
                            },
                            onChangeLanguage = onChangeLanguage,
                            viewModel = viewModel
                        )
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

}

@Composable
private fun ProfileHeader(
    username: String
) {
    val avatarGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF4B4B), Color(0xFF9C27B0))
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(AppColors.SurfaceDark)
                .border(4.dp, avatarGradient, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "JD",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = username,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Badges
        Row(verticalAlignment = Alignment.CenterVertically) {
            // ELO Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "ELO 1380",
                    color = Color(0xFFFF4B4B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Member since 2023",
                color = TextGray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun StatsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                iconTint = Color(0xFFFF4B4B),
                value = "147",
                label = "Matches Played"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ShowChart,
                iconTint = Color(0xFF00E676),
                value = "64%",
                label = "Win Rate"
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.TrendingUp,
                iconTint = Color(0xFF00D2FF),
                value = "5W",
                label = "Current Streak"
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WorkspacePremium,
                iconTint = Color(0xFF9C27B0),
                value = "#234",
                label = "Global Rank"
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDark)
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GearSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Build, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("My Gear", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        GearItem(
            label = "BLADE",
            value = "Butterfly Viscaria",
            iconContent = { Text("🏓", fontSize = 16.sp) } // Using an emoji/text block as a placeholder for paddle
        )
        Spacer(modifier = Modifier.height(12.dp))
        GearItem(
            label = "FOREHAND",
            value = "Tenergy 05",
            iconContent = { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFFF4B4B))) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        GearItem(
            label = "BACKHAND",
            value = "Dignics 09C",
            iconContent = { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color.Black)) }
        )
    }
}

@Composable
private fun GearItem(
    label: String,
    value: String,
    iconContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDark)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(text = label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingsAndLogout(
    currentLanguage: String,
    onLogoutClick: () -> Unit,
    onChangeLanguage: (String) -> Unit,
    viewModel: ProfileViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LanguageSelector(currentLanguage, onChangeLanguage, viewModel)

        Spacer(modifier = Modifier.height(12.dp))

        // Settings Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppColors.SurfaceDark)
                .clickable { /* TODO: Navigate to settings */ }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Account Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextGray)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logout Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFF4B4B).copy(alpha = 0.1f))
                .border(1.dp, Color(0xFFFF4B4B).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clickable { onLogoutClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = Color(0xFFFF4B4B), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", color = Color(0xFFFF4B4B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LanguageSelector(
    currentLanguage: String,
    onChangeLanguage: (String) -> Unit,
    viewModel: ProfileViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    // Smoothly rotates the arrow up and down
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val displayLanguage = when (currentLanguage) {
        "hu" -> "Magyar"
        "en" -> "English"
        else -> "English"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AppColors.SurfaceDark)
            .animateContentSize() // 👈 This makes the expansion buttery smooth!
    ) {
        // --- HEADER ROW (Always Visible) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Language, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Language", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))

            Text(displayLanguage, color = TextGray, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.rotate(rotation) // Applies the rotation animation
            )
        }

        // --- EXPANDED OPTIONS ---
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .padding(bottom = 8.dp) // Slight padding at the bottom of the card
            ) {
                // Subtle divider line
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Option: English
                LanguageOptionRow(
                    text = "English",
                    isSelected = currentLanguage == "en",
                    onClick = {
                        viewModel.changeLanguage("en")
                        onChangeLanguage("en")
                        viewModel.fetchUserProfile()
                        expanded = false
                    }
                )

                // Option: Magyar
                LanguageOptionRow(
                    text = "Magyar",
                    isSelected = currentLanguage == "hu",
                    onClick = {
                        viewModel.changeLanguage("hu")
                        onChangeLanguage("hu")
                        viewModel.fetchUserProfile()
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    val textColor = if (isSelected) AppColors.AccentOrange else Color.White
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 56.dp, vertical = 12.dp), // Indented to perfectly align with the text above
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = textColor, fontSize = 15.sp, fontWeight = fontWeight)
        Spacer(modifier = Modifier.weight(1f))

        // Add a checkmark if it is the currently selected language
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = AppColors.AccentOrange, modifier = Modifier.size(18.dp))
        }
    }
}