package com.example.lab09_exercise1.ui

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.drawable.toBitmap
import com.example.lab09_exercise1.data.DownloadItem
import com.example.lab09_exercise1.data.DownloadStatus
import java.io.File

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    hasStoragePermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsStateWithLifecycle()
    val showNetworkErrorDialog by viewModel.showNetworkErrorDialog.collectAsStateWithLifecycle()
    val showRetryFailedDialog by viewModel.showRetryFailedDialog.collectAsStateWithLifecycle()
    val selectedItemForDetails by viewModel.selectedItemForDetails.collectAsStateWithLifecycle()
    
    var urlText by remember { mutableStateOf("") }
    var showContextMenu by remember { mutableStateOf<DownloadItem?>(null) }
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Header
        Header()
        
        // URL Input and Download Button
        UrlInputSection(
            urlText = urlText,
            onUrlChange = { urlText = it },
            onDownloadClick = {
                if (urlText.isNotBlank()) {
                    viewModel.startDownload(urlText.trim(), hasStoragePermission)
                    urlText = ""
                }
            }
        )
        
        // Download List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (downloads.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(downloads) { item ->
                    DownloadItemRow(
                        item = item,
                        onLongPress = { showContextMenu = item },
                        onOpenClick = {
                            item.filePath?.let { path ->
                                openFile(context, path, item.fileName)
                            }
                        },
                        onDeleteClick = {
                            viewModel.deleteDownload(item)
                            showContextMenu = null
                        },
                        onDetailsClick = {
                            viewModel.showDetails(item)
                            showContextMenu = null
                        },
                        onRetryClick = {
                            viewModel.retryDownload(item)
                            showContextMenu = null
                        }
                    )
                }
            }
        }
    }
    
    // Permission Dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPermissionDialog() },
            title = { Text("Cần quyền ghi vào bộ nhớ") },
            text = { Text("Ứng dụng cần quyền ghi vào bộ nhớ ngoài để tải xuống tập tin.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissPermissionDialog()
                    onRequestPermission()
                }) {
                    Text("Cấp quyền")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPermissionDialog() }) {
                    Text("Hủy")
                }
            }
        )
    }
    
    // Network Error Dialog
    showNetworkErrorDialog?.let { error ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissNetworkErrorDialog() },
            title = { Text("Lỗi kết nối") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissNetworkErrorDialog() }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Retry Failed Downloads Dialog
    if (showRetryFailedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRetryFailedDialog() },
            title = { Text("Kết nối internet đã được khôi phục") },
            text = { Text("Bạn có muốn tải lại các tập tin thất bại không?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.retryAllFailed()
                    viewModel.dismissRetryFailedDialog()
                }) {
                    Text("Có")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRetryFailedDialog() }) {
                    Text("Không")
                }
            }
        )
    }
    
    // Details Dialog
    selectedItemForDetails?.let { item ->
        DetailsDialog(
            item = item,
            onDismiss = { viewModel.dismissDetails() }
        )
    }
    
    // Context Menu
    showContextMenu?.let { item ->
        ContextMenuDialog(
            item = item,
            onDismiss = { showContextMenu = null },
            onOpen = {
                item.filePath?.let { path ->
                    openFile(context, path, item.fileName)
                }
                showContextMenu = null
            },
            onDelete = {
                viewModel.deleteDownload(item)
                showContextMenu = null
            },
            onDetails = {
                viewModel.showDetails(item)
                showContextMenu = null
            },
            onRetry = {
                viewModel.retryDownload(item)
                showContextMenu = null
            }
        )
    }
}

@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF4CAF50))
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Download Manager",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun UrlInputSection(
    urlText: String,
    onUrlChange: (String) -> Unit,
    onDownloadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = urlText,
            onValueChange = onUrlChange,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            placeholder = { Text("http://example.com/file.mp4", color = Color.Gray) },
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4CAF50),
                unfocusedBorderColor = Color.LightGray
            ),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 14.sp
            )
        )
        Button(
            onClick = onDownloadClick,
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = 120.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Download", color = Color.White)
        }
    }
}

@Composable
fun DownloadItemRow(
    item: DownloadItem,
    onLongPress: () -> Unit,
    onOpenClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDetailsClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val context = LocalContext.current
    val iconResId = getIconResourceId(context, item.getIconResource())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLongPress() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val iconBitmap = remember(iconResId) {
                if (iconResId != 0) {
                    try {
                        val drawable = context.resources.getDrawable(iconResId, context.theme)
                        drawable.toBitmap(48.dp.value.toInt(), 48.dp.value.toInt()).asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
            
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                // Default icon if no icon found or failed to load
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.Gray
                )
            }
            
            // File Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.fileName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.getFormattedSize(),
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // Status or Progress
            when (item.status) {
                DownloadStatus.DOWNLOADING -> {
                    Column(
                        modifier = Modifier.width(100.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        LinearProgressIndicator(
                            progress = item.getProgress(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF2196F3),
                            trackColor = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(item.getProgress() * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
                DownloadStatus.COMPLETE -> {
                    Text(
                        text = "Complete",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
                DownloadStatus.FAILED -> {
                    Text(
                        text = "Failed",
                        fontSize = 14.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
                DownloadStatus.WAITING -> {
                    Text(
                        text = "Waiting",
                        fontSize = 14.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    Color(0xFF8D6E63),
                    shape = androidx.compose.foundation.shape.CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Let's download a file",
            fontSize = 18.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ContextMenuDialog(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onRetry: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                TextButton(onClick = onOpen) {
                    Text("Mở tập tin")
                }
                Divider()
                TextButton(onClick = onDelete) {
                    Text("Xóa tập tin")
                }
                Divider()
                TextButton(onClick = onDetails) {
                    Text("Xem chi tiết")
                }
                if (item.status == DownloadStatus.FAILED) {
                    Divider()
                    TextButton(onClick = onRetry) {
                        Text("Tải lại")
                    }
                }
                Divider()
                TextButton(onClick = onDismiss) {
                    Text("Hủy")
                }
            }
        }
    }
}

@Composable
fun DetailsDialog(
    item: DownloadItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chi tiết tập tin") },
        text = {
            Column {
                Text("Tên: ${item.fileName}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Kích thước: ${item.getFormattedSize()}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Trạng thái: ${getStatusText(item.status)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Link: ${item.url}")
                item.errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Lỗi: $it", color = Color.Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

fun getStatusText(status: DownloadStatus): String {
    return when (status) {
        DownloadStatus.WAITING -> "Đang chờ"
        DownloadStatus.DOWNLOADING -> "Đang tải"
        DownloadStatus.COMPLETE -> "Hoàn thành"
        DownloadStatus.FAILED -> "Thất bại"
    }
}

fun getIconResourceId(context: android.content.Context, iconName: String): Int {
    return try {
        context.resources.getIdentifier(iconName, "drawable", context.packageName)
    } catch (e: Exception) {
        0
    }
}

fun getMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        "ppt", "pptx" -> "application/vnd.ms-powerpoint"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "gif" -> "image/gif"
        "mp4" -> "video/mp4"
        "avi" -> "video/x-msvideo"
        "mp3" -> "audio/mpeg"
        "zip" -> "application/zip"
        else -> "*/*"
    }
}

fun openFile(context: android.content.Context, filePath: String, fileName: String) {
    val file = File(filePath)
    if (!file.exists()) {
        return
    }
    
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
            setDataAndType(uri, getMimeType(fileName))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Try with generic intent
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Handle error - file cannot be opened
        }
    }
}

