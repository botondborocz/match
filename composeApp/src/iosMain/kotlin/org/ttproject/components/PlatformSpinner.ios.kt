package org.ttproject.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityIndicatorView
import platform.UIKit.UIActivityIndicatorViewStyleMedium
import platform.UIKit.UIColor

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformSpinner(modifier: Modifier) {
    UIKitView(
        factory = {
            UIActivityIndicatorView(activityIndicatorStyle = UIActivityIndicatorViewStyleMedium).apply {
                startAnimating()
                hidesWhenStopped = false

                // 👇 THE FIX: Force the native view to have a solid background
                // Note: You have to manually convert your AppColors hex to iOS RGB values here
                backgroundColor = UIColor(
                    red = 15.0 / 255.0, // Replace with your exact background RGB percentages
                    green = 23.0 / 255.0,
                    blue = 42.0 / 255.0,
                    alpha = 1.0
                )
            }
        },
        modifier = modifier
    )
}