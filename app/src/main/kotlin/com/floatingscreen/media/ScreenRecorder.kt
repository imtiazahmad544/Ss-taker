package com.floatingscreen.media

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import com.floatingscreen.domain.model.AudioSource
import com.floatingscreen.domain.model.FrameRate
import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType
import com.floatingscreen.domain.model.RecordingQuality
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ScreenRecorder(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    private var mediaRecorder: MediaRecorder? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L
    private var isRecording = false

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }

    fun startRecording(
        quality: RecordingQuality,
        frameRate: FrameRate,
        audioSource: AudioSource,
        onError: (String) -> Unit
    ): File? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "REC_$timestamp.mp4"
            val dir = getOutputDirectory("Movies")
            dir.mkdirs()
            val file = File(dir, fileName)
            outputFile = file

            setupMediaRecorder(quality, frameRate, audioSource, file)

            val (recordWidth, recordHeight) = calculateDimensions(quality)

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                recordWidth,
                recordHeight,
                displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder!!.surface,
                null,
                null
            )

            mediaRecorder!!.start()
            startTimeMs = System.currentTimeMillis()
            isRecording = true

            Timber.d("Screen recording started: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Timber.e(e, "Failed to start recording")
            onError("Failed to start recording: ${e.message}")
            cleanup()
            null
        }
    }

    fun stopRecording(): MediaRecord? {
        return try {
            if (!isRecording) return null

            val elapsed = System.currentTimeMillis() - startTimeMs
            isRecording = false

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            virtualDisplay?.release()
            virtualDisplay = null

            val file = outputFile ?: return null
            if (!file.exists()) return null

            val quality = RecordingQuality.FHD
            MediaRecord(
                fileName = file.name,
                filePath = file.absolutePath,
                mediaType = MediaType.RECORDING,
                timestamp = startTimeMs,
                durationMs = elapsed,
                width = quality.width,
                height = quality.height,
                fileSizeBytes = file.length()
            )
        } catch (e: Exception) {
            Timber.e(e, "Error stopping recording")
            cleanup()
            null
        }
    }

    fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
        }
    }

    fun resumeRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.resume()
        }
    }

    private fun setupMediaRecorder(
        quality: RecordingQuality,
        frameRate: FrameRate,
        audioSource: AudioSource,
        outputFile: File
    ) {
        val (recordWidth, recordHeight) = calculateDimensions(quality)

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder!!.apply {
            // Audio setup
            when (audioSource) {
                AudioSource.MICROPHONE, AudioSource.BOTH -> {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                }
                AudioSource.INTERNAL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX)
                    }
                }
                AudioSource.NONE -> { /* no audio */ }
            }

            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            if (audioSource != AudioSource.NONE) {
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128_000)
            }

            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(quality.bitrate)
            setVideoFrameRate(frameRate.fps)
            setVideoSize(recordWidth, recordHeight)
            setOutputFile(outputFile.absolutePath)

            prepare()
        }
    }

    private fun calculateDimensions(quality: RecordingQuality): Pair<Int, Int> {
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Use screen aspect ratio but cap at quality max
        val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
        val maxHeight = quality.height
        val maxWidth = quality.width

        val width: Int
        val height: Int

        if (aspectRatio > 1) {
            width = minOf(screenWidth, maxWidth)
            height = (width / aspectRatio).toInt().let { if (it % 2 != 0) it + 1 else it }
        } else {
            height = minOf(screenHeight, maxHeight)
            width = (height * aspectRatio).toInt().let { if (it % 2 != 0) it + 1 else it }
        }

        return Pair(width, height)
    }

    private fun getOutputDirectory(type: String): File {
        return File(
            Environment.getExternalStoragePublicDirectory(
                if (type == "Movies") Environment.DIRECTORY_MOVIES
                else Environment.DIRECTORY_PICTURES
            ),
            "FloatingScreenUtility"
        )
    }

    private fun cleanup() {
        try {
            mediaRecorder?.release()
            virtualDisplay?.release()
        } catch (e: Exception) {
            Timber.e(e, "Error during cleanup")
        } finally {
            mediaRecorder = null
            virtualDisplay = null
        }
    }

    fun release() {
        if (isRecording) stopRecording()
        cleanup()
    }
}
