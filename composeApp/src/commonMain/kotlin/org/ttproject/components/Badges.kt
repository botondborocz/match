package org.ttproject.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ttproject.AppColors
import kotlin.math.sqrt

// --- 1. Adatmodell ---
data class BadgeData(
    val name: String,
    val icon: ImageVector,
    val level: Int // 1: Steel, 2: Cyber Blue, 3: Neon Orange, 4: Plasma
)

// --- 2. Hatszögletű Forma (Pointy-topped Hexagon) ---
class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val width = size.width
            val height = size.height
            moveTo(width / 2f, 0f)
            lineTo(width, height * 0.25f)
            lineTo(width, height * 0.75f)
            lineTo(width / 2f, height)
            lineTo(0f, height * 0.75f)
            lineTo(0f, height * 0.25f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun BadgeItem(badge: BadgeData, modifier: Modifier = Modifier) {
    val badgeColor = when (badge.level) {
        1 -> Color(0xFF6C7A89) // Steel
        2 -> Color(0xFF00E5FF) // Cyber Blue
        3 -> Color(0xFFFF6B35) // Neon Orange
        4 -> Color(0xFFD500F9) // Plasma
        else -> Color.DarkGray
    }

    val infiniteTransition = rememberInfiniteTransition(label = "plasmaPulse")
    val scaleMultiplier by if (badge.level == 4) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAnimation"
        )
    } else {
        androidx.compose.runtime.mutableStateOf(1f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .scale(scaleMultiplier),
            contentAlignment = Alignment.Center
        ) {
            // 👇 JAVÍTOTT CANVAS MATEMATIKA 👇
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path().apply {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // A sugár a rendelkezésre álló hely fele
                    val radius = minOf(size.width, size.height) / 2f

                    // Szabályos hatszög szélessége: sugár * gyök(3)
                    val hexWidth = radius * sqrt(3f)

                    moveTo(cx, cy - radius) // Felső csúcs
                    lineTo(cx + hexWidth / 2f, cy - radius / 2f) // Jobb felső
                    lineTo(cx + hexWidth / 2f, cy + radius / 2f) // Jobb alsó
                    lineTo(cx, cy + radius) // Alsó csúcs
                    lineTo(cx - hexWidth / 2f, cy + radius / 2f) // Bal alsó
                    lineTo(cx - hexWidth / 2f, cy - radius / 2f) // Bal felső
                    close()
                }
                drawPath(
                    path = path,
                    color = badgeColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Icon(
                imageVector = badge.icon,
                contentDescription = badge.name,
                tint = badgeColor,
                modifier = Modifier.size(18.dp) // Picit kisebbre vettem az ikont, hogy biztosan ne lógjon ki a keskenyebb hatszögből
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = badge.name,
            color = AppColors.TextPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            minLines = 2,
            maxLines = 2
        )
    }
}

// --- 4. A Reszponzív, Nem Görgethető Kitűző Sáv ---
@Composable
fun BadgesSection() {
    val myBadges = listOf(
        BadgeData("Alapító Tag", Icons.Default.Star, 4),
        BadgeData("Asztalfelderítő", Icons.Default.Place, 2),
        BadgeData("Helyszínelő", Icons.Default.CameraAlt, 1),
        BadgeData("Helyi Kritikus", Icons.Default.RateReview, 3),
        BadgeData("Jégtörő", Icons.Default.Bolt, 2),
        BadgeData("A Pálya Ördöge", Icons.Default.SportsKabaddi, 3),
        BadgeData("Sportdiplomata", Icons.Default.EventAvailable, 2),
        BadgeData("Okos Vágó", Icons.Default.ContentCut, 1),
        BadgeData("Elemzés Függő", Icons.Default.Psychology, 3),
        BadgeData("Sebességkirály", Icons.Default.Speed, 3)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Default.MilitaryTech,
                contentDescription = null,
                tint = AppColors.TextGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "BADGES",
                color = AppColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 👇 A 10 kitűzőt feldaraboljuk 5-ös csoportokra (2 sor lesz belőle)
        val chunkedBadges = myBadges.chunked(5)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp) // Távolság a két sor között
        ) {
            chunkedBadges.forEach { rowBadges ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly // Egyenletes eloszlás
                ) {
                    rowBadges.forEach { badge ->
                        BadgeItem(
                            badge = badge,
                            modifier = Modifier.weight(1f) // 👇 Ez osztja el a szélességet matematikai pontossággal
                        )
                    }
                }
            }
        }
    }
}