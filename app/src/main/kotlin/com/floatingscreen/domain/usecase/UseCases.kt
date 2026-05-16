package com.floatingscreen.domain.usecase

import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType
import com.floatingscreen.domain.repository.MediaRepository
import com.floatingscreen.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(): Flow<List<MediaRecord>> = repository.getAllMedia()
}

class GetMediaByTypeUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(type: MediaType): Flow<List<MediaRecord>> = repository.getMediaByType(type)
}

class SearchMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    operator fun invoke(query: String): Flow<List<MediaRecord>> = repository.searchMedia(query)
}

class DeleteMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: Long): Boolean = repository.deleteMediaFiles(id)
}

class RenameMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(id: Long, newName: String): Boolean =
        repository.renameMedia(id, newName)
}

class InsertMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(record: MediaRecord): Long = repository.insertMedia(record)
}

class GetSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    operator fun invoke() = repository.getSettings()
}

class UpdateSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(settings: com.floatingscreen.domain.model.AppSettings) =
        repository.updateSettings(settings)
}

class GetStorageUsedUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(): Long = repository.getTotalStorageUsed()
}
