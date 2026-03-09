package org.ttproject

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
object SharedTheme {
    // --- DARK THEME COLORS ---
    val hexBackground = "#0F172A"
    val hexSurfaceDark = "#162032"
    val hexAccentCyan = "#00D2FF"
    val hexTextPrimary = "#FFFFFF"
    val hexTextSecondary = "#94A3B8"
    val hexTextGray = "#A0AABF"
    val hexProBadgeBg = "#0C4A6E"
    val hexAccentOrange = "#FF6B35"
    val hexErrorText = "#FF4B4B"
    val hexSuccessText = "#00E676"
    val hexButtonBackground = "#151C2C"

    // --- LIGHT THEME COLORS ---
    val hexBackgroundLight = "#F8FAFC" // Soft off-white/light slate
    val hexSurfaceLight = "#FFFFFF" // Pure white for cards

    // Accents (slightly darkened for better contrast on white backgrounds)
    val hexAccentCyanLight = "#0284C7"
    val hexAccentOrangeLight = "#E85D04"

    // Text
    val hexTextPrimaryLight = "#0F172A" // Almost black (inverts the dark background)
    val hexTextSecondaryLight = "#475569" // Slate gray
    val hexTextGrayLight = "#64748B" // Medium gray

    // UI Elements
    val hexProBadgeBgLight = "#E0F2FE" // Very light blue for the badge background
    val hexErrorTextLight = "#DC2626" // Deeper red for legibility
    val hexSuccessTextLight = "#16A34A" // Deeper green for legibility
    val hexButtonBackgroundLight = "#E2E8F0" // Light gray for inactive/standard buttons
}

@OptIn(ExperimentalJsExport::class)
@JsExport
object SharedStrings {
    val appName = "SpinSync"
    val home = "Home"
    val map = "Map"
    val aiCoach = "AI Coach"
    val match = "Match"
    val profile = "Profile"
    val pro = "PRO"
    val userName = "John Doe"
    val userTitle = "Semi-Pro Player"
}