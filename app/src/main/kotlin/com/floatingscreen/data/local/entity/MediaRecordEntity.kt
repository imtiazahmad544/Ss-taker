package com.floatingscreen.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType

@Entity(tableName = "media_records")
data class MediaRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val mediaType: String, // "RECORDING" or "SCREENSHOT"
    val timestamp: Long,
    val durationMs: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val fileSizeBytes: Long = 0,
    val thumbnailPath: String? = null
) {
    fun toDomain(): MediaRecord = MediaRecord(
        id = id,
        fileName = fileName,
        filePath = filePath,
        mediaType = MediaType.valueOf(mediaType),
        timestamp = timestamp,
        durationMs = durationMs,
        width = width,
        height = height,
        fileSizeBytes = fileSizeBytes,
        thumbnailPath = thumbnailPath
    )

    companion object {
        fun fromDomain(record: MediaRecord): MediaRecordEntity = MediaRecordEntity(
            id = record.id,
            fileName = record.fileName,
            filePath = record.filePath,
            mediaType = record.mediaType.name,
            timestamp = record.timestamp,
            durationMs = record.durationMs,
            width = record.width,
            height = record.height,
            fileSizeBytes = record.fileSizeBytes,
            thumbnailPath = record.thumbnailPath
        )
    }
}
