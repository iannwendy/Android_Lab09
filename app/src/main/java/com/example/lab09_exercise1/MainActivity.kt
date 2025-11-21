package com.example.lab09_exercise1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.lab09_exercise1.ui.DownloadScreen
import com.example.lab09_exercise1.ui.DownloadViewModel
import com.example.lab09_exercise1.ui.theme.Lab09_Exercise1Theme

class MainActivity : ComponentActivity() {
    private val viewModel: DownloadViewModel by viewModels {
        androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasStoragePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // For Android 13+, we don't need storage permission for Downloads folder
        } else {
            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        }
        // Permission result is handled in the UI
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            checkAndRequestPermissions()
            
            setContent {
                Lab09_Exercise1Theme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val hasStoragePermission = checkStoragePermission()
                        DownloadScreen(
                            viewModel = viewModel,
                            hasStoragePermission = hasStoragePermission,
                            onRequestPermission = {
                                requestStoragePermission()
                            }
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's an error, show a simple error message
            setContent {
                Lab09_Exercise1Theme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = "Error: ${e.message}",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
    
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true // Android 13+ doesn't need storage permission for Downloads folder
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun checkAndRequestPermissions() {
        if (!checkStoragePermission()) {
            requestStoragePermission()
        }
    }
    
    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ doesn't need storage permission
            return
        }
        
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        )
    }
}
