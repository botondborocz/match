package org.ttproject

import androidx.compose.ui.window.ComposeUIViewController
import org.ttproject.di.initKoin

private var isKoinInitialized = false

fun MainViewController() = ComposeUIViewController {
    if (!isKoinInitialized) {
        initKoin()
        isKoinInitialized = true
    }

    App()
}