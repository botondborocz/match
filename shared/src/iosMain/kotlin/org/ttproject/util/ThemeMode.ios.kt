package org.ttproject.util

import androidx.compose.runtime.Composable
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle

@Composable
actual fun SetStatusBarColors(isDark: Boolean) {
    // No-op: iOS generally handles this automatically via the system theme,
    // or it's configured in your MainViewController bindings.
    // 👇 Force the entire iOS app window (and the keyboard!) to match your Compose theme
    val style = if (isDark) {
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
    } else {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight
    }

    UIApplication.sharedApplication.windows.firstOrNull()?.overrideUserInterfaceStyle = style
}