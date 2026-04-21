package org.ttproject.components

import androidx.compose.runtime.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue // 👈 Added this
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class) // 👈 Added global opt-in for this class
class IosAudioPlayer : AudioPlayer {
    private var player: AVPlayer? = null
    private var timeObserver: Any? = null

    override var isPlaying by mutableStateOf(false)
    override var currentPosition by mutableLongStateOf(0L)
    override var duration by mutableLongStateOf(0L)

    override fun play(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        stop()

        val playerItem = AVPlayerItem(uRL = nsUrl)
        player = AVPlayer(playerItem = playerItem)

        // 👈 FIX: Wrap CMTimeMake in cValue
        val interval = cValue<CMTime> {
            val time = CMTimeMake(value = 1, timescale = 10)
            value = time.value
            timescale = time.timescale
            flags = time.flags
            epoch = time.epoch
        }

        timeObserver = player?.addPeriodicTimeObserverForInterval(interval, null) { time ->
            time?.useContents {
                currentPosition = (CMTimeGetSeconds(this) * 1000).toLong()
            }

            val durationTime = player?.currentItem?.duration
            durationTime?.useContents {
                val secs = CMTimeGetSeconds(this)
                if (!secs.isNaN()) {
                    duration = (secs * 1000).toLong()
                }
            }

            isPlaying = player?.rate != 0f
        }

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = AVPlayerItemDidPlayToEndTimeNotification,
            `object` = player?.currentItem,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            isPlaying = false
            currentPosition = 0L
            // 👈 FIX: Use kCMTimeZero properly
            player?.seekToTime(kCMTimeZero.readValue())
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
        // 👈 FIX: Wrap seek time in cValue
        val time = cValue<CMTime> {
            val t = CMTimeMakeWithSeconds(seconds = position / 1000.0, preferredTimescale = 1000)
            value = t.value
            timescale = t.timescale
            flags = t.flags
            epoch = t.epoch
        }
        player?.seekToTime(time)
        currentPosition = position
    }
}

@Composable
actual fun rememberAudioPlayer(): AudioPlayer {
    val player = remember { IosAudioPlayer() }
    DisposableEffect(Unit) {
        onDispose { player.stop() }
    }
    return player
}