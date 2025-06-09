package com.example.videodownloaderapp.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM downloaded_videos ORDER BY downloadDate DESC")
    fun getAllVideos(): Flow<List<DownloadedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: DownloadedVideo)

    @Delete
    suspend fun deleteVideo(video: DownloadedVideo)

    @Query("SELECT COUNT(*) FROM downloaded_videos")
    suspend fun getVideoCount(): Int
}