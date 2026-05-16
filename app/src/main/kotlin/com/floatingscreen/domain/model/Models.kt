package com.floatingscreen.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// ============================================================
// Recording Quality
// ============================================================

enum class RecordingQuality(val label: String, val width: Int, val height: Int, val bitrate: Int) {
    SD("SD (480p)", 854, 480, 2_000_000),
    HD("HD (720p)", 1280, 720, 4_000_000),
    FHD("FHD (1080p)", 1920, 1080, 8_000_000),
    QHD("QHD (1440p)", 2560, 1440, 16_000_000);

    companion object {
        fun fromLabel(label: String) = values().find { it.label == label } ?: HD
    }
}

enum class FrameRate(val fps: Int, val label: String) {
    FPS_24(24, "24 fps"),
    FPS_30(30, "30 fps"),
    FPS_60(60, "60 fps");

    companion object {
        fun fromFps(fps: Int) = values().find { it.fps == fps } ?: FPS_30
    }
}

enum class AudioSource(val label: String) {
    NONE("No Audio"),
    MICROPHONE("Microphone"),
    INTERNAL("Internal Audio (Android 10+)"),
    BOTH("Mic + Internal");

    companion object {
        fun fromLabel(label: String) = values().find { it.label == label } ?: MICROPHONE
    }
}

// ============================================================
// Media Record
// ============================================================

enum class MediaType { RECORDING, SCREENSHOT }

@Parcelize
data class MediaRecord(
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val mediaType: MediaType,
    val timestamp: Long,
    val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Long = 0,
    val thumbnailPath: String? = null
) : Parcelable {

    val formattedDuration: String
        get() {
            if (durationMs == 0L) return ""
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            val hours = durationMs / (1000 * 60 * 60)
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() = when {
            fileSizeBytes >= 1_000_000_000 -> "%.1f GB".format(fileSizeBytes / 1_000_000_000.0)
            fileSizeBytes >= 1_000_000 -> "%.1f MB".format(fileSizeBytes / 1_000_000.0)
            fileSizeBytes >= 1_000 -> "%.1f KB".format(fileSizeBytes / 1_000.0)
            else -> "$fileSizeBytes B"
        }

    val resolution: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else ""
}

// ============================================================
// Recording State
// ============================================================

sealed class RecordingState {
    object Idle : RecordingState()
    object Preparing : RecordingState()
    data class Recording(val startTimeMs: Long) : RecordingState()
    data class Paused(val elapsedMs: Long) : RecordingState()
    object Stopping : RecordingState()
    data class Completed(val record: MediaRecord) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

// ============================================================
// Screenshot State
// ============================================================

sealed class ScreenshotState {
    object Idle : ScreenshotState()
    data class Countdown(val secondsLeft: Int) : ScreenshotState()
    object Capturing : ScreenshotState()
    data class Completed(val record: MediaRecord) : ScreenshotState()
    data class Error(val message: String) : ScreenshotState()
}

// ============================================================
// App Settings
// ============================================================

data class AppSettings(
    val recordingQuality: RecordingQuality = RecordingQuality.FHD,
    val frameRate: FrameRate = FrameRate.FPS_30,
    val audioSource: AudioSource = AudioSource.MICROPHONE,
    val floatingButtonOpacity: Float = 0.85f,
    val isDarkTheme: Boolean = true,
    val autoStartFloating: Boolean = false,
    val showCountdownTimer: Boolean = true,
    val vibrateFeedback: Boolean = true,
    val saveLocation: SaveLocation = SaveLocation.DEFAULT
)

enum class SaveLocation(val label: String) {
    DEFAULT("Default (Movies/Pictures)"),
    DOWNLOADS("Downloads"),
    DCIM("DCIM/FloatingScreen");

    companion object {
        fun fromLabel(label: String) = values().find { it.label == label } ?: DEFAULT
    }
}

// ============================================================
// Permission State
// ============================================================

data class PermissionStatus(
    val overlayPermission: Boolean = false,
    val recordAudioPermission: Boolean = false,
    val notificationPermission: Boolean = false,
    val mediaProjectionGranted: Boolean = false
) {
    val canRecord: Boolean get() = overlayPermission && notificationPermission && mediaProjectionGranted
    val canScreenshot: Boolean get() = overlayPermission && notificationPermission && mediaProjectionGranted
}

// ============================================================
// Floating Window Position
// ============================================================

data class FloatingPosition(
    val x: Int = 0,
    val y: Int = 200
)
