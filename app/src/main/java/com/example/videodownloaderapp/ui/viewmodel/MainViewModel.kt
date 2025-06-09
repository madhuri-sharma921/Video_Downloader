package com.example.videodownloaderapp.ui.viewmodel

import android.util.Log
import kotlinx.coroutines.flow.catch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videodownloaderapp.data.database.DownloadedVideo
import com.example.videodownloaderapp.data.database.VideoDao
import com.example.videodownloaderapp.data.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    private val _downloadedVideos = MutableStateFlow<List<DownloadedVideo>>(emptyList())
    val downloadedVideos: StateFlow<List<DownloadedVideo>> = _downloadedVideos.asStateFlow()

    init {
        Log.d("MainViewModel", "🏗️ MainViewModel created, loading videos...")
        loadVideos()

    }

    private fun loadVideos() {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "🔄 Loading videos from repository...")

                repository.getAllVideos()
                    .catch { exception ->
                        Log.e("MainViewModel", "❌ Flow collection error", exception)
                        _downloadedVideos.value = emptyList()
                    }
                    .collect { videoList ->
                        Log.d("MainViewModel", "📹 Received ${videoList.size} videos from database")
                        videoList.forEachIndexed { index, video ->
                            Log.d("MainViewModel", "   ${index + 1}. ${video.title} (${video.platform})")
                        }
                        _downloadedVideos.value = videoList
                    }

            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Error loading videos", e)
                _downloadedVideos.value = emptyList()
            }
        }
    }

    fun insertVideo(video: DownloadedVideo) {
        viewModelScope.launch {
            try {
                Log.d("MainViewModel", "🔄 Inserting video: ${video.title}")
                repository.insertVideo(video)
                Log.d("MainViewModel", "✅ Video insertion completed")

            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Failed to insert video", e)
            }
        }
    }

    fun deleteVideo(video: DownloadedVideo) {
        viewModelScope.launch {
            try {
                repository.deleteVideo(video)
                // Delete the actual file
                try {
                    File(video.filePath).delete()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Failed to delete file", e)
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "❌ Failed to delete video", e)
            }
        }
    }

    fun refreshVideos() {
        Log.d("MainViewModel", "🔄 Manual refresh requested")
        loadVideos()
    }
}