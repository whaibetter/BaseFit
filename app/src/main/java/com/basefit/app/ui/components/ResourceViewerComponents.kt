package com.basefit.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.basefit.app.data.entity.ExerciseResource
import com.basefit.app.data.entity.ResourceType
import com.basefit.app.ui.theme.*
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResourceViewer(
    resources: List<ExerciseResource>,
    modifier: Modifier = Modifier,
    onResourceClick: ((ExerciseResource) -> Unit)? = null
) {
    val context = LocalContext.current

    if (resources.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = TextHint,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "暂无动作演示资源",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextHint
                )
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { resources.size })

    Box(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            pageSpacing = 8.dp
        ) { page ->
            val resource = resources[page]
            ResourcePage(
                resource = resource,
                onClick = {
                    onResourceClick?.invoke(resource) ?: openResource(context, resource)
                }
            )
        }

        if (resources.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(resources.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Primary
                                else Primary.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourcePage(
    resource: ExerciseResource,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (resource.resourceType) {
                ResourceType.GIF, ResourceType.IMAGE -> {
                    AsyncImage(
                        model = resource.resourcePath,
                        contentDescription = resource.displayName ?: "动作演示",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                ResourceType.VIDEO -> {
                    AsyncImage(
                        model = resource.thumbnailPath ?: resource.resourcePath,
                        contentDescription = resource.displayName ?: "视频",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "播放视频",
                                    tint = Primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
                ResourceType.LINK -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "打开链接",
                                tint = Primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "点击查看外部资源",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Primary
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
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
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            resource.displayName?.let { name ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun openResource(context: Context, resource: ExerciseResource) {
    try {
        when (resource.resourceType) {
            ResourceType.LINK -> {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resource.resourcePath))
                context.startActivity(intent)
            }
            ResourceType.VIDEO -> {
                val file = File(resource.resourcePath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "视频文件不存在", Toast.LENGTH_SHORT).show()
                }
            }
            ResourceType.GIF, ResourceType.IMAGE -> {
                val file = File(resource.resourcePath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "image/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "图片文件不存在", Toast.LENGTH_SHORT).show()
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "无法打开资源: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ResourcePreviewCard(
    resource: ExerciseResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .size(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (resource.resourceType) {
                ResourceType.GIF, ResourceType.IMAGE -> {
                    AsyncImage(
                        model = resource.resourcePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                ResourceType.VIDEO -> {
                    AsyncImage(
                        model = resource.thumbnailPath ?: resource.resourcePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                ResourceType.LINK -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResourceInfoRow(
    resource: ExerciseResource,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = when (resource.resourceType) {
                ResourceType.GIF -> Icons.Default.Gif
                ResourceType.IMAGE -> Icons.Default.Image
                ResourceType.VIDEO -> Icons.Default.VideoFile
                ResourceType.LINK -> Icons.Default.Link
            },
            contentDescription = null,
            tint = when (resource.resourceType) {
                ResourceType.GIF -> Color(0xFF9C27B0)
                ResourceType.IMAGE -> Color(0xFF4CAF50)
                ResourceType.VIDEO -> Color(0xFFFF5722)
                ResourceType.LINK -> Color(0xFF2196F3)
            },
            modifier = Modifier.size(20.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = resource.displayName ?: when (resource.resourceType) {
                    ResourceType.GIF -> "GIF动图"
                    ResourceType.IMAGE -> "图片"
                    ResourceType.VIDEO -> "视频"
                    ResourceType.LINK -> "外部链接"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                resource.fileSize?.let { size ->
                    Text(
                        text = formatFileSize(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                resource.duration?.let { duration ->
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "${minutes}分${secs}秒"
    } else {
        "${secs}秒"
    }
}
