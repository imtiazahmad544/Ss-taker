package com.floatingscreen.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.floatingscreen.data.local.dao.MediaRecordDao
import com.floatingscreen.data.local.entity.MediaRecordEntity

@Database(
    entities = [MediaRecordEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaRecordDao(): MediaRecordDao

    companion object {
        const val DATABASE_NAME = "floating_screen_db"
    }
}
