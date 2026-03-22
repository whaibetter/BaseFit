package com.basefit.app.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.data.entity.ExerciseMedia
import com.basefit.app.data.entity.MediaType
import com.basefit.app.ui.components.MediaViewerDialog
import com.basefit.app.ui.components.VideoPlayer
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ExerciseDetailViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // 媒体查看器状态
    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    
    // 删除确认对话框
    var showDeleteMediaDialog by remember { mutableStateOf<ExerciseMedia?>(null) }

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    val exercise = state.exercise
    val mediaList = state.mediaList

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        exercise?.name ?: "动作详情",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { exercise?.let { onNavigateToEdit(it.id) } }) {
                        Icon(Icons.Default.Edit, "编辑")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = Background
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (exercise == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("动作不存在", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 动作基本信息
                item {
                    ExerciseInfoCard(exercise = exercise)
                }
                
                // 媒体资源
                if (mediaList.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "媒体资源 (${mediaList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            TextButton(onClick = { exercise?.let { onNavigateToEdit(it.id) } }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("添加")
                            }
                        }
                    }
                    
                    items(mediaList.sortedBy { it.orderIndex }) { media ->
                        MediaCard(
                            media = media,
                            onClick = {
                                selectedMediaIndex = mediaList.sortedBy { it.orderIndex }.indexOf(media)
                                showMediaViewer = true
                            },
                            onDelete = { showDeleteMediaDialog = media }
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ImageNotSupported,
                                    contentDescription = null,
                                    tint = TextHint,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "暂无媒体资源",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "点击右上角编辑按钮添加",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextHint
                                )
                            }
                        }
                    }
                }
                
                // 底部间距
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
    
    // 媒体查看器
    if (showMediaViewer && mediaList.isNotEmpty()) {
        MediaViewerDialog(
            mediaList = mediaList.sortedBy { it.orderIndex },
            initialIndex = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
    }
    
    // 删除媒体确认对话框
    showDeleteMediaDialog?.let { media ->
        AlertDialog(
            onDismissRequest = { showDeleteMediaDialog = null },
            title = { Text("删除媒体") },
            text = { Text("确定要删除这个${when(media.type) {
                MediaType.IMAGE -> "图片"
                MediaType.GIF -> "GIF"
                MediaType.VIDEO -> "视频"
            }}吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedia(media)
                        showDeleteMediaDialog = null
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMediaDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ExerciseInfoCard(exercise: com.basefit.app.data.entity.Exercise) {
    val categoryColor = when (exercise.category) {
        ExerciseCategory.BODYWEIGHT -> BodyweightColor
        ExerciseCategory.STRENGTH -> StrengthColor
        ExerciseCategory.CARDIO -> CardioColor
    }

    val categoryName = when (exercise.category) {
        ExerciseCategory.BODYWEIGHT -> "自重训练"
        ExerciseCategory.STRENGTH -> "力量训练"
        ExerciseCategory.CARDIO -> "有氧运动"
    }

    val categoryIcon = when (exercise.category) {
        ExerciseCategory.BODYWEIGHT -> Icons.Default.FitnessCenter
        ExerciseCategory.STRENGTH -> Icons.Default.FitnessCenter
        ExerciseCategory.CARDIO -> Icons.Default.DirectionsRun
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(categoryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = categoryColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaCard(
    media: com.basefit.app.data.entity.ExerciseMedia,
    onClick: () -> Unit,
    onDelete: () -> Unit
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 媒体预览区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(typeColor.copy(alpha = 0.05f))
            ) {
                // 显示媒体内容
                when (media.type) {
                    MediaType.VIDEO -> {
                        media.localPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                VideoPlayer(
                                    videoPath = path,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // 显示占位符
                                MediaPlaceholder(typeIcon, typeColor, media.type.name)
                            }
                        } ?: MediaPlaceholder(typeIcon, typeColor, media.type.name)
                    }
                    MediaType.IMAGE, MediaType.GIF -> {
                        media.localPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                val bitmap = remember(path) {
                                    BitmapFactory.decodeFile(path)
                                }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } ?: MediaPlaceholder(typeIcon, typeColor, media.type.name)
                            } else {
                                MediaPlaceholder(typeIcon, typeColor, media.type.name)
                            }
                        } ?: MediaPlaceholder(typeIcon, typeColor, media.type.name)
                    }
                }

                // 删除按钮
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 点击查看提示
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "点击查看",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            // 媒体信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = typeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = media.type.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = typeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = media.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MediaPlaceholder(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    typeName: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = typeName,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}
