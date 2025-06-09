package com.example.videodownloaderapp.data.repository

import android.util.Log
import com.example.videodownloaderapp.data.database.DownloadedVideo
import com.example.videodownloaderapp.data.database.VideoDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoRepository @Inject constructor(
    private val videoDao: VideoDao
) {

    fun getAllVideos(): Flow<List<DownloadedVideo>> {
        Log.d("VideoRepository", "🔄 Getting all videos from database...")
        return videoDao.getAllVideos()
    }

    suspend fun insertVideo(video: DownloadedVideo) {
        Log.d("VideoRepository", "🔄 Inserting video: ${video.title}")
        videoDao.insertVideo(video)
        Log.d("VideoRepository", "✅ Video inserted successfully")

        // Debug: Check count after insert
        val count = videoDao.getVideoCount()
        Log.d("VideoRepository", "📊 Total videos in database: $count")
    }

    suspend fun deleteVideo(video: DownloadedVideo) {
        Log.d("VideoRepository", "🔄 Deleting video: ${video.title}")
        videoDao.deleteVideo(video)
        Log.d("VideoRepository", "✅ Video deleted successfully")
    }

    suspend fun getVideoCount(): Int {
        return videoDao.getVideoCount()
    }
}