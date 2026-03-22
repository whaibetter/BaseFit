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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.data.entity.MediaType
import com.basefit.app.data.storage.LocalMediaStorage
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ExerciseViewModel
import com.basefit.app.viewmodel.MediaItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExerciseViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaStorage = remember { LocalMediaStorage.getInstance(context) }

    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.BODYWEIGHT) }
    var mediaItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

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
            mediaItems = mediaItems + MediaItem(uri = it, type = MediaType.IMAGE)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            mediaItems = mediaItems + MediaItem(uri = it, type = MediaType.VIDEO)
        }
    }

    val gifPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            mediaItems = mediaItems + MediaItem(uri = it, type = MediaType.GIF)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "添加动作",
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("动作名称") },
                placeholder = { Text("例如：俯卧撑") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category selection
            Text(
                text = "选择分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            categories.forEach { (category, label) ->
                val color = when (category) {
                    ExerciseCategory.BODYWEIGHT -> BodyweightColor
                    ExerciseCategory.STRENGTH -> StrengthColor
                    ExerciseCategory.CARDIO -> CardioColor
                }

                val isSelected = selectedCategory == category

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) color.copy(alpha = 0.1f) else Surface
                    ),
                    onClick = { selectedCategory = category }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = color,
                                unselectedColor = TextHint
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) color else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Media section
            Text(
                text = "媒体资源（可选）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "添加图片、GIF动图或视频来展示动作要领",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Media buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MediaButton(
                    icon = Icons.Default.Image,
                    label = "图片",
                    color = Primary,
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.weight(1f)
                )
                MediaButton(
                    icon = Icons.Default.Gif,
                    label = "GIF",
                    color = Warning,
                    onClick = { gifPicker.launch("image/gif") },
                    modifier = Modifier.weight(1f)
                )
                MediaButton(
                    icon = Icons.Default.Videocam,
                    label = "视频",
                    color = CardioColor,
                    onClick = { videoPicker.launch("video/*") },
                    modifier = Modifier.weight(1f)
                )
            }

            // Selected media preview
            if (mediaItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "已选择 ${mediaItems.size} 个媒体文件",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mediaItems, key = { it.uri.toString() }) { item ->
                        MediaPreviewCard(
                            item = item,
                            onRemove = {
                                mediaItems = mediaItems - item
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Preset suggestions
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "常见动作参考",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val suggestions = when (selectedCategory) {
                        ExerciseCategory.BODYWEIGHT -> listOf("俯卧撑", "仰卧起坐", "深蹲", "引体向上", "平板支撑", "波比跳")
                        ExerciseCategory.STRENGTH -> listOf("哑铃弯举", "杠铃卧推", "硬拉", "哑铃推举", "划船", "腿举")
                        ExerciseCategory.CARDIO -> listOf("跑步", "跳绳", "骑行", "游泳", "登山机", "椭圆机")
                    }

                    suggestions.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { suggestion ->
                                FilterChip(
                                    selected = false,
                                    onClick = { name = suggestion },
                                    label = { Text(suggestion) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "请输入动作名称", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (isLoading) return@Button

                    isLoading = true
                    scope.launch {
                        // 检查名称是否已存在
                        if (viewModel.checkNameExists(name.trim())) {
                            isLoading = false
                            Toast.makeText(context, "动作名称已存在，请使用其他名称", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // 添加动作
                        val exerciseId = viewModel.addExerciseWithMedia(
                            name = name.trim(),
                            category = selectedCategory,
                            mediaItems = mediaItems,
                            mediaStorage = mediaStorage
                        )

                        isLoading = false

                        if (exerciseId > 0) {
                            Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(context, "添加失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "保存",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaPreviewCard(
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
        // Placeholder for media preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(typeColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

        // Remove button
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