package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode { Light, Dark, System }

// This broadcasts the current theme state to the entire app
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.System }

@Composable
expect fun SetStatusBarColors(isDark: Boolean, isSystemDefault: Boolean)

@Composable
expect fun ClearChatNotificationEffect(chatId: String)