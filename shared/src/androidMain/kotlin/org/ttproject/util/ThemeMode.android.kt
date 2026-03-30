package org.ttproject.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.NotificationManager
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun SetStatusBarColors(isDark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // This is the magic line.
            // If the theme is dark, we DO NOT want light status bar appearance (black icons).
            // We want the opposite, so we pass !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }
}

@Composable
actual fun ClearChatNotificationEffect(chatId: String) {
    val context = LocalContext.current

    LaunchedEffect(chatId) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Cancel the specific notification using the hashcode!
        notificationManager.cancel(chatId.hashCode())
    }
}