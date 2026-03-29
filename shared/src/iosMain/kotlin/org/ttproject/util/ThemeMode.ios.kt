package org.ttproject.util

import androidx.compose.runtime.Composable
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindowScene
import platform.UIKit.UIWindow // 👈 1. Add this import!

@Composable
actual fun SetStatusBarColors(isDark: Boolean) {
    val style = if (isDark) {
        UIUserInterfaceStyle.UIUserInterfaceStyleDark
    } else {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight
    }

    val window =
        UIApplication.sharedApplication.connectedScenes.firstNotNullOfOrNull { it as? UIWindowScene }
            ?.windows
        ?.firstOrNull() as? UIWindow // 👈 2. Add 'as? UIWindow' right here

    window?.overrideUserInterfaceStyle = style
}