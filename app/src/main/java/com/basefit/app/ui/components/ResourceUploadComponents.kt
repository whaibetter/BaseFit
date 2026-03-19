package com.basefit.app.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import coil.compose.AsyncImage
import com.basefit.app.data.entity.ExerciseResource
import com.basefit.app.data.entity.ResourceType
import com.basefit.app.ui.theme.*
import com.basefit.app.utils.FileUtils

@Composable
fun ResourceUploadSection(
    exerciseId: Long,
    resources: List<ExerciseResource>,
    onAddResource: (ResourceType, String, String?, String?, Long?, Int?) -> Unit,
    onDeleteResource: (ExerciseResource) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var linkUrl by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "动作资源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { showLinkDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Surface,
                        contentColor = Primary
                    )
                ) {
                    Icon(Icons.Default.Link, contentDescription = "添加链接")
                }
                IconButton(
                    onClick = { showAddDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加资源")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (resources.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = TextHint,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "暂无资源，点击上方按钮添加",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextHint
                        )
                    }
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(resources, key = { it.id }) { resource ->
                    ResourceItem(
                        resource = resource,
                        onDelete = { onDeleteResource(resource) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddResourceDialog(
            onDismiss = { showAddDialog = false },
            onResourceSelected = { type, path, thumbnail, name, size, duration ->
                onAddResource(type, path, thumbnail, name, size, duration)
                showAddDialog = false
            }
        )
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("添加资源链接") },
            text = {
                Column {
                    OutlinedTextField(
                        value = linkUrl,
                        onValueChange = { linkUrl = it },
                        label = { Text("资源链接") },
                        placeholder = { Text("输入GIF、图片或视频链接") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "支持GIF、图片或视频的外部链接",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (linkUrl.isNotBlank()) {
                            if (FileUtils.isValidUrl(linkUrl)) {
                                val type = when {
                                    linkUrl.contains(".gif", ignoreCase = true) -> ResourceType.GIF
                                    linkUrl.contains(".mp4", ignoreCase = true) ||
                                    linkUrl.contains(".webm", ignoreCase = true) -> ResourceType.VIDEO
                                    else -> ResourceType.IMAGE
                                }
                                onAddResource(type, linkUrl, null, null, null, null)
                                linkUrl = ""
                                showLinkDialog = false
                            } else {
                                Toast.makeText(context, "请输入有效的链接地址", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showLinkDialog = false
                    linkUrl = ""
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ResourceItem(
    resource: ExerciseResource,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .size(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (resource.resourceType) {
                ResourceType.GIF, ResourceType.IMAGE -> {
                    AsyncImage(
                        model = resource.resourcePath,
                        contentDescription = resource.displayName ?: "资源",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                ResourceType.VIDEO -> {
                    val thumbnail = resource.thumbnailPath ?: resource.resourcePath
                    AsyncImage(
                        model = thumbnail,
                        contentDescription = resource.displayName ?: "视频",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "播放视频",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                ResourceType.LINK -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "外部链接",
                                tint = Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = resource.displayName ?: "外部链接",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showDeleteConfirm = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (resource.resourceType) {
                        ResourceType.GIF -> Color(0xFF9C27B0)
                        ResourceType.IMAGE -> Color(0xFF4CAF50)
                        ResourceType.VIDEO -> Color(0xFFFF5722)
                        ResourceType.LINK -> Color(0xFF2196F3)
                    }
                ) {
                    Text(
                        text = when (resource.resourceType) {
                            ResourceType.GIF -> "GIF"
                            ResourceType.IMAGE -> "图片"
                            ResourceType.VIDEO -> "视频"
                            ResourceType.LINK -> "链接"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除这个资源吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun AddResourceDialog(
    onDismiss: () -> Unit,
    onResourceSelected: (ResourceType, String, String?, String?, Long?, Int?) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleImageSelection(context, it, onResourceSelected)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            handleVideoSelection(context, it, onResourceSelected)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(context, "需要存储权限才能选择文件", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndRequestPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            val needsRequest = permissions.any {
                ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needsRequest) {
                permissionLauncher.launch(permissions)
                false
            } else {
                true
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "添加资源",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResourceOptionCard(
                        icon = Icons.Default.Image,
                        label = "图片",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (checkAndRequestPermissions()) {
                                imagePicker.launch("image/*")
                            }
                        }
                    )
                    ResourceOptionCard(
                        icon = Icons.Default.Gif,
                        label = "GIF动图",
                        color = Color(0xFF9C27B0),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (checkAndRequestPermissions()) {
                                imagePicker.launch("image/gif")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ResourceOptionCard(
                        icon = Icons.Default.VideoFile,
                        label = "视频",
                        color = Color(0xFFFF5722),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (checkAndRequestPermissions()) {
                                videoPicker.launch("video/*")
                            }
                        }
                    )
                    ResourceOptionCard(
                        icon = Icons.Default.Link,
                        label = "链接",
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onDismiss()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("取消")
                }
            }
        }
    }
}

@Composable
private fun ResourceOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        }
    }
}

private fun handleImageSelection(
    context: Context,
    uri: Uri,
    onResourceSelected: (ResourceType, String, String?, String?, Long?, Int?) -> Unit
) {
    val mimeType = context.contentResolver.getType(uri)
    val isGif = mimeType?.contains("gif", ignoreCase = true) == true ||
                uri.toString().contains(".gif", ignoreCase = true)

    val result = FileUtils.copyFileToAppStorage(
        context,
        uri,
        if (isGif) "gif" else "image"
    )

    result?.let { (path, size) ->
        val thumbnail = if (!isGif) {
            FileUtils.createImageThumbnail(context, path)
        } else null

        val fileName = FileUtils.getFileName(context, uri)
        val resourceType = if (isGif) ResourceType.GIF else ResourceType.IMAGE

        onResourceSelected(resourceType, path, thumbnail, fileName, size, null)
    }
}

private fun handleVideoSelection(
    context: Context,
    uri: Uri,
    onResourceSelected: (ResourceType, String, String?, String?, Long?, Int?) -> Unit
) {
    val result = FileUtils.copyFileToAppStorage(context, uri, "video")

    result?.let { (path, size) ->
        val thumbnail = FileUtils.createVideoThumbnail(context, path)
        val duration = FileUtils.getVideoDuration(path)
        val fileName = FileUtils.getFileName(context, uri)

        onResourceSelected(ResourceType.VIDEO, path, thumbnail, fileName, size, duration)
    }
}
