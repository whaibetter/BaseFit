package com.basefit.app.ui.screens

import android.graphics.BitmapFactory
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.repository.TodayPlanItem
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.data.entity.ExerciseMedia
import com.basefit.app.data.entity.MediaType
import com.basefit.app.ui.components.MediaViewerDialog
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ChallengePlanWithExercise
import com.basefit.app.viewmodel.HomeViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlan: () -> Unit,
    onNavigateToCheckIn: (Long, Long) -> Unit,
    onNavigateToExerciseDetail: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    bottomBarPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 监听页面恢复，自动刷新数据
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadTodayPlans()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "BaseFit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToPlan,
                containerColor = Primary,
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = bottomBarPadding.calculateBottomPadding())
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加计划")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            // Progress Card
            ProgressCard(
                completed = state.completedCount,
                total = state.totalCount,
                onAddPlan = onNavigateToPlan
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Tab切换
                var selectedTab by remember { mutableStateOf(0) }
                val tabs = listOf("今日计划", "进行中的挑战")
                val challengeCount = state.activeChallenges.size
                
                // 自定义Tab样式
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        val count = if (index == 0) state.todayPlans.size else challengeCount
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            label = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        title,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) Primary.copy(alpha = 0.3f) else TextHint.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Primary else TextSecondary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                containerColor = Surface
                            )
                        )
                    }
                }
                
                // 内容区域
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // 今日计划
                            if (state.todayPlans.isEmpty()) {
                                item {
                                    EmptyPlanCard(onAddPlan = onNavigateToPlan)
                                }
                            } else {
                                items(state.todayPlans, key = { "plan_${it.weekPlan.id}" }) { item ->
                                    TodayPlanCard(
                                        item = item,
                                        onQuickCheckIn = { exerciseId, sets, reps ->
                                            viewModel.quickCheckIn(exerciseId, sets, reps)
                                            Toast.makeText(context, "打卡成功!", Toast.LENGTH_SHORT).show()
                                        },
                                        onDetailCheckIn = { exerciseId ->
                                            val today = getDayStart(System.currentTimeMillis())
                                            onNavigateToCheckIn(exerciseId, today)
                                        },
                                        onViewDetail = { exerciseId ->
                                            onNavigateToExerciseDetail(exerciseId)
                                        }
                                    )
                                }
                            }
                        }
                        1 -> {
                            // 挑战计划
                            if (state.activeChallenges.isEmpty()) {
                                item {
                                    EmptyChallengeCard(onAddPlan = onNavigateToPlan)
                                }
                            } else {
                                items(state.activeChallenges, key = { "challenge_${it.challenge.id}" }) { item ->
                                    ChallengeCheckInCard(
                                        item = item,
                                        onCheckIn = { exerciseId, sets, reps ->
                                            viewModel.quickCheckIn(exerciseId, sets, reps)
                                            Toast.makeText(context, "打卡成功! 挑战进度已更新", Toast.LENGTH_SHORT).show()
                                        },
                                        onViewDetail = { exerciseId ->
                                            onNavigateToExerciseDetail(exerciseId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Bottom spacer
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressCard(
    completed: Int,
    total: Int,
    onAddPlan: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        val progress = if (total > 0) completed.toFloat() / total else 0f
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            val progressValue = if (total > 0) completed.toFloat() / total else 0f
            
            Column {
                Text(
                    text = "今日进度",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "$completed",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / $total",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 24.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = progressValue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                
                if (total == 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onAddPlan) {
                        Text(
                            "添加计划开始打卡",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Circular progress indicator
            if (total > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier.size(80.dp),
                        color = Color.White,
                        strokeWidth = 6.dp,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "${(progressValue * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayPlanCard(
    item: TodayPlanItem,
    onQuickCheckIn: (Long, Int, Int) -> Unit,
    onDetailCheckIn: (Long) -> Unit,
    onViewDetail: (Long) -> Unit
) {
    val categoryColor = when (item.exercise.category) {
        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor
        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor
        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor
    }

    val categoryIcon = when (item.exercise.category) {
        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> Icons.Default.FitnessCenter
        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> Icons.Default.FitnessCenter
        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> Icons.Default.DirectionsRun
    }
    
    // 媒体查看器状态
    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }
    
    val firstMedia = item.mediaList.sortedBy { it.orderIndex }.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) 
                Success.copy(alpha = 0.08f) 
            else 
                Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 上半部分：媒体预览或分类图标
            if (firstMedia != null && !item.isCompleted) {
                // 有媒体时显示大图预览
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable {
                            selectedMediaIndex = 0
                            showMediaViewer = true
                        }
                ) {
                    MediaPreviewLarge(
                        media = firstMedia,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // 媒体数量角标
                    if (item.mediaList.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${item.mediaList.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    
                    // 底部渐变遮罩
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                                )
                            )
                    )
                }
            }
            
            // 下半部分：动作信息
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 无媒体或已完成时显示分类图标
                if (firstMedia == null || item.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isCompleted)
                                    Success.copy(alpha = 0.15f)
                                else
                                    categoryColor.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isCompleted) Icons.Default.Check else categoryIcon,
                            contentDescription = null,
                            tint = if (item.isCompleted) Success else categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // 动作信息
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewDetail(item.exercise.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isCompleted) TextSecondary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "查看详情",
                            tint = TextHint,
                            modifier = Modifier.size(18.dp)
                        )
                        if (item.isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Success.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "已完成",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Success,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.weekPlan.targetSets}组 × ${item.weekPlan.targetReps}次",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        
                        if (item.isCompleted) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Success,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.completedSets}组 × ${item.completedReps}次",
                                style = MaterialTheme.typography.bodySmall,
                                color = Success,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // 操作按钮
            if (!item.isCompleted) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onQuickCheckIn(
                                item.exercise.id,
                                item.weekPlan.targetSets,
                                item.weekPlan.targetReps
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("快速打卡", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    
                    OutlinedButton(
                        onClick = { onDetailCheckIn(item.exercise.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = categoryColor)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("详细记录", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    
    // 媒体查看器
    if (showMediaViewer && item.mediaList.isNotEmpty()) {
        MediaViewerDialog(
            mediaList = item.mediaList.sortedBy { it.orderIndex },
            initialIndex = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
    }
}

@Composable
private fun MediaPreviewLarge(
    media: ExerciseMedia,
    modifier: Modifier = Modifier
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
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Surface),
        contentAlignment = Alignment.Center
    ) {
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
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // 视频类型显示播放图标
            if (media.type == MediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = media.type.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = typeColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaThumbnailSmall(
    media: ExerciseMedia,
    size: Int = 48,
    onClick: () -> Unit = {}
) {
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
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

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

@Composable
private fun EmptyPlanCard(onAddPlan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
                Icons.Default.EventNote,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "今日暂无计划",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击下方按钮创建你的健身计划",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddPlan,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加计划")
            }
        }
    }
}

@Composable
private fun EmptyChallengeCard(onAddPlan: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无进行中的挑战",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "创建一个挑战计划来激励自己",
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddPlan,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("创建挑战")
            }
        }
    }
}

private fun getDayStart(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

@Composable
private fun ChallengeCheckInCard(
    item: ChallengePlanWithExercise,
    onCheckIn: (Long, Int, Int) -> Unit,
    onViewDetail: (Long) -> Unit
) {
    val challenge = item.challenge
    val targetReps = challenge.targetTotalReps
    val completedReps = item.completedReps
    val progress = if (targetReps > 0) completedReps.toFloat() / targetReps else 0f
    val isCompleted = completedReps >= targetReps
    
    var showInputDialog by remember { mutableStateOf(false) }
    var inputSets by remember { mutableStateOf(challenge.targetSets.toString()) }
    var inputReps by remember { mutableStateOf(challenge.targetReps.toString()) }
    
    // 媒体查看器状态
    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }

    val categoryColor = when (item.exercise.category) {
        ExerciseCategory.BODYWEIGHT -> BodyweightColor
        ExerciseCategory.STRENGTH -> StrengthColor
        ExerciseCategory.CARDIO -> CardioColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Success.copy(alpha = 0.08f) else Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 媒体缩略图或挑战图标
                val firstMedia = item.mediaList.sortedBy { it.orderIndex }.firstOrNull()
                
                if (firstMedia != null && !isCompleted) {
                    // 有媒体时显示缩略图
                    MediaThumbnailSmall(
                        media = firstMedia,
                        size = 44,
                        onClick = {
                            selectedMediaIndex = 0
                            showMediaViewer = true
                        }
                    )
                } else {
                    // 无媒体或已完成时显示挑战图标
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                (if (isCompleted) Success else Warning).copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (isCompleted) Success else Warning,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewDetail(item.exercise.id) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = challenge.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) TextSecondary else TextPrimary
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "查看详情",
                            tint = TextHint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = item.exercise.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (isCompleted) Success else Warning).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isCompleted) Success else Warning,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$completedReps",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Success else Primary
                    )
                    Text(
                        text = " / $targetReps 次",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress.coerceAtMost(1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isCompleted) Success else Primary,
                trackColor = Divider
            )
            
            // 媒体数量提示（只有多个媒体时才显示）
            if (item.mediaList.size > 1) {
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
                        text = "${item.mediaList.size} 个媒体",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                }
            }
            
            // Check-in button
            if (!isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { showInputDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("打卡记录", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    
    // 媒体查看器
    if (showMediaViewer && item.mediaList.isNotEmpty()) {
        MediaViewerDialog(
            mediaList = item.mediaList.sortedBy { it.orderIndex },
            initialIndex = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
    }
    
    // Input dialog
    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text("打卡记录 - ${item.exercise.name}") },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputSets,
                        onValueChange = { inputSets = it.filter { c -> c.isDigit() } },
                        label = { Text("组数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputReps,
                        onValueChange = { inputReps = it.filter { c -> c.isDigit() } },
                        label = { Text("每组次数") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sets = inputSets.toIntOrNull() ?: 0
                        val reps = inputReps.toIntOrNull() ?: 0
                        if (sets > 0 && reps > 0) {
                            onCheckIn(item.exercise.id, sets, reps)
                            showInputDialog = false
                        }
                    }
                ) {
                    Text("确定", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showInputDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
