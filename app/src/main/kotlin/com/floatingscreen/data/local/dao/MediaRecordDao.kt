package com.floatingscreen.data.local.dao

import androidx.room.*
import com.floatingscreen.data.local.entity.MediaRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaRecordDao {

    @Query("SELECT * FROM media_records ORDER BY timestamp DESC")
    fun getAllMedia(): Flow<List<MediaRecordEntity>>

    @Query("SELECT * FROM media_records WHERE mediaType = :type ORDER BY timestamp DESC")
    fun getMediaByType(type: String): Flow<List<MediaRecordEntity>>

    @Query("SELECT * FROM media_records WHERE fileName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchMedia(query: String): Flow<List<MediaRecordEntity>>

    @Query("SELECT * FROM media_records WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: Long): MediaRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MediaRecordEntity): Long

    @Delete
    suspend fun delete(entity: MediaRecordEntity)

    @Query("DELETE FROM media_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE media_records SET fileName = :newName WHERE id = :id")
    suspend fun renameMedia(id: Long, newName: String)

    @Query("SELECT SUM(fileSizeBytes) FROM media_records")
    suspend fun getTotalStorageUsed(): Long?

    @Query("SELECT COUNT(*) FROM media_records WHERE mediaType = :type")
    suspend fun getCountByType(type: String): Int
}
