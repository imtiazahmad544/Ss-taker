package com.floatingscreen.data.repository

import com.floatingscreen.data.local.dao.MediaRecordDao
import com.floatingscreen.data.local.entity.MediaRecordEntity
import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType
import com.floatingscreen.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    private val dao: MediaRecordDao
) : MediaRepository {

    override fun getAllMedia(): Flow<List<MediaRecord>> =
        dao.getAllMedia().map { list -> list.map { it.toDomain() } }

    override fun getMediaByType(type: MediaType): Flow<List<MediaRecord>> =
        dao.getMediaByType(type.name).map { list -> list.map { it.toDomain() } }

    override fun searchMedia(query: String): Flow<List<MediaRecord>> =
        dao.searchMedia(query).map { list -> list.map { it.toDomain() } }

    override suspend fun insertMedia(record: MediaRecord): Long {
        return try {
            dao.insert(MediaRecordEntity.fromDomain(record))
        } catch (e: Exception) {
            Timber.e(e, "Failed to insert media record")
            -1L
        }
    }

    override suspend fun deleteMedia(id: Long): Boolean {
        return try {
            dao.deleteById(id)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete media record: $id")
            false
        }
    }

    override suspend fun deleteMediaFiles(id: Long): Boolean {
        return try {
            val entity = dao.getMediaById(id) ?: return false

            // Delete the actual file
            val file = File(entity.filePath)
            if (file.exists()) file.delete()

            // Delete thumbnail if exists
            entity.thumbnailPath?.let { thumbPath ->
                val thumb = File(thumbPath)
                if (thumb.exists()) thumb.delete()
            }

            dao.deleteById(id)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete media files for: $id")
            false
        }
    }

    override suspend fun renameMedia(id: Long, newName: String): Boolean {
        return try {
            val entity = dao.getMediaById(id) ?: return false
            val oldFile = File(entity.filePath)
            if (!oldFile.exists()) return false

            val newFile = File(oldFile.parent, newName)
            val renamed = oldFile.renameTo(newFile)

            if (renamed) {
                dao.renameMedia(id, newName)
            }
            renamed
        } catch (e: Exception) {
            Timber.e(e, "Failed to rename media: $id")
            false
        }
    }

    override suspend fun getMediaById(id: Long): MediaRecord? =
        dao.getMediaById(id)?.toDomain()

    override suspend fun getTotalStorageUsed(): Long =
        dao.getTotalStorageUsed() ?: 0L
}
