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

    val hexBackgroundLight = "#F8FAFC"
// Nagyon világos, hideg "pala" szürke. Kellemesebb a szemnek, mint a tiszta fehér, és térérzetet ad a kártyáknak.

    val hexSurfaceLight = "#FFFFFF"
// Tiszta fehér. Ezen fognak ülni a tartalmi elemek (pl. a Tinder-kártyák), egy nagyon finom árnyékkal.

    val hexAccentCyanLight = "#0284C7"
// Sötétebb, mélyebb ciánkék (Sky-600). Az eredeti #00D2FF fehér alapon olvashatatlan lenne, ez viszont megőrzi a tech-vonalat, de erős kontrasztot ad.

    val hexTextPrimaryLight = "#0F172A"
// Zseniális trükk: a sötét témád hátterét használjuk fő szövegszínnek! Feketébb a szürkénél, de puhább a tiszta feketénél. Gyönyörű lesz.

    val hexTextSecondaryLight = "#475569"
// Középsötét szürkéskék. Alcímekhez, ikonokhoz.

    val hexTextGrayLight = "#64748B"
// Világosabb szürke (pl. "2 órája", "1.5 km-re" feliratokhoz).

    val hexProBadgeBgLight = "#E0F2FE"
// Egy nagyon halvány, elegáns jégkék. Egy világos témában a sötét badge túl nehéz lenne, így viszont a sötétkék szöveggel (PrimaryLight) nagyon prémium hatást kelt.

    val hexAccentOrangeLight = "#FF6B35"
// MARAD! Ez az appod lelke (a pingpong labda színe). Fehér/világos alapon is brutálisan jól vonzza a tekintetet.

    val hexErrorTextLight = "#DC2626"
// Kicsit sötétebb piros. Az eredeti neon pirosad túl világos lenne fehér alapon.

    val hexSuccessTextLight = "#16A34A"
// Kicsit sötétebb, határozottabb zöld a megfelelő kontrasztért.

    val hexButtonBackgroundLight = "#F1F5F9"
// Világosszürke a másodlagos (nem narancssárga) gombok és beviteli mezők (TextField) hátterének.
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