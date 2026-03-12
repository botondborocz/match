package org.ttproject.util

import androidx.compose.runtime.Composable

@Composable
actual fun SetStatusBarColors(isDark: Boolean) {
    // No-op: iOS generally handles this automatically via the system theme,
    // or it's configured in your MainViewController bindings.
}