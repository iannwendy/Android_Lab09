package com.example.lab09_exercise1.data

import java.io.File

enum class DownloadStatus {
    WAITING,
    DOWNLOADING,
    COMPLETE,
    FAILED
}

data class DownloadItem(
    val id: String,
    val url: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val status: DownloadStatus = DownloadStatus.WAITING,
    val filePath: String? = null,
    val errorMessage: String? = null
) {
    fun getProgress(): Float {
        return if (fileSize > 0) {
            (downloadedSize.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    
    fun getFormattedSize(): String {
        return formatBytes(fileSize)
    }
    
    fun getFileExtension(): String {
        return fileName.substringAfterLast('.', "").uppercase()
    }
    
    fun getIconResource(): String {
        val ext = getFileExtension()
        return when {
            ext in listOf("ZIP", "RAR", "7Z", "TAR", "GZ") -> "icon_archive"
            ext in listOf("TXT", "PDF", "DOC", "DOCX", "XLS", "XLSX", "PPT", "PPTX") -> {
                if (ext in listOf("DOC", "DOCX", "XLS", "XLSX", "PPT", "PPTX")) "icon_office" else "icon_text"
            }
            ext in listOf("MP3", "WAV", "AAC", "FLAC", "OGG", "M4A") -> "icon_music"
            ext in listOf("MP4", "AVI", "MKV", "MOV", "WMV", "FLV", "WEBM") -> "icon_movie"
            ext in listOf("JPG", "JPEG", "PNG", "GIF", "BMP", "WEBP") -> "icon_image"
            else -> "icon_other"
        }
    }
    
    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.2f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.2f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
}

