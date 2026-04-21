package org.ttproject.components

import androidx.compose.runtime.*
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.darwin.NSObject

class IosAudioPlayer : AudioPlayer {
    private var player: AVPlayer? = null
    private var timeObserver: Any? = null

    override var isPlaying by mutableStateOf(false)
    override var currentPosition by mutableLongStateOf(0L)
    override var duration by mutableLongStateOf(0L)

    override fun play(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return

        // 1. Clean up any existing observer/player
        stop()

        val playerItem = AVPlayerItem(uRL = nsUrl)
        player = AVPlayer(playerItem = playerItem)

        // 2. Add Periodic Time Observer (updates every 100ms)
        val interval = CMTimeMake(value = 1, timescale = 10)
        timeObserver = player?.addPeriodicTimeObserverForInterval(interval, null) { time ->
            val currentTimeSeconds = CMTimeGetSeconds(time!!)
            currentPosition = (currentTimeSeconds * 1000).toLong()

            val durationSeconds = CMTimeGetSeconds(player?.currentItem?.duration ?: return@addPeriodicTimeObserverForInterval)
            if (!durationSeconds.isNaN()) {
                duration = (durationSeconds * 1000).toLong()
            }

            // Sync playing state
            isPlaying = player?.rate != 0f
        }

        // 3. Listen for completion to reset UI
        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player?.currentItem,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            isPlaying = false
            currentPosition = 0L
            player?.seekToTime(kCMTimeZero)
        }

        player?.play()
        isPlaying = true
    }

    override fun pause() {
        player?.pause()
        isPlaying = false
    }

    override fun resume() {
        player?.play()
        isPlaying = true
    }

    override fun stop() {
        player?.pause()
        timeObserver?.let {
            player?.removeTimeObserver(it)
            timeObserver = null
        }
        player = null
        isPlaying = false
        currentPosition = 0L
    }

    override fun seekTo(position: Long) {
        val time = CMTimeMakeWithSeconds(seconds = position / 1000.0, preferredTimescale = 1000)
        player?.seekToTime(time)
        currentPosition = position
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val player = remember { IosAudioPlayer() }

    // Cleanup when the user leaves the chat screen
    DisposableEffect(Unit) {
        onDispose {
            player.stop()
        }
    }

    return player
}