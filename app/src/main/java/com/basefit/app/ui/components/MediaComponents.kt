package com.basefit.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.basefit.app.data.entity.ExerciseMedia
import com.basefit.app.data.entity.MediaType
import com.basefit.app.ui.theme.*
import java.io.File

/**
 * 媒体缩略图组件
 */
@Composable
fun MediaThumbnail(
    media: ExerciseMedia,
    size: Int = 48,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current

    val typeColor = when (media.type) {
        MediaType.IMAGE -> Primary
        MediaType.GIF -> Warning
        MediaType.VIDEO -> CardioColor
    }

    val typeIcon = when (media.type) {
        MediaType.IMAGE -> Icons.Default.Image
        MediaType.GIF -> Icons.Default.Gif
        MediaType.VIDEO -> Icons.Default.Videocam
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 尝试加载缩略图或图片
        val imagePath = media.thumbnailPath ?: media.localPath
        val bitmap = remember(imagePath) {
            imagePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(path)
                } else null
            }
        }

        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 视频类型显示播放图标
            if (media.type == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size((size / 3).dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size((size / 5).dp)
                    )
                }
            }
        } else {
            // 默认占位图
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(typeColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size((size / 2).dp)
                )
            }
        }
    }
}

/**
 * 媒体横向列表组件
 */
@Composable
fun MediaRow(
    mediaList: List<ExerciseMedia>,
    onMediaClick: (ExerciseMedia) -> Unit = {}
) {
    if (mediaList.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(mediaList, key = { it.id }) { media ->
            MediaThumbnail(
                media = media,
                size = 64,
                onClick = { onMediaClick(media) }
            )
        }
    }
}

/**
 * 全屏媒体查看器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerDialog(
    mediaList: List<ExerciseMedia>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val currentMedia = mediaList.getOrNull(currentIndex) ?: return

    val context = LocalContext.current
    var scale by remember { mutableStateOf(1f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            // 关闭按钮
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // 媒体计数器
            if (mediaList.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 60.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${mediaList.size}",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 媒体内容
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 100.dp)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.5f, 3f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                when (currentMedia.type) {
                    MediaType.IMAGE, MediaType.GIF -> {
                        val bitmap = remember(currentMedia.localPath) {
                            currentMedia.localPath?.let { path ->
                                val file = File(path)
                                if (file.exists()) {
                                    BitmapFactory.decodeFile(path)
                                } else null
                            }
                        }

                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "无法加载图片",
                                    color = Color.White
                                )
                            }
                        }
                    }
                    MediaType.VIDEO -> {
                        currentMedia.localPath?.let { path ->
                            VideoPlayer(
                                videoPath = path,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )
                        }
                    }
                }
            }

            // 左右切换按钮
            if (mediaList.size > 1) {
                // 左箭头
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "上一张",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // 右箭头
                if (currentIndex < mediaList.size - 1) {
                    IconButton(
                        onClick = { currentIndex++ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "下一张",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            // 底部缩略图列表
            if (mediaList.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mediaList.indices.toList()) { index ->
                        val media = mediaList[index]
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (index == currentIndex) Primary else Color.White.copy(alpha = 0.3f))
                                .padding(2.dp)
                        ) {
                            MediaThumbnail(
                                media = media,
                                size = 44,
                                onClick = { currentIndex = index }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 视频播放器组件
 */
@Composable
fun VideoPlayer(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoPath(videoPath)
                    setOnPreparedListener { 
                        it.isLooping = true 
                    }
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { videoView ->
                if (isPlaying) {
                    videoView.start()
                } else {
                    videoView.pause()
                }
            }
        )

        // 播放/暂停按钮
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { isPlaying = !isPlaying },
            contentAlignment = Alignment.Center
        ) {
            if (!isPlaying) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}
