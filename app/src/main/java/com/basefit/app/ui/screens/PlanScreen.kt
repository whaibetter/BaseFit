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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("周计划", "挑战计划")

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
    val isActive = now in item.challenge.startDate..item.challenge.endDate
    val isPast = now > item.challenge.endDate

    val totalDays = ((item.challenge.endDate - item.challenge.startDate) / (24 * 60 * 60 * 1000) + 1).toInt()
    val passedDays = ((now - item.challenge.startDate) / (24 * 60 * 60 * 1000) + 1).coerceIn(0L, totalDays.toLong()).toInt()
    val progress = if (totalDays > 0) passedDays.toFloat() / totalDays else 0f

    val categoryColor = when (item.exercise.category) {
        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor
        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor
        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor
    }

    val statusColor = when {
        isPast -> TextSecondary
        isActive -> Success
        else -> Warning
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) Surface.copy(alpha = 0.5f) else Surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(40.dp)
                            .background(categoryColor, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.challenge.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPast) TextSecondary else TextPrimary
                        )
                        Text(
                            text = item.exercise.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isPast -> "已结束"
                            isActive -> "进行中"
                            else -> "未开始"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = if (isPast) TextHint else Error
                        )
                    }
                }
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.challenge.targetSets}组 × ${item.challenge.targetReps}次/天",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "$passedDays/$totalDays 天",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = Primary,
                    trackColor = Divider
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${dateFormat.format(Date(item.challenge.startDate))} - ${dateFormat.format(Date(item.challenge.endDate))}",
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
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
