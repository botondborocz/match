package org.ttproject.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.ttproject.AppColors
import org.ttproject.shared.resources.Res as SharedRes
import org.ttproject.shared.resources.find_your_match
import org.ttproject.shared.resources.new_matches_today

@Composable
fun MatchScreen() {
    // 1. The Trigger State
    var isVisible by remember { mutableStateOf(false) }

    // 2. Fire the animation immediately when the screen enters the composition
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val cardGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3B4CCA),
            Color(0xFF151C2C)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // --- ANIMATED HEADER & BADGE ---
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + slideInVertically(
                initialOffsetY = { -40 }, // Slides down slightly
                animationSpec = tween(400)
            )
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
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF00D2FF).copy(alpha = 0.15f))
                        .border(1.dp, Color(0xFF00D2FF).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(SharedRes.string.new_matches_today),
                        color = Color(0xFF00D2FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ANIMATED MAIN CARD ---
        AnimatedVisibility(
            visible = isVisible,
            modifier = Modifier
                .weight(1f) // Moved the weight modifier here!
                .fillMaxWidth(),
            enter = fadeIn(tween(500, delayMillis = 150)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + slideInVertically(
                initialOffsetY = { 100 },
                animationSpec = tween(500, delayMillis = 150)
            )
        ) {
            // The Box is now inside, safely centering the card
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MatchCard(cardGradient)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- ANIMATED ACTION BUTTONS ---
        AnimatedVisibility(
            visible = isVisible,
            // Delay the buttons so they pop in exactly as the card finishes settling
            enter = fadeIn(tween(300, delayMillis = 350)) + scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        ) {
            ActionButtonsRow()
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MatchCard(backgroundBrush: Brush) {
    // This Box ensures the card never gets wider than 400.dp on tablets/desktop!
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 400.dp) // <--- THE TABLET SECRET
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom // Pushes content to the bottom
        ) {

            // Placeholder for the actual user image
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // You can replace this with an AsyncImage (Coil) later
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                )
            }

            // User Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bence, ", // Hardcoded for preview
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "26",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // ELO Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .border(1.dp, AppColors.AccentOrange, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Replace with Shield Icon if you have one
                        Icon(Icons.Default.Star, contentDescription = null, tint = AppColors.AccentOrange, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ELO 1450",
                            color = AppColors.AccentOrange,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Distance
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "1.5 km away",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playstyle Tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagChip("Offensive Looper")
                    TagChip("Advanced")
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

@Composable
fun ActionButtonsRow() {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Pass Button (Red)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF151C2C))
                .border(2.dp, Color(0xFFFF4B4B).copy(alpha = 0.5f), CircleShape)
                .clickable { /* Handle Pass */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Pass", tint = Color(0xFFFF4B4B), modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Super Like / Star Button (Teal)
        Box(
            modifier = Modifier
                .size(48.dp) // Slightly smaller
                .clip(CircleShape)
                .background(Color(0xFF151C2C))
                .border(2.dp, Color(0xFF00D2FF).copy(alpha = 0.5f), CircleShape)
                .clickable { /* Handle Super Like */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFF00D2FF), modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Like Button (Green)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF151C2C))
                .border(2.dp, Color(0xFF00E676).copy(alpha = 0.5f), CircleShape)
                .clickable { /* Handle Like */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = "Like", tint = Color(0xFF00E676), modifier = Modifier.size(32.dp))
        }
    }
}