package com.example.videodownloaderapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.videodownloaderapp.data.database.VideoDao
import com.example.videodownloaderapp.data.database.VideoDatabase
import com.example.videodownloaderapp.ui.components.RequestPermissions
import com.example.videodownloaderapp.ui.screens.DownloadScreen
import com.example.videodownloaderapp.ui.screens.GalleryScreen
import com.example.videodownloaderapp.ui.screens.HistoryScreen
import com.example.videodownloaderapp.ui.screens.VideoPlayerScreen
import com.example.videodownloaderapp.ui.theme.VideoDownloaderAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var database: VideoDatabase
    private lateinit var videoDao: VideoDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = VideoDatabase.getDatabase(this)
        videoDao = database.videoDao()

        lifecycleScope.launch {
            val videos = videoDao.getAllVideos()
        }

        enableEdgeToEdge()
        setContent {
            VideoDownloaderAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var permissionsGranted by remember { mutableStateOf(false) }

                    if (permissionsGranted) {
                        VideoDownloaderApp(videoDao = videoDao) // Pass videoDao here
                    } else {
                        RequestPermissions {
                            permissionsGranted = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        NavigationItem("download", "Download", R.drawable.ic_download),
        NavigationItem("gallery", "Gallery", R.drawable.ic_video_library),
        NavigationItem("history", "History", R.drawable.ic_history)
    )

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}

// Update NavigationItem data class
data class NavigationItem(
    val route: String,
    val title: String,
    val icon: Int
)

@Composable
fun NavigationHost(
    navController: NavHostController,
    videoDao: VideoDao, // Add this parameter
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "download",
        modifier = modifier
    ) {
        composable("download") {
            DownloadScreen(videoDao = videoDao)
        }
        composable("gallery") {
            GalleryScreen(navController = navController)
        }
        composable("history") {
            HistoryScreen(videoDao = videoDao) // Now videoDao is available
        }
        composable("player/{videoId}") { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
            VideoPlayerScreen(videoId = videoId, navController = navController)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDownloaderApp(videoDao: VideoDao) { // Add this parameter
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { paddingValues ->
        NavigationHost(
            navController = navController,
            videoDao = videoDao, // Pass it down to NavigationHost
            modifier = Modifier.padding(paddingValues)
        )
    }
}