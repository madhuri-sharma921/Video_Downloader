package com.example.videodownloaderapp.domain

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.videodownloaderapp.VideoDownloadApplication
import com.example.videodownloaderapp.data.database.DownloadedVideo
import com.example.videodownloaderapp.data.repository.VideoRepository
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: VideoRepository
) {
    private val downloadDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        "VideoDownloader"
    )

    init {
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
    }

    suspend fun downloadVideo(url: String): Result<DownloadedVideo> = withContext(Dispatchers.IO) {
        try {
            Log.d("VideoDownloadManager", "🎯 downloadVideo() called for: $url")
            Log.d("VideoDownloadManager", "Starting download process for: $url")

            val videoId = UUID.randomUUID().toString()
            val fileName = "video_$videoId"
            val outputPath = File(downloadDir, "$fileName.%(ext)s").absolutePath

            // Get video info first
            Log.d("VideoDownloadManager", "Getting video info...")
            val infoRequest = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--no-playlist")
                addOption("--socket-timeout", "30")
            }

            val info = YoutubeDL.getInstance().execute(infoRequest, videoId)

            if (info.exitCode != 0) {
                Log.e("VideoDownloadManager", "Failed to get video info: ${info.err}")
                throw Exception("Failed to get video information: ${info.err}")
            }

            // Parse video information
            val title = extractTitle(info.out) ?: "Video_${System.currentTimeMillis()}"
            val duration = extractDuration(info.out) ?: 0L

            Log.d("VideoDownloadManager", "Video info retrieved - Title: $title, Duration: $duration")

            // Setup download request
            val downloadRequest = YoutubeDLRequest(url).apply {
                addOption("-o", outputPath)
                addOption("--no-playlist")
                addOption("--extract-flat", "false")
                addOption("--format", "best[height<=720]/best")
                addOption("--socket-timeout", "30")
                addOption("--retries", "3")
            }

            Log.d("VideoDownloadManager", "Starting download: $title")
            val downloadResult = YoutubeDL.getInstance().execute(downloadRequest, videoId)

            // Check if download was successful
            if (downloadResult.exitCode != 0) {
                Log.e("VideoDownloadManager", "Download failed with exit code: ${downloadResult.exitCode}")
                Log.e("VideoDownloadManager", "Error output: ${downloadResult.err}")
                throw Exception("Download failed: ${downloadResult.err}")
            }

            // Find the downloaded file
            val downloadedFiles = downloadDir.listFiles { file ->
                file.name.startsWith("video_$videoId") &&
                        (file.name.endsWith(".mp4") || file.name.endsWith(".webm") ||
                                file.name.endsWith(".mkv") || file.name.endsWith(".m4a") ||
                                file.name.endsWith(".flv") || file.name.endsWith(".avi"))
            }

            val downloadedFile = downloadedFiles?.firstOrNull()
                ?: throw Exception("Downloaded file not found in ${downloadDir.absolutePath}")

            Log.d("VideoDownloadManager", "Downloaded file found: ${downloadedFile.name}")

            val platform = detectPlatform(url)

            val downloadedVideo = DownloadedVideo(
                id = 0L,
                title = title,
                url = url,
                filePath = downloadedFile.absolutePath,
                duration = duration,
                fileSize = downloadedFile.length(),
                platform = platform
            )

            // Insert into Room database
            Log.d("VideoDownloadManager", "💾 About to insert video into database: ${downloadedVideo.title}")
            repository.insertVideo(downloadedVideo)
            Log.d("VideoDownloadManager", "✅ Video inserted into Room database successfully!")

            Log.d("VideoDownloadManager", "🎉 Download completed successfully: ${downloadedFile.absolutePath}")

            // IMPORTANT: Return the success result
            return@withContext Result.success(downloadedVideo)

        } catch (e: Exception) {
            Log.e("VideoDownloadManager", "❌ Download failed with exception", e)
            return@withContext Result.failure(e)
        }
    }

    private fun extractTitle(output: String): String? {
        return try {
            val patterns = listOf(
                "\"title\":\\s*\"([^\"]+)\"".toRegex(),
                "\"fulltitle\":\\s*\"([^\"]+)\"".toRegex(),
                "\"display_id\":\\s*\"([^\"]+)\"".toRegex()
            )

            for (pattern in patterns) {
                pattern.find(output)?.groupValues?.get(1)?.let { title ->
                    return title.replace("\\\"", "\"")
                        .replace("\\n", " ")
                        .replace("\\t", " ")
                        .trim()
                }
            }
            null
        } catch (e: Exception) {
            Log.e("VideoDownloadManager", "Error extracting title", e)
            null
        }
    }

    private fun extractDuration(output: String): Long? {
        return try {
            val durationRegex = "\"duration\":\\s*(\\d+(?:\\.\\d+)?)".toRegex()
            durationRegex.find(output)?.groupValues?.get(1)?.toDoubleOrNull()?.toLong()
        } catch (e: Exception) {
            Log.e("VideoDownloadManager", "Error extracting duration", e)
            null
        }
    }

    private fun detectPlatform(url: String): String {
        return when {
            url.contains("youtube.com", ignoreCase = true) ||
                    url.contains("youtu.be", ignoreCase = true) -> "youtube"
            url.contains("instagram.com", ignoreCase = true) -> "instagram"
            url.contains("tiktok.com", ignoreCase = true) -> "tiktok"
            url.contains("facebook.com", ignoreCase = true) -> "facebook"
            url.contains("twitter.com", ignoreCase = true) ||
                    url.contains("x.com", ignoreCase = true) -> "twitter"
            else -> "unknown"
        }
    }
}