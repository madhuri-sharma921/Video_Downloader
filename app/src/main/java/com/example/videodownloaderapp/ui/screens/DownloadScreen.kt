package com.example.videodownloaderapp.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videodownloaderapp.data.database.DownloadedVideo
import com.example.videodownloaderapp.data.database.VideoDao
import com.example.videodownloaderapp.ui.viewmodel.MainViewModel
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.launch
import java.io.File
import kotlin.Long

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadScreen(
    viewModel: DownloadingViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
    videoDao: VideoDao // Add this parameter
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Permission launcher for older Android versions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startDownload(context, mainViewModel, videoDao) // Pass videoDao

        } else {
            Toast.makeText(
                context,
                "Storage permission is required for older Android versions",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // URL Input
        OutlinedTextField(
            value = uiState.url,
            onValueChange = viewModel::updateUrl,
            label = { Text("Video URL (YouTube, Vimeo, etc.)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            isError = uiState.urlError.isNotEmpty(),
            supportingText = if (uiState.urlError.isNotEmpty()) {
                { Text(uiState.urlError) }
            } else null,
            placeholder = { Text("https://www.youtube.com/watch?v=...") }
        )

        // Config File Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Use Config File")
            Switch(
                checked = uiState.useConfigFile,
                onCheckedChange = viewModel::updateUseConfigFile
            )
        }

        // Initialization Status
        if (uiState.isInitializing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Initializing downloader...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Download Progress
        if (uiState.isDownloading) {
            Column {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${uiState.progress.toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Status Text
        if (uiState.statusText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.statusText,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Command Output
        if (uiState.commandOutput.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Output:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.commandOutput,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Download Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                            viewModel.startDownload(context, mainViewModel, videoDao) // Pass videoDao
                        }
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                            if (isStoragePermissionGranted(context)) {
                                viewModel.startDownload(context, mainViewModel, videoDao) // Pass videoDao
                                // REMOVED: videoDao.insert(video) - this was wrong here
                            } else {
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        }
                        else -> {
                            viewModel.startDownload(context, mainViewModel, videoDao) // Pass videoDao
                        }
                    }
                },
                enabled = !uiState.isDownloading && !uiState.isInitializing,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (uiState.isInitializing) "Initializing..." else "Start Download")
            }

            Button(
                onClick = { viewModel.stopDownload() },
                enabled = uiState.isDownloading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Download")
            }
        }

        // Info text
        Text(
            text = "Note: Downloads will be saved to app's internal Downloads folder",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ViewModel for managing state
class DownloadingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadingUiState())
    val uiState: StateFlow<DownloadingUiState> = _uiState.asStateFlow()

    private val compositeDisposable = CompositeDisposable()
    private var processId: String? = null
    private var isLibraryInitialized = false

    private val callback = { progress: Float, etaInSeconds: Long, line: String ->
        Log.d("DownloadingViewModel", "Progress: $progress%, ETA: ${etaInSeconds}s, Line: $line")
        _uiState.value = _uiState.value.copy(
            progress = progress,
            statusText = if (line.isNotEmpty()) line else "Downloading... ${progress.toInt()}%"
        )
        Unit
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(
            url = url,
            urlError = ""
        )
    }

    fun updateUseConfigFile(useConfig: Boolean) {
        _uiState.value = _uiState.value.copy(useConfigFile = useConfig)
    }

    private fun addToHistory(url: String, title: String, filePath: String, status: DownloadStatus) {
        val historyItem = DownloadHistoryItem(
            url = url,
            title = title,
            filePath = filePath,
            timestamp = System.currentTimeMillis(),
            status = status
        )

        // Here you would typically save to a shared data source or database
        // For now, we'll just add to the local state
        val updatedHistory = _uiState.value.downloadHistory.toMutableList()
        updatedHistory.removeAll { it.url == url }
        updatedHistory.add(historyItem)

        _uiState.value = _uiState.value.copy(downloadHistory = updatedHistory)
    }

    fun startDownload(context: Context, mainViewModel: MainViewModel, videoDao: VideoDao) { // Add videoDao parameter
        val currentState = _uiState.value

        if (currentState.isDownloading || currentState.isInitializing) {
            Log.d("DownloadingViewModel", "Download already in progress, ignoring request")
            return
        }

        val url = currentState.url.trim()
        if (url.isEmpty()) {
            _uiState.value = currentState.copy(urlError = "Please enter a valid URL")
            return
        }

        if (!isValidUrl(url)) {
            _uiState.value = currentState.copy(urlError = "Please enter a valid video URL")
            return
        }

        Log.d("DownloadingViewModel", "Starting download for URL: $url")

        if (!isLibraryInitialized) {
            initializeLibraries(context, url, mainViewModel, videoDao) // Pass videoDao
        } else {
            performDownload(context, url, mainViewModel, videoDao) // Pass videoDao
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    private fun initializeLibraries(context: Context, url: String, mainViewModel: MainViewModel, videoDao: VideoDao) { // Add videoDao parameter
        _uiState.value = _uiState.value.copy(
            isInitializing = true,
            statusText = "Initializing downloader libraries..."
        )

        Log.d("DownloadingViewModel", "Initializing YoutubeDL and FFmpeg")

        val initDisposable = Observable.fromCallable {
            try {
                YoutubeDL.getInstance().init(context)
                Log.d("DownloadingViewModel", "YoutubeDL initialized successfully")

                try {
                    FFmpeg.getInstance().init(context)
                    Log.d("DownloadingViewModel", "FFmpeg initialized successfully")
                } catch (e: Exception) {
                    Log.w("DownloadingViewModel", "FFmpeg initialization failed, continuing without it", e)
                }

                isLibraryInitialized = true
                true
            } catch (e: Exception) {
                Log.e("DownloadingViewModel", "Library initialization failed", e)
                throw e
            }
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ success ->
                Log.d("DownloadingViewModel", "Initialization successful, starting download")
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    statusText = "Libraries initialized, starting download..."
                )
                performDownload(context, url, mainViewModel, videoDao) // Pass videoDao
            }, { e ->
                Log.e("DownloadingViewModel", "Failed to initialize libraries", e)
                _uiState.value = _uiState.value.copy(
                    isInitializing = false,
                    statusText = "Initialization failed",
                    commandOutput = "Initialization Error: ${e.message}\n\nTroubleshooting:\n" +
                            "• Ensure youtubedl-android dependencies are properly added\n" +
                            "• Check if android:extractNativeLibs=\"true\" is set in manifest\n" +
                            "• Verify app has internet permission\n" +
                            "• Try restarting the app",
                    isDownloading = false
                )
            })

        compositeDisposable.add(initDisposable)
    }

    private fun performDownload(context: Context, url: String, mainViewModel: MainViewModel, videoDao: VideoDao) { // Add videoDao parameter
        processId = "download_${System.currentTimeMillis()}"

        val downloadDir = getDownloadLocation(context)

        Log.d("DownloadingViewModel", "Download directory: ${downloadDir.absolutePath}")
        Log.d("DownloadingViewModel", "Process ID: $processId")

        addToHistory(url, "", downloadDir.absolutePath, DownloadStatus.IN_PROGRESS)

        _uiState.value = _uiState.value.copy(
            isDownloading = true,
            isLoading = true,
            statusText = "Preparing download...",
            progress = 0f,
            commandOutput = "Starting download to: ${downloadDir.absolutePath}"
        )

        // Try with different format options
        tryDownloadWithFormats(context, url, downloadDir, 0, mainViewModel, videoDao) // Pass videoDao
    }

    private fun tryDownloadWithFormats(context: Context, url: String, downloadDir: File, attemptIndex: Int, mainViewModel: MainViewModel, videoDao: VideoDao) { // Add videoDao parameter
        val formatOptions = listOf(
            "best[ext=mp4]/bestvideo[ext=mp4]+bestaudio[ext=m4a]/bestvideo+bestaudio/best",
            "best[height<=720][ext=mp4]/best[height<=720]",
            "bestvideo[height<=720]+bestaudio/best[height<=720]",
            "mp4/best"
        )

        if (attemptIndex >= formatOptions.size) {
            // All format attempts failed
            Log.e("DownloadingViewModel", "All format attempts failed")
            addToHistory(url, "Failed Download", "", DownloadStatus.FAILED)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                statusText = "Download failed - no compatible format found",
                commandOutput = "❌ All format attempts failed\n\nTroubleshooting:\n" +
                        "• Video may be region-locked or private\n" +
                        "• Try a different video URL\n" +
                        "• Check if video is age-restricted\n" +
                        "• Some videos may not be downloadable",
                isDownloading = false
            )
            processId = null
            return
        }

        val currentFormat = formatOptions[attemptIndex]
        Log.d("DownloadingViewModel", "Trying format: $currentFormat (attempt ${attemptIndex + 1}/${formatOptions.size})")

        val request = YoutubeDLRequest(url)
        setupDownloadOptionsWithFormat(request, downloadDir, currentFormat)

        val disposable = Observable.fromCallable {
            Log.d("DownloadingViewModel", "Executing YoutubeDL request with format: $currentFormat")
            val response = YoutubeDL.getInstance().execute(request, processId!!, callback)
            Log.d("DownloadingViewModel", "YoutubeDL execution completed successfully")
            response
        }
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ youtubeDLResponse ->
                Log.d("DownloadingViewModel", "Download completed successfully with format: $currentFormat")

                val videoTitle = extractVideoTitle(youtubeDLResponse.out) ?: "Downloaded Video"
                val filePath = "${downloadDir.absolutePath}/$videoTitle"

                // THIS IS WHERE YOU SAVE TO DATABASE - AFTER SUCCESSFUL DOWNLOAD
                val downloadedVideo = DownloadedVideo(
                    id = 0,
                    title = videoTitle,
                    url = url,
                    filePath = filePath,
                    thumbnailPath = null,
                    duration = 0,
                    fileSize = 0L,
                    downloadDate = System.currentTimeMillis(),
                    platform = "",
                )

                // SAVE TO DATABASE HERE
                viewModelScope.launch {
                    try {
                        videoDao.insertVideo(downloadedVideo)
                        Log.d("DownloadingViewModel", "Video saved to database successfully")
                    } catch (e: Exception) {
                        Log.e("DownloadingViewModel", "Failed to save video to database", e)
                    }
                }

                addToHistory(url, videoTitle, filePath, DownloadStatus.SUCCESS)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    progress = 100f,
                    statusText = "Download completed successfully!",
                    commandOutput = "✅ Download finished!\n\nFormat used: $currentFormat\nSaved to: ${downloadDir.absolutePath}\n\nOutput:\n${youtubeDLResponse.out}",
                    isDownloading = false
                )
                processId = null
            }, { e ->
                Log.e("DownloadingViewModel", "Download failed with format $currentFormat", e)

                if (attemptIndex < formatOptions.size - 1) {
                    // Try next format
                    Log.d("DownloadingViewModel", "Retrying with next format...")
                    _uiState.value = _uiState.value.copy(
                        statusText = "Format failed, trying alternative... (${attemptIndex + 2}/${formatOptions.size})"
                    )
                    tryDownloadWithFormats(context, url, downloadDir, attemptIndex + 1, mainViewModel, videoDao) // Pass videoDao
                } else {
                    // All attempts failed
                    val errorMessage = when (e) {
                        is YoutubeDLException -> "YoutubeDL Error: ${e.message}"
                        else -> "Download Error: ${e.message ?: "Unknown error"}"
                    }

                    addToHistory(url, "Failed Download", "", DownloadStatus.FAILED)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusText = "Download failed",
                        commandOutput = "❌ $errorMessage\n\nAll ${formatOptions.size} format attempts failed.\n\nTroubleshooting:\n" +
                                "• Check internet connection\n" +
                                "• Video may be private, deleted, or region-blocked\n" +
                                "• Try a different video URL\n" +
                                "• Some videos may require login or have restrictions",
                        isDownloading = false
                    )
                    processId = null
                }
            })

        compositeDisposable.add(disposable)
    }

    // Rest of your code remains the same...
    private fun setupDownloadOptionsWithFormat(request: YoutubeDLRequest, downloadDir: File, format: String) {
        Log.d("DownloadingViewModel", "Setting up download with format: $format")

        request.addOption("--no-mtime")
        request.addOption("--no-warnings")
        request.addOption("--ignore-errors")
        request.addOption("--no-cache-dir")

        // Use the specified format
        request.addOption("-f", format)

        // Simpler output template
        request.addOption("-o", "${downloadDir.absolutePath}/%(title).100s.%(ext)s")

        // Network and retry options
        request.addOption("--socket-timeout", "30")
        request.addOption("--retries", "3")
        request.addOption("--fragment-retries", "3")

        // YouTube specific options
        request.addOption("--no-check-certificate")
        request.addOption("--no-playlist")

        // Better user agent
        request.addOption("--user-agent", "Mozilla/5.0 (Linux; Android 11; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
    }

    private fun extractVideoTitle(output: String): String? {
        return try {
            // Multiple patterns to extract video title
            val patterns = listOf(
                "\\[download\\] Destination: .*/(.+?)\\.[a-zA-Z0-9]+$".toRegex(RegexOption.MULTILINE),
                "\\[download\\] (.+?) has already been downloaded".toRegex(),
                "\"title\":\\s*\"([^\"]+)\"".toRegex(),
                "Downloading: (.+)$".toRegex(RegexOption.MULTILINE)
            )

            for (pattern in patterns) {
                val match = pattern.find(output)
                if (match != null && match.groupValues.size > 1) {
                    val title = match.groupValues[1]
                        .replace(Regex("[^a-zA-Z0-9\\s\\-_.()]"), "") // Clean special chars
                        .trim()
                    if (title.isNotEmpty()) {
                        Log.d("DownloadingViewModel", "Extracted title: $title")
                        return title
                    }
                }
            }

            // Fallback: try to extract from any line with "title"
            val fallbackRegex = ".*title.*[\"']([^\"']+)[\"']".toRegex(RegexOption.IGNORE_CASE)
            val fallbackMatch = fallbackRegex.find(output)
            fallbackMatch?.groupValues?.get(1)?.trim()

        } catch (e: Exception) {
            Log.w("DownloadingViewModel", "Could not extract video title", e)
            null
        }
    }

    private fun setupDownloadOptions(request: YoutubeDLRequest, downloadDir: File) {
        val config = File(downloadDir, "config.txt")

        if (_uiState.value.useConfigFile && config.exists()) {
            Log.d("DownloadingViewModel", "Using config file: ${config.absolutePath}")
            request.addOption("--config-location", config.absolutePath)
        } else {
            Log.d("DownloadingViewModel", "Using default download options")

            request.addOption("--no-mtime")
            request.addOption("--no-warnings")
            request.addOption("--ignore-errors")
            request.addOption("--no-cache-dir")

            // More flexible format selection - start with most compatible
            request.addOption("-f", "best[height<=720][ext=mp4]/best[height<=480][ext=mp4]/best[ext=mp4]/mp4/best")

            // Alternative: if the above fails, try these fallbacks
            request.addOption("--format-sort", "height:720,ext:mp4:m4a")

            // Simpler output template to avoid filename issues
            request.addOption("-o", "${downloadDir.absolutePath}/%(title).100s.%(ext)s")

            // Network and retry options
            request.addOption("--socket-timeout", "30")
            request.addOption("--retries", "5")
            request.addOption("--fragment-retries", "5")

            // YouTube specific options
            request.addOption("--no-check-certificate")
            request.addOption("--prefer-insecure")

            // Extract flat to get better format info
            request.addOption("--no-playlist")

            // User agent - use a more recent one
            request.addOption("--user-agent", "Mozilla/5.0 (Linux; Android 11; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")

            // Add cookies support for better access
            request.addOption("--cookies-from-browser", "chrome")
        }
    }

    fun stopDownload() {
        Log.d("DownloadingViewModel", "Stop download requested")

        processId?.let { id ->
            try {
                Log.d("DownloadingViewModel", "Destroying process: $id")
                YoutubeDL.getInstance().destroyProcessById(id)
                Log.d("DownloadingViewModel", "Process destroyed successfully")

                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    isLoading = false,
                    statusText = "Download stopped by user",
                    commandOutput = _uiState.value.commandOutput + "\n\n⏹️ Download stopped by user"
                )
                processId = null
            } catch (e: Exception) {
                Log.e("DownloadingViewModel", "Error stopping download", e)
                _uiState.value = _uiState.value.copy(
                    statusText = "Error stopping download: ${e.message}",
                    commandOutput = _uiState.value.commandOutput + "\n\n❌ Error stopping: ${e.message}"
                )
            }
        } ?: run {
            Log.w("DownloadingViewModel", "No active download to stop")
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                isLoading = false,
                statusText = "No active download to stop"
            )
        }
    }

    private fun getDownloadLocation(context: Context): File {
        val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "VideoDownloader")

        if (!downloadDir.exists()) {
            val created = downloadDir.mkdirs()
            Log.d("DownloadingViewModel", "Created download directory: ${downloadDir.absolutePath}, success: $created")

            if (!downloadDir.canWrite()) {
                Log.w("DownloadingViewModel", "Download directory is not writable")
            }
        }

        try {
            val testFile = File(downloadDir, "test.txt")
            testFile.writeText("test")
            testFile.delete()
            Log.d("DownloadingViewModel", "Directory write test successful")
        } catch (e: Exception) {
            Log.e("DownloadingViewModel", "Directory write test failed", e)
        }

        return downloadDir
    }

    override fun onCleared() {
        Log.d("DownloadingViewModel", "ViewModel cleared, disposing composites")
        compositeDisposable.dispose()
        super.onCleared()
    }
}

// UI State data class
data class DownloadingUiState(
    val url: String = "",
    val urlError: String = "",
    val useConfigFile: Boolean = false,
    val isDownloading: Boolean = false,
    val isLoading: Boolean = false,
    val isInitializing: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val commandOutput: String = "",
    val downloadHistory: List<DownloadHistoryItem> = emptyList()
)

// Download history item
data class DownloadHistoryItem(
    val url: String,
    val title: String,
    val filePath: String,
    val timestamp: Long,
    val status: DownloadStatus
)

enum class DownloadStatus {
    SUCCESS, FAILED, IN_PROGRESS
}

// Helper function for permission check
private fun isStoragePermissionGranted(context: Context): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> true
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        else -> true
    }
}