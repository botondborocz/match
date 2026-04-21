package org.ttproject.components

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.posix.memcpy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class VoiceRecorderImpl(
    private val onPermissionDenied: () -> Unit
) : VoiceRecorder {
    private var recorder: AVAudioRecorder? = null
    private var audioUrl: NSURL? = null

    override fun startRecording(onStart: () -> Unit) {
        val session = AVAudioSession.sharedInstance()

        session.requestRecordPermission { granted ->
            if (granted) {
                try {
                    session.setCategory(AVAudioSessionCategoryPlayAndRecord, mode = AVAudioSessionModeDefault, error = null)
                    session.setActive(true, error = null)

                    // 1. Create temp path
                    val tempPath = NSTemporaryDirectory() + NSUUID().UUIDString() + ".m4a"
                    val url = NSURL.fileURLWithPath(tempPath)
                    audioUrl = url

                    // 2. Configure recorder settings
                    val settings = mapOf<Any?, Any>(
                        AVFormatIDKey to kAudioFormatMPEG4AAC,
                        AVSampleRateKey to 44100.0,
                        AVNumberOfChannelsKey to 1
                    )

                    // 3. Start!
                    recorder = AVAudioRecorder(url, settings, null)
                    recorder?.record()

                    // 4. Tell UI to start
                    onStart()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                onPermissionDenied()
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stopRecording(): ByteArray? {
        recorder?.stop()
        recorder = null

        val url = audioUrl ?: return null
        val data = NSData.dataWithContentsOfURL(url) ?: return null

        val bytes = ByteArray(data.length.toInt()).apply {
            usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
        }

        NSFileManager.defaultManager.removeItemAtURL(url, null)
        audioUrl = null

        return bytes
    }

    override fun cancelRecording() {
        recorder?.stop()
        recorder = null
        audioUrl?.let { NSFileManager.defaultManager.removeItemAtURL(it, null) }
        audioUrl = null
    }
}

@Composable
actual fun rememberVoiceRecorder(onPermissionDenied: () -> Unit): VoiceRecorder {
    return remember { VoiceRecorderImpl(onPermissionDenied) }
}