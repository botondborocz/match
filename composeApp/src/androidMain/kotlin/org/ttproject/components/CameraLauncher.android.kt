package org.ttproject.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

actual class CameraLauncher(
    private val onLaunch: () -> Unit
) {
    actual fun launch() {
        onLaunch()
    }
}

@Composable
actual fun rememberCameraLauncher(onResult: (ByteArray?) -> Unit): CameraLauncher {
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempPhotoUri != null) {
                // Read the bytes from the saved file and return them
                val bytes = context.contentResolver.openInputStream(tempPhotoUri!!)?.use { it.readBytes() }
                onResult(bytes)
            } else {
                onResult(null)
            }
        }
    )

    return remember {
        CameraLauncher(
            onLaunch = {
                // 1. Create a temporary empty file in the cache
                val tempFile = File.createTempFile("capture_", ".jpg", context.cacheDir)

                // 2. Get a secure URI using the FileProvider
                tempPhotoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )

                // 3. Launch the Android camera!
                cameraLauncher.launch(tempPhotoUri!!)
            }
        )
    }
}