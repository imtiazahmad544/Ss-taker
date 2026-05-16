package com.floatingscreen

import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType
import org.junit.Assert.*
import org.junit.Test

class MediaRecordTest {

    private fun makeRecord(type: MediaType, bytes: Long, durationMs: Long = 0) = MediaRecord(
        id = 1L,
        fileName = "test.${if (type == MediaType.RECORDING) "mp4" else "png"}",
        filePath = "/test/path",
        mediaType = type,
        timestamp = System.currentTimeMillis(),
        durationMs = durationMs,
        width = 1920,
        height = 1080,
        fileSizeBytes = bytes
    )

    @Test
    fun `formattedSize returns correct unit for bytes`() {
        val record = makeRecord(MediaType.SCREENSHOT, 512)
        assertEquals("512 B", record.formattedSize)
    }

    @Test
    fun `formattedSize returns correct unit for kilobytes`() {
        val record = makeRecord(MediaType.SCREENSHOT, 1_500)
        assertTrue(record.formattedSize.contains("KB"))
    }

    @Test
    fun `formattedSize returns correct unit for megabytes`() {
        val record = makeRecord(MediaType.RECORDING, 25_000_000)
        assertTrue(record.formattedSize.contains("MB"))
    }

    @Test
    fun `formattedSize returns correct unit for gigabytes`() {
        val record = makeRecord(MediaType.RECORDING, 2_000_000_000)
        assertTrue(record.formattedSize.contains("GB"))
    }

    @Test
    fun `formattedDuration returns empty string for screenshot`() {
        val record = makeRecord(MediaType.SCREENSHOT, 1000, durationMs = 0)
        assertEquals("", record.formattedDuration)
    }

    @Test
    fun `formattedDuration formats minutes and seconds correctly`() {
        val record = makeRecord(MediaType.RECORDING, 1000, durationMs = 125_000) // 2m 5s
        assertEquals("02:05", record.formattedDuration)
    }

    @Test
    fun `formattedDuration formats hours correctly`() {
        val record = makeRecord(MediaType.RECORDING, 1000, durationMs = 3_661_000) // 1h 1m 1s
        assertEquals("01:01:01", record.formattedDuration)
    }

    @Test
    fun `resolution returns correct string`() {
        val record = makeRecord(MediaType.RECORDING, 1000)
        assertEquals("1920x1080", record.resolution)
    }

    @Test
    fun `resolution returns empty when dimensions zero`() {
        val record = makeRecord(MediaType.SCREENSHOT, 1000).copy(width = 0, height = 0)
        assertEquals("", record.resolution)
    }
}
