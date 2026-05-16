package com.floatingscreen.di

import android.content.Context
import androidx.room.Room
import com.floatingscreen.data.local.dao.MediaRecordDao
import com.floatingscreen.data.local.database.AppDatabase
import com.floatingscreen.data.repository.MediaRepositoryImpl
import com.floatingscreen.data.repository.SettingsRepositoryImpl
import com.floatingscreen.domain.repository.MediaRepository
import com.floatingscreen.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideMediaRecordDao(database: AppDatabase): MediaRecordDao =
        database.mediaRecordDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
