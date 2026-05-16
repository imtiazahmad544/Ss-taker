package com.floatingscreen.domain.repository

import com.floatingscreen.domain.model.AppSettings
import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAllMedia(): Flow<List<MediaRecord>>
    fun getMediaByType(type: MediaType): Flow<List<MediaRecord>>
    fun searchMedia(query: String): Flow<List<MediaRecord>>
    suspend fun insertMedia(record: MediaRecord): Long
    suspend fun deleteMedia(id: Long): Boolean
    suspend fun deleteMediaFiles(id: Long): Boolean
    suspend fun renameMedia(id: Long, newName: String): Boolean
    suspend fun getMediaById(id: Long): MediaRecord?
    suspend fun getTotalStorageUsed(): Long
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun resetToDefaults()
}
