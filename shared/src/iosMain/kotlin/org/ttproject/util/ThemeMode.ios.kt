package org.ttproject.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindow // 👈 1. Add this import!

@Composable
actual fun SetStatusBarColors(isDark: Boolean, isSystemDefault: Boolean) {
    // 👇 The Magic Logic
    val style = when {
        isSystemDefault -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified // Hands control back to iOS!
        isDark -> UIUserInterfaceStyle.UIUserInterfaceStyleDark // Forces Dark (and dark keyboard)
        else -> UIUserInterfaceStyle.UIUserInterfaceStyleLight // Forces Light (and light keyboard)
    }

    val window = UIApplication.sharedApplication.connectedScenes
        .firstNotNullOfOrNull { it as? UIWindowScene }
        ?.windows
        ?.firstOrNull() as? UIWindow

    window?.overrideUserInterfaceStyle = style
}

@Composable
actual fun ClearChatNotificationEffect(chatId: String) {
    LaunchedEffect(chatId) {
        // iOS notification clearing logic goes here later!
        // (Uses UNUserNotificationCenter)
    }
}