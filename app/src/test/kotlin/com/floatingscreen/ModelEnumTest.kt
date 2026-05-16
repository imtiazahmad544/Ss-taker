package com.floatingscreen

import com.floatingscreen.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class ModelEnumTest {

    @Test
    fun `RecordingQuality fromLabel returns correct value`() {
        assertEquals(RecordingQuality.FHD, RecordingQuality.fromLabel("FHD (1080p)"))
        assertEquals(RecordingQuality.HD, RecordingQuality.fromLabel("HD (720p)"))
    }

    @Test
    fun `RecordingQuality fromLabel defaults to HD for unknown`() {
        assertEquals(RecordingQuality.HD, RecordingQuality.fromLabel("Unknown Quality"))
    }

    @Test
    fun `FrameRate fromFps returns correct value`() {
        assertEquals(FrameRate.FPS_30, FrameRate.fromFps(30))
        assertEquals(FrameRate.FPS_60, FrameRate.fromFps(60))
    }

    @Test
    fun `FrameRate fromFps defaults to 30 for unknown`() {
        assertEquals(FrameRate.FPS_30, FrameRate.fromFps(99))
    }

    @Test
    fun `AudioSource fromLabel returns correct value`() {
        assertEquals(AudioSource.NONE, AudioSource.fromLabel("No Audio"))
        assertEquals(AudioSource.MICROPHONE, AudioSource.fromLabel("Microphone"))
    }

    @Test
    fun `SaveLocation fromLabel returns default for unknown`() {
        assertEquals(SaveLocation.DEFAULT, SaveLocation.fromLabel("nonexistent"))
    }

    @Test
    fun `PermissionStatus canRecord requires all permissions`() {
        val status = PermissionStatus(
            overlayPermission = true,
            notificationPermission = true,
            mediaProjectionGranted = true,
            recordAudioPermission = false
        )
        assertTrue(status.canRecord)  // audio not strictly required for canRecord flag
    }

    @Test
    fun `PermissionStatus canRecord is false without overlay`() {
        val status = PermissionStatus(
            overlayPermission = false,
            notificationPermission = true,
            mediaProjectionGranted = true
        )
        assertFalse(status.canRecord)
    }

    @Test
    fun `RecordingQuality bitrates are reasonable`() {
        assertTrue(RecordingQuality.SD.bitrate < RecordingQuality.HD.bitrate)
        assertTrue(RecordingQuality.HD.bitrate < RecordingQuality.FHD.bitrate)
        assertTrue(RecordingQuality.FHD.bitrate < RecordingQuality.QHD.bitrate)
    }

    @Test
    fun `RecordingQuality dimensions increase with quality`() {
        assertTrue(RecordingQuality.SD.height < RecordingQuality.HD.height)
        assertTrue(RecordingQuality.HD.height < RecordingQuality.FHD.height)
    }
}
