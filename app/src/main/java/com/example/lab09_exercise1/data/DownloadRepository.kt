package com.example.lab09_exercise1.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DownloadRepository(private val context: Context) {
    private val gson = Gson()
    private val prefs: SharedPreferences = context.getSharedPreferences("downloads_prefs", Context.MODE_PRIVATE)
    private val downloadsKey = "downloads_list"
    
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()
    
    init {
        try {
            loadDownloads()
        } catch (e: Exception) {
            // If loading fails, start with empty list
            _downloads.value = emptyList()
            e.printStackTrace()
        }
    }
    
    fun addDownload(downloadItem: DownloadItem) {
        val currentList = _downloads.value.toMutableList()
        currentList.add(0, downloadItem) // Add to top
        _downloads.value = currentList
        saveDownloads()
    }
    
    fun updateDownload(downloadItem: DownloadItem) {
        val currentList = _downloads.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == downloadItem.id }
        if (index != -1) {
            currentList[index] = downloadItem
            _downloads.value = currentList
            saveDownloads()
        }
    }
    
    fun removeDownload(id: String) {
        val currentList = _downloads.value.toMutableList()
        currentList.removeAll { it.id == id }
        _downloads.value = currentList
        saveDownloads()
    }
    
    fun getDownloadById(id: String): DownloadItem? {
        return _downloads.value.find { it.id == id }
    }
    
    fun getFailedDownloads(): List<DownloadItem> {
        return _downloads.value.filter { it.status == DownloadStatus.FAILED }
    }
    
    private fun saveDownloads() {
        try {
            val json = gson.toJson(_downloads.value)
            prefs.edit().putString(downloadsKey, json).apply()
        } catch (e: Exception) {
            // If saving fails, log but don't crash
            e.printStackTrace()
        }
    }
    
    private fun loadDownloads() {
        try {
            val json = prefs.getString(downloadsKey, null)
            if (json != null && json.isNotEmpty()) {
                val type = object : TypeToken<List<DownloadItem>>() {}.type
                val list: List<DownloadItem> = gson.fromJson(json, type)
                _downloads.value = list
            }
        } catch (e: Exception) {
            // If loading fails, start with empty list
            _downloads.value = emptyList()
            e.printStackTrace()
        }
    }
}

