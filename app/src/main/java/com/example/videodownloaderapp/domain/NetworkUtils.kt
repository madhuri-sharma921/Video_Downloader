package com.example.videodownloaderapp.domain

object NetworkUtils {
    fun isValidVideoUrl(url: String): Boolean {
        val supportedDomains = listOf(
            "youtube.com", "youtu.be", "instagram.com",
            "tiktok.com", "facebook.com", "twitter.com"
        )

        return try {
            val lowerUrl = url.lowercase()
            supportedDomains.any { domain -> lowerUrl.contains(domain) }
        } catch (e: Exception) {
            false
        }
    }

    fun extractVideoId(url: String): String {
        return when {
            url.contains("youtube.com") -> {
                url.substringAfter("v=").substringBefore("&")
            }
            url.contains("youtu.be") -> {
                url.substringAfterLast("/").substringBefore("?")
            }
            url.contains("instagram.com") -> {
                url.substringAfter("/p/").substringBefore("/")
            }
            else -> url.hashCode().toString()
        }
    }
}


data class DownloadProgress(
    val videoId: String,
    val progress: Float = 0f,
    val status: DownloadStatus = DownloadStatus.IDLE,
    val error: String? = null
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}