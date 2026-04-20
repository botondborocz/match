package org.ttproject.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(modifier: Modifier, url: String) {
    // 👇 FIX 1: Pass 'url' into remember!
    val player = remember(url) { AVPlayer(uRL = NSURL.URLWithString(url)!!) }
    val playerViewController = remember(url) { AVPlayerViewController() }

    playerViewController.player = player
    playerViewController.showsPlaybackControls = true

    UIKitView(
        factory = { playerViewController.view },
        modifier = modifier,
        update = { player.play() }
    )
}