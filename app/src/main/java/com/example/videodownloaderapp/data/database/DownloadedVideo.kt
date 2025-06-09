package com.example.videodownloaderapp.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloaded_videos")
data class DownloadedVideo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val url: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val duration: Long = 0,
    val fileSize: Long = 0,
    val downloadDate: Long = System.currentTimeMillis(),
    val platform: String,

    )