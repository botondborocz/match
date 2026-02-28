package org.ttproject.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.ttproject.AppColors
import org.ttproject.AppIcon
import org.ttproject.MainNavItems
import org.ttproject.NavRoute
import org.ttproject.NavigationItem

@Composable
fun MobileBottomNav(
    currentRoute: NavRoute,
    onNavigate: (NavRoute) -> Unit
) {
    val glowColor = AppColors.AccentOrange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.SurfaceDark)
    ) {
        // We use BoxWithConstraints to calculate the exact width of each tab for the sliding animation
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(80.dp) // Fixed height for the bar area
        ) {
            val totalWidth = maxWidth
            val itemCount = MainNavItems.size
            val itemWidth = totalWidth / itemCount

            // 1. Find the index of the currently selected item
            val selectedIndex = MainNavItems.indexOfFirst { it.route == currentRoute }
            val isCoachSelected = MainNavItems[selectedIndex].route == NavRoute.Coach

            // 2. Animate the X offset for the sliding line
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedIndex,
                animationSpec = tween(durationMillis = 300)
            )

            // 3. Animate the opacity (Hide line if Coach/Middle button is selected)
            val indicatorAlpha by animateFloatAsState(
                targetValue = if (isCoachSelected) 0f else 1f,
                animationSpec = tween(durationMillis = 200)
            )

            // --- THE SLIDING GLOW LINE ---
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .alpha(indicatorAlpha), // Fade out when Coach is selected
                contentAlignment = Alignment.BottomCenter
            ) {
                // This creates the "Blurred" look using a gradient
                Box(
                    modifier = Modifier
                        .padding(bottom = 2.dp) // Distance from bottom
                        .width(40.dp) // The line is narrower than the full tab
                        .height(4.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    glowColor.copy(alpha = 0.8f), // Center is bright
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // --- THE ICONS ROW ---
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MainNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val isCoach = item.route == NavRoute.Coach
                    val label = stringResource(item.title)

                    // Interaction source to remove ripple if desired, or keep it standard
                    val interactionSource = remember { MutableInteractionSource() }

                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null // Removes the grey click splash for a cleaner look
                            ) { onNavigate(item.route) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCoach) {
                            // --- BIG COACH BUTTON (Pops out) ---
                            CoachButton(
                                item = item,
                                isSelected = isSelected,
                                label = label,
                                glowColor = glowColor
                            )
                        } else {
                            // --- STANDARD TAB ITEM ---
                            StandardTabItem(
                                item = item,
                                isSelected = isSelected,
                                label = label,
                                activeColor = glowColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandardTabItem(
    item: org.ttproject.NavigationItem,
    isSelected: Boolean,
    label: String,
    activeColor: Color
) {
    // Animate the vertical position (Move UP when selected)
    val animatedOffsetY by animateDpAsState(
        targetValue = if (isSelected) (-6).dp else 0.dp,
        animationSpec = tween(durationMillis = 300) // Smooth spring or tween
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
//        modifier = Modifier.offset(y = animatedOffsetY)
    ) {
        val tint = if (isSelected) activeColor else AppColors.TextSecondary
        val iconSize = 26.dp

        // Render Icon Wrapper
        when (val icon = item.icon) {
            is AppIcon.Vector -> Icon(
                imageVector = icon.value,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(iconSize).offset(y = animatedOffsetY)
            )
            is AppIcon.Drawable -> Icon(
                painter = painterResource(icon.value),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(iconSize).offset(y = animatedOffsetY)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            color = tint,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun CoachButton(
    item: NavigationItem,
    isSelected: Boolean,
    label: String,
    glowColor: Color
) {
    // 1. Scale Animation (Bouncy Spring)
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    // 2. Elevation/Shadow Animation
    val elevation by animateDpAsState(
        targetValue = if (isSelected) 12.dp else 4.dp,
        animationSpec = tween(300)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // Push the whole column up so the button floats, but text stays near bottom
        modifier = Modifier.offset(y = (-18).dp)
    ) {
        // --- THE GLOWING CIRCLE ---
        Box(
            modifier = Modifier
                .scale(scale) // Apply scale only to the circle
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    spotColor = glowColor, // Colored shadow creates the glow
                    ambientColor = glowColor
                )
                .size(56.dp)
                .clip(CircleShape)
                .background(glowColor)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (val icon = item.icon) {
                is AppIcon.Vector -> Icon(
                    imageVector = icon.value,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                is AppIcon.Drawable -> Icon(
                    painter = painterResource(icon.value),
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // --- TEXT UNDER THE CIRCLE ---
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) glowColor else AppColors.TextSecondary,
            // Ensure text doesn't scale with the button (looks blurry if scaled)
            modifier = Modifier.scale(1f)
        )
    }
}