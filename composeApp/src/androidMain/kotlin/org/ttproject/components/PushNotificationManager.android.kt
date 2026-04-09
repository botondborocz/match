package org.ttproject.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

@Composable
actual fun PushNotificationManager(onTokenReceived: (String) -> Unit) {
    val context = LocalContext.current

    // 1. Silently check if they already granted it (or if they are on an older Android version)
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true // Android 12 and below don't need runtime permission for this
            }
        )
    }

    // 2. The Permission Launcher (just like your map!)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // 3. The Trigger: Listen to the permission state
    LaunchedEffect(hasNotificationPermission) {
        if (hasNotificationPermission) {
            // We have permission! Grab the token from Firebase.
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    println("✅ Got Android FCM token: $token")
                    onTokenReceived(token)
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // We don't have permission yet. Launch the prompt!
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}