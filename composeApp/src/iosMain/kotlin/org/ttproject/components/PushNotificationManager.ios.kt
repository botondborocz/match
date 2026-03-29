package org.ttproject.components

import androidx.compose.runtime.Composable

@Composable
actual fun PushNotificationManager(onTokenReceived: (String) -> Unit) {
    // TODO: Implement APNs fetch when Apple Developer Account is active
    println("iOS Push token fetch ignored for now.")
}