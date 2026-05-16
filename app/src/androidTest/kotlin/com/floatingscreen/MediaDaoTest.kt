package com.floatingscreen

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.floatingscreen.data.local.dao.MediaRecordDao
import com.floatingscreen.data.local.database.AppDatabase
import com.floatingscreen.data.local.entity.MediaRecordEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MediaRecordDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.mediaRecordDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndRetrieve() = runBlocking {
        val entity = MediaRecordEntity(
            fileName = "test.mp4",
            filePath = "/path/test.mp4",
            mediaType = "RECORDING",
            timestamp = System.currentTimeMillis(),
            durationMs = 60_000,
            width = 1920,
            height = 1080,
            fileSizeBytes = 25_000_000
        )
        val id = dao.insert(entity)
        assertTrue(id > 0)

        val retrieved = dao.getMediaById(id)
        assertNotNull(retrieved)
        assertEquals("test.mp4", retrieved!!.fileName)
        assertEquals("RECORDING", retrieved.mediaType)
    }

    @Test
    fun getAllMediaReturnsAllItems() = runBlocking {
        repeat(5) { i ->
            dao.insert(
                MediaRecordEntity(
                    fileName = "file_$i.mp4",
                    filePath = "/path/file_$i.mp4",
                    mediaType = if (i % 2 == 0) "RECORDING" else "SCREENSHOT",
                    timestamp = System.currentTimeMillis() + i,
                    fileSizeBytes = 1000L * (i + 1)
                )
            )
        }
        val all = dao.getAllMedia().first()
        assertEquals(5, all.size)
    }

    @Test
    fun deleteByIdRemovesRecord() = runBlocking {
        val id = dao.insert(
            MediaRecordEntity(
                fileName = "delete_me.mp4",
                filePath = "/path/delete_me.mp4",
                mediaType = "RECORDING",
                timestamp = System.currentTimeMillis()
            )
        )
        dao.deleteById(id)
        assertNull(dao.getMediaById(id))
    }

    @Test
    fun renameUpdatesFileName() = runBlocking {
        val id = dao.insert(
            MediaRecordEntity(
                fileName = "old_name.mp4",
                filePath = "/path/old_name.mp4",
                mediaType = "RECORDING",
                timestamp = System.currentTimeMillis()
            )
        )
        dao.renameMedia(id, "new_name.mp4")
        val updated = dao.getMediaById(id)
        assertEquals("new_name.mp4", updated?.fileName)
    }

    @Test
    fun searchMatchesFileName() = runBlocking {
        dao.insert(
            MediaRecordEntity(
                fileName = "vacation_2024.mp4",
                filePath = "/path/vacation_2024.mp4",
                mediaType = "RECORDING",
                timestamp = System.currentTimeMillis()
            )
        )
        dao.insert(
            MediaRecordEntity(
                fileName = "birthday.png",
                filePath = "/path/birthday.png",
                mediaType = "SCREENSHOT",
                timestamp = System.currentTimeMillis()
            )
        )
        val results = dao.searchMedia("vacation").first()
        assertEquals(1, results.size)
        assertEquals("vacation_2024.mp4", results[0].fileName)
    }

    @Test
    fun totalStorageIsSum() = runBlocking {
        dao.insert(
            MediaRecordEntity("a.mp4", "/a.mp4", "RECORDING", 0, fileSizeBytes = 1000)
        )
        dao.insert(
            MediaRecordEntity("b.png", "/b.png", "SCREENSHOT", 0, fileSizeBytes = 2000)
        )
        val total = dao.getTotalStorageUsed()
        assertEquals(3000L, total)
    }
}
