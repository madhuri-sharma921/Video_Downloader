package com.example.videodownloaderapp

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.getInstance
import com.yausername.youtubedl_android.YoutubeDLException
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch



@HiltAndroidApp
class VideoDownloadApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            Aria2c.getInstance().init(this);
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "failed to initialize youtubedl-android", e)
        }
        // Alternative 1: Initialize without YoutubeDL for now
        Log.d("VideoDownloaderApp", "Application initialized successfully")

        // Alternative 2: Use a different video downloader library
        initializeVideoDownloader()
    }

    private fun initializeVideoDownloader() {
        applicationScope.launch {
            try {
                // Option 1: Use NewPipe Extractor (lighter alternative)
                // implementation 'com.github.TeamNewPipe:NewPipeExtractor:v0.22.1'

                // Option 2: Use custom implementation with OkHttp
                // implementation 'com.squareup.okhttp3:okhttp:4.12.0'

                // Option 3: Initialize youtubedl-android when imports work
                initializeYoutubeDLWhenAvailable()

            } catch (e: Exception) {
                Log.e("VideoDownloaderApp", "Error initializing video downloader: ${e.message}", e)
            }
        }
    }

    private fun initializeYoutubeDLWhenAvailable() {
        try {
            // This will only work after you've properly added the dependencies
            // and the imports are resolved

            // Uncomment these lines once imports work:
            // YoutubeDL.getInstance().init(applicationContext)
            // Log.d("VideoDownloaderApp", "YoutubeDL initialized successfully")

            Log.d("VideoDownloaderApp", "Ready to initialize YoutubeDL when imports are fixed")

        } catch (e: Exception) {
            Log.e("VideoDownloaderApp", "YoutubeDL not available: ${e.message}", e)
        }
    }
}

