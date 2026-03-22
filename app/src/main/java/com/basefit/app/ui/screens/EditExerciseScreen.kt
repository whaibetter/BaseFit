package com.basefit.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.data.entity.ExerciseMedia
import com.basefit.app.data.entity.MediaType
import com.basefit.app.data.storage.LocalMediaStorage
import com.basefit.app.ui.components.MediaThumbnail
import com.basefit.app.ui.components.MediaViewerDialog
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ExerciseDetailViewModel
import com.basefit.app.viewmodel.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    exerciseId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val mediaStorage = remember { LocalMediaStorage.getInstance(context) }
    val scope = rememberCoroutineScope()
    
    // 表单状态
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.BODYWEIGHT) }
    var existingMedia by remember { mutableStateOf<List<ExerciseMedia>>(emptyList()) }
    var newMediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var mediaToDelete by remember { mutableStateOf<List<ExerciseMedia>>(emptyList()) }
    
    // 加载数据
    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }
    
    // 当数据加载完成后更新表单
    LaunchedEffect(state.exercise, state.mediaList) {
        state.exercise?.let { exercise ->
            name = exercise.name
            selectedCategory = exercise.category
        }
        existingMedia = state.mediaList
    }
    
    val categories = mapOf(
        ExerciseCategory.BODYWEIGHT to "自重训练",
        ExerciseCategory.STRENGTH to "力量训练",
        ExerciseCategory.CARDIO to "有氧运动"
    )
    
    // 媒体选择器
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            newMediaItems = newMediaItems + MediaItem(uri = it, type = MediaType.IMAGE)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            newMediaItems = newMediaItems + MediaItem(uri = it, type = MediaType.VIDEO)
        }
    }

    val gifPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            newMediaItems = newMediaItems + MediaItem(uri = it, type = MediaType.GIF)
        }
    }
    
    // 媒体查看器状态
    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "编辑动作",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // 名称输入
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("动作名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Divider
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 分类选择
                Text(
                    text = "动作分类",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (category, label) ->
                        val isSelected = selectedCategory == category
                        val categoryColor = when (category) {
                            ExerciseCategory.BODYWEIGHT -> BodyweightColor
                            ExerciseCategory.STRENGTH -> StrengthColor
                            ExerciseCategory.CARDIO -> CardioColor
                        }
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = categoryColor.copy(alpha = 0.15f),
                                containerColor = Surface
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 现有媒体
                if (existingMedia.isNotEmpty()) {
                    Text(
                        text = "已保存的媒体 (${existingMedia.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(existingMedia, key = { it.id }) { media ->
                            ExistingMediaCard(
                                media = media,
                                isMarkedForDeletion = mediaToDelete.contains(media),
                                onClick = {
                                    selectedMediaIndex = existingMedia.indexOf(media)
                                    showMediaViewer = true
                                },
                                onToggleDelete = {
                                    mediaToDelete = if (mediaToDelete.contains(media)) {
                                        mediaToDelete - media
                                    } else {
                                        mediaToDelete + media
                                    }
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 新添加的媒体
                if (newMediaItems.isNotEmpty()) {
                    Text(
                        text = "待添加的媒体 (${newMediaItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(newMediaItems, key = { it.uri.toString() }) { item ->
                            NewMediaCard(
                                item = item,
                                onRemove = {
                                    newMediaItems = newMediaItems - item
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 添加媒体按钮
                Text(
                    text = "添加媒体",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MediaAddButton(
                        icon = Icons.Default.Image,
                        label = "图片",
                        color = Primary,
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                    MediaAddButton(
                        icon = Icons.Default.Gif,
                        label = "GIF",
                        color = Warning,
                        onClick = { gifPicker.launch("image/*") },
                        modifier = Modifier.weight(1f)
                    )
                    MediaAddButton(
                        icon = Icons.Default.Videocam,
                        label = "视频",
                        color = CardioColor,
                        onClick = { videoPicker.launch("video/*") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 保存按钮
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "请输入动作名称", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        state.exercise?.let { exercise ->
                            scope.launch {
                                // 检查名称是否与其他动作重复
                                if (name.trim() != exercise.name && 
                                    viewModel.checkNameExistsForOther(name.trim(), exercise.id)) {
                                    Toast.makeText(context, "动作名称已存在，请使用其他名称", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                
                                viewModel.updateExercise(
                                    exercise.copy(
                                        name = name.trim(),
                                        category = selectedCategory
                                    ),
                                    mediaToDelete = mediaToDelete,
                                    newMediaItems = newMediaItems,
                                    mediaStorage = mediaStorage
                                )
                                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("保存修改", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    
    // 媒体查看器
    if (showMediaViewer && existingMedia.isNotEmpty()) {
        MediaViewerDialog(
            mediaList = existingMedia,
            initialIndex = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
    }
}

@Composable
private fun ExistingMediaCard(
    media: ExerciseMedia,
    isMarkedForDeletion: Boolean,
    onClick: () -> Unit,
    onToggleDelete: () -> Unit
) {
    val typeColor = when (media.type) {
        MediaType.IMAGE -> Primary
        MediaType.GIF -> Warning
        MediaType.VIDEO -> CardioColor
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        MediaThumbnail(
            media = media,
            size = 100,
            onClick = onClick
        )
        
        // 删除标记遮罩
        if (isMarkedForDeletion) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Error.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // 切换删除按钮
        IconButton(
            onClick = onToggleDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(4.dp)
        ) {
            Icon(
                if (isMarkedForDeletion) Icons.Default.Restore else Icons.Default.Close,
                contentDescription = if (isMarkedForDeletion) "恢复" else "标记删除",
                tint = Color.White,
                modifier = Modifier
                    .background(
                        if (isMarkedForDeletion) Primary else Error,
                        CircleShape
                    )
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun NewMediaCard(
    item: MediaItem,
    onRemove: () -> Unit
) {
    val typeColor = when (item.type) {
        MediaType.IMAGE -> Primary
        MediaType.GIF -> Warning
        MediaType.VIDEO -> CardioColor
    }

    val typeIcon = when (item.type) {
        MediaType.IMAGE -> Icons.Default.Image
        MediaType.GIF -> Icons.Default.Gif
        MediaType.VIDEO -> Icons.Default.Videocam
    }

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
    ) {
        // 占位符背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(typeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor
                )
            }
        }

        // 类型标签
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = typeColor.copy(alpha = 0.8f)
        ) {
            Text(
                text = "待保存",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // 删除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .padding(4.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "移除",
                tint = Color.White,
                modifier = Modifier
                    .background(Error, CircleShape)
                    .padding(4.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
private fun MediaAddButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontWeight = FontWeight.Medium)
    }
}
