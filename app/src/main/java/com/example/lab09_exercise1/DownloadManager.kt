package com.example.lab09_exercise1

import android.content.Context
import android.os.Environment
import com.example.lab09_exercise1.data.DownloadItem
import com.example.lab09_exercise1.data.DownloadRepository
import com.example.lab09_exercise1.data.DownloadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val repository: DownloadRepository
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val activeDownloads = mutableMapOf<String, Boolean>()
    
    fun downloadFile(url: String, onProgress: (DownloadItem) -> Unit, onError: (String) -> Unit) {
        val scope = CoroutineScope(Dispatchers.IO)
        
        scope.launch {
            var downloadId: String? = null
            try {
                val fileName = extractFileName(url)
                val id = UUID.randomUUID().toString()
                downloadId = id
                
                val downloadItem = DownloadItem(
                    id = id,
                    url = url,
                    fileName = fileName,
                    status = DownloadStatus.WAITING
                )
                
                repository.addDownload(downloadItem)
                
                activeDownloads[id] = true
                
                val updatedItem = downloadItem.copy(status = DownloadStatus.DOWNLOADING)
                repository.updateDownload(updatedItem)
                withContext(Dispatchers.Main) {
                    onProgress(updatedItem)
                }
                
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
                
                val contentLength = response.body?.contentLength() ?: -1L
                val body = response.body ?: throw Exception("Response body is null")
                
                val downloadsDir = getDownloadsDirectory()
                // Handle duplicate file names
                var file = File(downloadsDir, fileName)
                var counter = 1
                while (file.exists() && activeDownloads[id] == true) {
                    val nameWithoutExt = fileName.substringBeforeLast('.', fileName)
                    val ext = fileName.substringAfterLast('.', "")
                    val newName = if (ext.isNotEmpty()) {
                        "${nameWithoutExt}_$counter.$ext"
                    } else {
                        "${nameWithoutExt}_$counter"
                    }
                    file = File(downloadsDir, newName)
                    counter++
                }
                
                var downloadedSize = 0L
                var lastUpdateTime = System.currentTimeMillis()
                
                body.byteStream().use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int = 0
                        
                        while (activeDownloads[id] == true) {
                            bytesRead = inputStream.read(buffer)
                            if (bytesRead == -1) break
                            
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead
                            
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastUpdateTime > 100) { // Update every 100ms
                                val currentItem = repository.getDownloadById(id)
                                if (currentItem != null) {
                                    val item = currentItem.copy(
                                        fileSize = contentLength,
                                        downloadedSize = downloadedSize,
                                        fileName = file.name
                                    )
                                    repository.updateDownload(item)
                                    withContext(Dispatchers.Main) {
                                        onProgress(item)
                                    }
                                    lastUpdateTime = currentTime
                                }
                            }
                        }
                    }
                }
                
                if (activeDownloads[id] == true) {
                    val currentItem = repository.getDownloadById(id)
                    if (currentItem != null) {
                        val finalItem = currentItem.copy(
                            fileSize = contentLength,
                            downloadedSize = downloadedSize,
                            status = DownloadStatus.COMPLETE,
                            filePath = file.absolutePath,
                            fileName = file.name
                        )
                        repository.updateDownload(finalItem)
                        withContext(Dispatchers.Main) {
                            onProgress(finalItem)
                        }
                    }
                } else {
                    file.delete()
                }
                
            } catch (e: Exception) {
                downloadId?.let { id ->
                    val failedItem = repository.getDownloadById(id)
                    if (failedItem != null) {
                        val updatedItem = failedItem.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = e.message ?: "Unknown error"
                        )
                        repository.updateDownload(updatedItem)
                        withContext(Dispatchers.Main) {
                            onProgress(updatedItem)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Download failed")
                }
            } finally {
                downloadId?.let { activeDownloads.remove(it) }
            }
        }
    }
    
    fun retryDownload(downloadItem: DownloadItem, onProgress: (DownloadItem) -> Unit, onError: (String) -> Unit) {
        val updatedItem = downloadItem.copy(
            status = DownloadStatus.WAITING,
            errorMessage = null,
            downloadedSize = 0L
        )
        repository.updateDownload(updatedItem)
        downloadFile(downloadItem.url, onProgress, onError)
    }
    
    fun cancelDownload(id: String) {
        activeDownloads[id] = false
    }
    
    private fun extractFileName(url: String): String {
        return try {
            val urlObj = URL(url)
            val path = urlObj.path
            if (path.isNotEmpty() && path != "/") {
                val fileName = path.substringAfterLast('/')
                if (fileName.isNotEmpty()) {
                    fileName
                } else {
                    "download_${System.currentTimeMillis()}"
                }
            } else {
                "download_${System.currentTimeMillis()}"
            }
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }
    
    private fun getDownloadsDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }
}

