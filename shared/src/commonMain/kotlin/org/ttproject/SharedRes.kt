package org.ttproject

import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

@OptIn(ExperimentalJsExport::class)
@JsExport
object SharedTheme {
    // Define colors as Hex Strings so both React and Compose can read them
    val hexBackground = "#0F172A"
    val hexSurfaceDark = "#162032"
    val hexAccentCyan = "#00D2FF"
    val hexTextPrimary = "#FFFFFF"
    val hexTextSecondary = "#94A3B8"
    val hexProBadgeBg = "#0C4A6E"
    val hexAccentOrange = "#FF6B35"
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