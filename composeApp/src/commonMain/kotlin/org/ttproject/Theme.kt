package org.ttproject

import androidx.compose.ui.graphics.Color
import org.ttproject.SharedTheme

object AppColors {
    val Background = SharedTheme.hexBackground.toColor()
    val SurfaceDark = SharedTheme.hexSurfaceDark.toColor()
    val AccentCyan = SharedTheme.hexAccentCyan.toColor()
    val TextPrimary = SharedTheme.hexTextPrimary.toColor()
    val TextSecondary = SharedTheme.hexTextSecondary.toColor()
    val TextGray = SharedTheme.hexTextGray.toColor()
    val ProBadgeBg = SharedTheme.hexProBadgeBg.toColor()
    val AccentOrange = SharedTheme.hexAccentOrange.toColor()
    val ErrorText = SharedTheme.hexErrorText.toColor()
    val SuccessText = SharedTheme.hexSuccessText.toColor()
    val ButtonBackground = SharedTheme.hexButtonBackground.toColor()
}

fun String.toColor(): Color {
    return Color(removePrefix("#").toLong(16) or 0x00000000FF000000)
}