package org.ttproject.components

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.ttproject.AppColors

@Composable
actual fun PlatformSpinner(modifier: Modifier) {
    CircularProgressIndicator(
        modifier = modifier,
        color = AppColors.TextGray,
        strokeWidth = 2.dp
    )
}