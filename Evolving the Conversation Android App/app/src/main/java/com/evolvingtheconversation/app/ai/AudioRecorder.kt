package com.evolvingtheconversation.app.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay
import java.io.File

/**
 * Records microphone audio to a temporary .m4a (AAC in an MPEG-4 container) file
 * and hands back the raw bytes — the same "audio/mp4" mime type the iOS app sends.
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    // MediaRecorder.stop() throws if called before it has captured any real
    // audio data — this happens if stop() is called too soon after start().
    // Enforcing a minimum duration avoids that failure mode entirely.
    private val minRecordingMs = 800L

    val isRecording: Boolean
        get() = recorder != null

    /** Starts recording. Caller must hold RECORD_AUDIO permission before calling this. */
    fun start() {
        cancel() // clear out any previous session first
        val file = File.createTempFile("tutor_recording_", ".m4a", context.cacheDir)
        outputFile = file

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        try {
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mr
            startTimeMs = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "start() failed", e)
            mr.release()
            recorder = null
            outputFile?.delete()
            outputFile = null
            throw e
        }
    }

    /**
     * Stops recording and returns the recorded audio as raw bytes, or null if
     * nothing was recorded (e.g. start() failed or was never called, or the
     * file turned out empty).
     *
     * Suspends briefly if called too soon after start() — MediaRecorder needs
     * a minimum amount of captured audio before stop() will succeed.
     */
    suspend fun stopAndGetBytes(): ByteArray? {
        val mr = recorder ?: return null

        val elapsed = System.currentTimeMillis() - startTimeMs
        if (elapsed < minRecordingMs) {
            delay(minRecordingMs - elapsed)
        }

        return try {
            mr.stop()
            val bytes = outputFile?.readBytes()
            if (bytes == null || bytes.isEmpty()) {
                Log.w("AudioRecorder", "stopAndGetBytes(): file was empty")
                null
            } else {
                bytes
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "stop() failed", e)
            null
        } finally {
            mr.release()
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }

    /** Cancels an in-progress recording without returning any data. Safe to call anytime. */
    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: Exception) {
            // stop() throws if start() never actually began recording — safe to ignore here.
        } finally {
            recorder?.release()
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }
}