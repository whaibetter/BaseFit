package com.basefit.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.Exercise
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.data.entity.ExerciseMedia
import com.basefit.app.ui.components.MediaViewerDialog
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ExerciseViewModel
import com.basefit.app.viewmodel.ExerciseWithMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddExercise: () -> Unit,
    onNavigateToEditExercise: (Long) -> Unit,
    viewModel: ExerciseViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    
    // 删除确认对话框
    var showDeleteDialog by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "动作管理",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAddExercise) {
                        Icon(Icons.Default.Add, "添加动作")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.exercises.isEmpty()) {
                EmptyExerciseContent(onAddExercise = onNavigateToAddExercise)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.exercises, key = { it.exercise.id }) { exerciseWithMedia ->
                        ExerciseManageCard(
                            exerciseWithMedia = exerciseWithMedia,
                            onEdit = { onNavigateToEditExercise(exerciseWithMedia.exercise.id) },
                            onDelete = { showDeleteDialog = exerciseWithMedia.exercise }
                        )
                    }
                }
            }
        }
    }
    
    // 删除确认对话框
    showDeleteDialog?.let { target ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { 
                Text("确定要删除「${target.name}」吗？\n删除后相关数据将被清除。") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExercise(target)
                        showDeleteDialog = null
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun EmptyExerciseContent(onAddExercise: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无动作",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击右上角添加你的第一个动作",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddExercise,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加动作")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseManageCard(
    exerciseWithMedia: ExerciseWithMedia,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val exercise = exerciseWithMedia.exercise
    val mediaList = exerciseWithMedia.mediaList
    
    val categoryColor = when (exercise.category) {
        ExerciseCategory.BODYWEIGHT -> BodyweightColor
        ExerciseCategory.STRENGTH -> StrengthColor
        ExerciseCategory.CARDIO -> CardioColor
    }

    var showMenu by remember { mutableStateOf(false) }
    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    
    val firstMedia = mediaList.sortedBy { it.orderIndex }.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEdit() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 媒体缩略图或分类图标
                if (firstMedia != null) {
                    MediaThumbnailItem(
                        media = firstMedia,
                        size = 48,
                        onClick = {
                            selectedMediaIndex = 0
                            showMediaViewer = true
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = exercise.name.first().toString(),
                            color = categoryColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // 信息
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = when (exercise.category) {
                            ExerciseCategory.BODYWEIGHT -> "自重训练"
                            ExerciseCategory.STRENGTH -> "力量训练"
                            ExerciseCategory.CARDIO -> "有氧运动"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // 菜单
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多",
                            tint = TextSecondary
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("编辑")
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Error)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("删除", color = Error)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }
            
            // 媒体数量提示（只有多个媒体时才显示）
            if (mediaList.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${mediaList.size} 个媒体",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
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
}

@Composable
private fun MediaThumbnailItem(
    media: ExerciseMedia,
    size: Int = 48,
    onClick: () -> Unit = {}
) {
    val typeColor = when (media.type) {
        com.basefit.app.data.entity.MediaType.IMAGE -> Primary
        com.basefit.app.data.entity.MediaType.GIF -> Warning
        com.basefit.app.data.entity.MediaType.VIDEO -> CardioColor
    }

    val typeIcon = when (media.type) {
        com.basefit.app.data.entity.MediaType.IMAGE -> Icons.Default.Image
        com.basefit.app.data.entity.MediaType.GIF -> Icons.Default.Gif
        com.basefit.app.data.entity.MediaType.VIDEO -> Icons.Default.Videocam
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
                val file = java.io.File(path)
                if (file.exists()) {
                    android.graphics.BitmapFactory.decodeFile(path)
                } else null
            }
        }

        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )

            // 视频类型显示播放图标
            if (media.type == com.basefit.app.data.entity.MediaType.VIDEO) {
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