package com.example.videodownloaderapp.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

import com.google.accompanist.permissions.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestPermissions(
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.INTERNET
        )
    } else {
        listOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        )
    }

    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    if (!multiplePermissionsState.allPermissionsGranted) {
        PermissionRequestDialog(
            multiplePermissionsState = multiplePermissionsState
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestDialog(
    multiplePermissionsState: MultiplePermissionsState
) {
    AlertDialog(
        onDismissRequest = { /* Cannot dismiss */ },
        title = { Text("Permissions Required") },
        text = {
            Text("This app needs storage permissions to download and save videos. Please grant the required permissions to continue.")
        },
        confirmButton = {
            TextButton(
                onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }
            ) {
                Text("Grant Permissions")
            }
        }
    )
}
