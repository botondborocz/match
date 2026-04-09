package org.ttproject.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import org.ttproject.AppColors

@Composable
actual fun PlatformSpinner(modifier: Modifier) {
    val spinnerColor = AppColors.TextGray
    // 1. The infinite ticking animation
    val infiniteTransition = rememberInfiniteTransition()
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // 2. Draw the 12 iOS petals purely in Compose!
    Canvas(modifier = modifier) {
        val petalCount = 12
        val currentTick = tick.toInt()
        val center = Offset(size.width / 2, size.height / 2)

        for (i in 0 until petalCount) {
            // Calculates the classic iOS trailing fade effect
            val distance = (currentTick - i + petalCount) % petalCount
            val alpha = (1f - (distance / petalCount.toFloat())).coerceIn(0.25f, 1f)

            rotate(degrees = i * (360f / petalCount), pivot = center) {
                drawLine(
                    color = spinnerColor.copy(alpha = alpha),
                    start = Offset(center.x, size.height * 0.1f), // Top part of petal
                    end = Offset(center.x, size.height * 0.35f),  // Bottom part of petal
                    strokeWidth = size.width * 0.08f,             // Auto-scales the thickness
                    cap = StrokeCap.Round
                )
            }
        }
    }
}