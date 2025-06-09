package com.example.videodownloaderapp.di.database

import android.content.Context
import androidx.room.Room
import com.example.videodownloaderapp.data.database.VideoDao
import com.example.videodownloaderapp.data.database.VideoDatabase
import com.example.videodownloaderapp.data.repository.VideoRepository
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
    fun provideVideoDatabase(@ApplicationContext context: Context): VideoDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            VideoDatabase::class.java,
            "video_database"
        )
            .fallbackToDestructiveMigration() // For testing - removes this in production
            .build()
    }

    @Provides
    @Singleton
    fun provideVideoDao(database: VideoDatabase): VideoDao {
        return database.videoDao()
    }

    @Provides
    @Singleton
    fun provideVideoRepository(videoDao: VideoDao): VideoRepository {
        return VideoRepository(videoDao)
    }
}