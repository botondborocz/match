package org.ttproject.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Tells Compose: "I'll provide the right spinner depending on the platform!"
@Composable
expect fun PlatformSpinner(modifier: Modifier = Modifier)