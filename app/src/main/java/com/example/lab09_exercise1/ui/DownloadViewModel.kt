package com.example.lab09_exercise1.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lab09_exercise1.DownloadManager
import com.example.lab09_exercise1.data.DownloadItem
import com.example.lab09_exercise1.data.DownloadRepository
import com.example.lab09_exercise1.data.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DownloadRepository(application)
    private val downloadManager: DownloadManager by lazy { DownloadManager(application, repository) }
    
    val downloads: StateFlow<List<DownloadItem>> = repository.downloads
    
    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()
    
    private val _showNetworkErrorDialog = MutableStateFlow<String?>(null)
    val showNetworkErrorDialog: StateFlow<String?> = _showNetworkErrorDialog.asStateFlow()
    
    private val _showRetryFailedDialog = MutableStateFlow(false)
    val showRetryFailedDialog: StateFlow<Boolean> = _showRetryFailedDialog.asStateFlow()
    
    private val _selectedItemForDetails = MutableStateFlow<DownloadItem?>(null)
    val selectedItemForDetails: StateFlow<DownloadItem?> = _selectedItemForDetails.asStateFlow()
    
    private var isNetworkAvailable = true
    private val connectivityManager: ConnectivityManager? = try {
        application.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (e: Exception) {
        null
    }
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    init {
        // Delay network monitoring setup to avoid crash on initialization
        viewModelScope.launch {
            try {
                setupNetworkMonitoring()
            } catch (e: Exception) {
                // Handle error silently - network monitoring is optional
                e.printStackTrace()
                isNetworkAvailable = true // Assume network is available if monitoring fails
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            networkCallback?.let { callback ->
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        } catch (e: Exception) {
            // Ignore errors when unregistering
            e.printStackTrace()
        }
    }
    
    fun startDownload(url: String, hasPermission: Boolean) {
        if (!hasPermission) {
            _showPermissionDialog.value = true
            return
        }
        
        if (!isNetworkAvailable) {
            _showNetworkErrorDialog.value = "Không có kết nối internet. Vui lòng kiểm tra kết nối của bạn."
            return
        }
        
        viewModelScope.launch {
            downloadManager.downloadFile(
                url = url,
                onProgress = { item ->
                    // Progress updates handled automatically through repository
                },
                onError = { error ->
                    if (error.contains("network", ignoreCase = true) || 
                        error.contains("timeout", ignoreCase = true) ||
                        error.contains("connection", ignoreCase = true)) {
                        _showNetworkErrorDialog.value = "Lỗi kết nối: $error"
                    } else {
                        _showNetworkErrorDialog.value = "Lỗi tải xuống: $error"
                    }
                }
            )
        }
    }
    
    fun retryDownload(item: DownloadItem) {
        if (!isNetworkAvailable) {
            _showNetworkErrorDialog.value = "Không có kết nối internet. Vui lòng kiểm tra kết nối của bạn."
            return
        }
        
        viewModelScope.launch {
            downloadManager.retryDownload(
                downloadItem = item,
                onProgress = {},
                onError = { error ->
                    _showNetworkErrorDialog.value = "Lỗi tải lại: $error"
                }
            )
        }
    }
    
    fun retryAllFailed() {
        val failedDownloads = repository.getFailedDownloads()
        failedDownloads.forEach { item ->
            retryDownload(item)
        }
    }
    
    fun deleteDownload(item: DownloadItem) {
        viewModelScope.launch {
            item.filePath?.let { path ->
                try {
                    java.io.File(path).delete()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            repository.removeDownload(item.id)
        }
    }
    
    fun showDetails(item: DownloadItem) {
        _selectedItemForDetails.value = item
    }
    
    fun dismissDetails() {
        _selectedItemForDetails.value = null
    }
    
    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
    }
    
    fun dismissNetworkErrorDialog() {
        _showNetworkErrorDialog.value = null
    }
    
    fun dismissRetryFailedDialog() {
        _showRetryFailedDialog.value = false
    }
    
    private fun setupNetworkMonitoring() {
        try {
            val cm = connectivityManager ?: return
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (!isNetworkAvailable) {
                        isNetworkAvailable = true
                        val failedCount = repository.getFailedDownloads().size
                        if (failedCount > 0) {
                            _showRetryFailedDialog.value = true
                        }
                    }
                    isNetworkAvailable = true
                }
                
                override fun onLost(network: Network) {
                    isNetworkAvailable = false
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    isNetworkAvailable = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            }
            
            networkCallback?.let {
                cm.registerNetworkCallback(networkRequest, it)
            }
            
            // Check initial network state
            val network = cm.activeNetwork
            val capabilities = cm.getNetworkCapabilities(network)
            isNetworkAvailable = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            // If network monitoring fails, assume network is available
            isNetworkAvailable = true
            e.printStackTrace()
        }
    }
}

