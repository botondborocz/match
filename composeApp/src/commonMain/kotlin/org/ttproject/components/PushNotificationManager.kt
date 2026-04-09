package org.ttproject.components

import androidx.compose.runtime.Composable

@Composable
expect fun PushNotificationManager(onTokenReceived: (String) -> Unit)