package com.basefit.app.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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

    val lazyListState = rememberLazyListState()
    val collapseProgress = remember { derivedStateOf {
        val firstVisibleItem = lazyListState.firstVisibleItemIndex
        val firstVisibleOffset = lazyListState.firstVisibleItemScrollOffset
        if (firstVisibleItem > 0) 1f
        else (firstVisibleOffset / 200f).coerceIn(0f, 1f)
    }}

    val selectedTab = state.selectedTab
    val tabs = listOf("今日计划", "进行中的挑战")
    val challengeCount = state.activeChallenges.size

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadTodayPlans(isInitial = false)
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToPlan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            CollapsibleProgressCard(
                completed = state.completedCount,
                total = state.totalCount,
                collapseProgress = collapseProgress.value,
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
                            onClick = { viewModel.selectTab(index) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        title,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "$count",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
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

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleProgressCard(
    completed: Int,
    total: Int,
    collapseProgress: Float,
    onAddPlan: () -> Unit
) {
    val progressValue = if (total > 0) completed.toFloat() / total else 0f

    val expandedHeight = 140
    val collapsedHeight = 56
    val targetHeight = (collapsedHeight + (expandedHeight - collapsedHeight) * (1 - collapseProgress)).toInt()
    val animatedHeight by animateIntAsState(
        targetValue = targetHeight,
        label = "progressCardHeight"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(animatedHeight.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (collapseProgress < 0.5f) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "今日进度",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$completed",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " / $total",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }

                    if (total > 0) {
                        Box(
                            modifier = Modifier.size(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = progressValue,
                                modifier = Modifier.size(72.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 5.dp,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                            )
                            Text(
                                text = "${(progressValue * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "今日进度",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    LinearProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.onPrimary,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "$completed / $total",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (total > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(progressValue * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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

    var showMediaViewer by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf(0) }

    val firstMedia = item.mediaList.sortedBy { it.orderIndex }.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (firstMedia != null && !item.isCompleted) {
                    MediaThumbnailSmall(
                        media = firstMedia,
                        size = 44,
                        onClick = {
                            selectedMediaIndex = 0
                            showMediaViewer = true
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                (if (item.isCompleted) Success else categoryColor).copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
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
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onViewDetail(item.exercise.id) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "查看详情",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${item.weekPlan.targetSets}组 × ${item.weekPlan.targetReps}次",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = (if (item.isCompleted) Success else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (item.isCompleted) "已完成" else "待打卡",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.isCompleted) Success else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (item.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "已完成 ${item.completedSets}组 × ${item.completedReps}次",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Success,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (item.mediaList.size > 1 && !item.isCompleted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.mediaList.size} 个媒体",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!item.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = categoryColor)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("快速打卡", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { onDetailCheckIn(item.exercise.id) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, categoryColor.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = categoryColor)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("详细记录", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showMediaViewer && item.mediaList.isNotEmpty()) {
        MediaViewerDialog(
            mediaList = item.mediaList.sortedBy { it.orderIndex },
            initialIndex = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "今日暂无计划",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击下方按钮创建你的健身计划",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddPlan,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无进行中的挑战",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "创建一个挑战计划来激励自己",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddPlan,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
            containerColor = if (isCompleted) Success.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val firstMedia = item.mediaList.sortedBy { it.orderIndex }.firstOrNull()

                if (firstMedia != null && !isCompleted) {
                    MediaThumbnailSmall(
                        media = firstMedia,
                        size = 44,
                        onClick = {
                            selectedMediaIndex = 0
                            showMediaViewer = true
                        }
                    )
                } else {
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
                            color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "查看详情",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = item.exercise.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = if (isCompleted) Success else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " / $targetReps 次",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = if (isCompleted) Success else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (item.mediaList.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.mediaList.size} 个媒体",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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

    if (showMediaViewer && item.mediaList.isNotEmpty()) {
        MediaViewerDialog(
            mediaList = item.mediaList.sortedBy { it.orderIndex },
            initialIndex = selectedMediaIndex,
            onDismiss = { showMediaViewer = false }
        )
    }

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
                    Text("确定", color = MaterialTheme.colorScheme.primary)
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