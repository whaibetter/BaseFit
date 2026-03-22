package com.basefit.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.ChallengePlan
import com.basefit.app.data.entity.WeekPlan
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddWeekPlan: () -> Unit,
    onNavigateToAddChallenge: () -> Unit,
    viewModel: PlanViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("周计划", "挑战计划")
    
    // 监听页面恢复，自动刷新数据
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
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
                        "健身计划",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    if (selectedTab == 0) onNavigateToAddWeekPlan() 
                    else onNavigateToAddChallenge() 
                },
                containerColor = Primary,
                contentColor = Surface
            ) {
                Icon(Icons.Default.Add, "添加")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Surface,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> WeekPlanTab(
                        plans = state.weekPlans,
                        onDelete = { viewModel.deleteWeekPlan(it) }
                    )
                    1 -> ChallengePlanTab(
                        challenges = state.challenges,
                        onDelete = { viewModel.deleteChallenge(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekPlanTab(
    plans: List<WeekPlanWithExercise>,
    onDelete: (WeekPlan) -> Unit
) {
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    if (plans.isEmpty()) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = "暂无周计划",
            subtitle = "点击右下角按钮添加计划"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Group by day of week
            val groupedPlans = plans.groupBy { it.weekPlan.dayOfWeek }
            
            groupedPlans.keys.sorted().forEach { dayOfWeek ->
                item {
                    Text(
                        text = dayNames[dayOfWeek - 1],
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(groupedPlans[dayOfWeek] ?: emptyList()) { item ->
                    WeekPlanCard(
                        item = item,
                        onDelete = { onDelete(item.weekPlan) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekPlanCard(
    item: WeekPlanWithExercise,
    onDelete: () -> Unit
) {
    val categoryColor = when (item.exercise.category) {
        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor
        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor
        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(categoryColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.weekPlan.targetSets}组 × ${item.weekPlan.targetReps}次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = Error
                )
            }
        }
    }
}

@Composable
private fun ChallengePlanTab(
    challenges: List<ChallengePlanWithExercise>,
    onDelete: (ChallengePlan) -> Unit
) {
    if (challenges.isEmpty()) {
        EmptyState(
            icon = Icons.Default.EmojiEvents,
            title = "暂无挑战计划",
            subtitle = "开始你的第一个挑战吧!"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(challenges, key = { it.challenge.id }) { item ->
                ChallengeCard(
                    item = item,
                    onDelete = { onDelete(item.challenge) }
                )
            }
        }
    }
}

@Composable
private fun ChallengeCard(
    item: ChallengePlanWithExercise,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    val now = System.currentTimeMillis()
    val challenge = item.challenge
    val isActive = now in challenge.startDate..challenge.endDate
    val isPast = now > challenge.endDate

    // 使用次数进度
    val targetReps = challenge.targetTotalReps
    val completedReps = item.completedReps
    val progress = if (targetReps > 0) completedReps.toFloat() / targetReps else 0f
    val isCompleted = completedReps >= targetReps

    // 计算天数
    val totalDays = ((challenge.endDate - challenge.startDate) / (24 * 60 * 60 * 1000) + 1).toInt()

    val categoryColor = when (item.exercise.category) {
        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor
        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor
        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor
    }

    val statusColor = when {
        isCompleted -> Success
        isPast -> TextSecondary
        isActive -> Warning
        else -> TextHint
    }

    val statusText = when {
        isCompleted -> "已完成"
        isPast -> "已结束"
        isActive -> "进行中"
        else -> "未开始"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) 
                Success.copy(alpha = 0.08f) 
            else if (isPast) 
                Surface.copy(alpha = 0.5f) 
            else 
                Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category icon
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                categoryColor.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (isCompleted) Success else categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = challenge.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPast && !isCompleted) TextSecondary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.exercise.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress section (show for active or completed challenges)
            if (isActive || isCompleted) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "已完成",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$completedReps",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Success else Primary
                            )
                            Text(
                                text = " / $targetReps 次",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }
                    
                    // Percentage
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (isCompleted) Success else Primary).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) Success else Primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = progress.coerceAtMost(1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = if (isCompleted) Success else Primary,
                    trackColor = Divider,
                )
                
                // Daily suggestion
                if (challenge.targetSets > 0 && challenge.targetReps > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "建议: ${challenge.targetSets}组 x ${challenge.targetReps}次/天",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextHint
                    )
                }
            }

            // Date range
            Spacer(modifier = Modifier.height(12.dp))
            val totalDaysVal = totalDays
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${dateFormat.format(Date(challenge.startDate))} - ${dateFormat.format(Date(challenge.endDate))} (${totalDaysVal}天)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = if (isPast && !isCompleted) TextHint else Error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextHint
            )
        }
    }
}