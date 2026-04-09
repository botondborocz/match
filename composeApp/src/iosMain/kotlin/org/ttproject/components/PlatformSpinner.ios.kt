package org.ttproject.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIActivityIndicatorViewStyleMedium

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformSpinner(modifier: Modifier) {
    UIKitView(
        factory = {
            // 👇 THE FIX: Change parameter name to 'activityIndicatorStyle'
            UIActivityIndicatorView(activityIndicatorStyle = UIActivityIndicatorViewStyleMedium).apply {
                startAnimating()
                hidesWhenStopped = false
            }
        },
        modifier = modifier
    )
}