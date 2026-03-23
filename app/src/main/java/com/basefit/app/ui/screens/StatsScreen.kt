package com.basefit.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.*
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.StatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = viewModel(),
    bottomBarPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsState()
    var selectedTrendTab by remember { mutableStateOf(0) }
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "统计成就",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStats() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ComparisonSection(
                        weekComparison = state.weekComparison,
                        monthComparison = state.monthComparison
                    )
                }

                item {
                    TrendSection(
                        weeklyTrend = state.weeklyTrend,
                        monthlyTrend = state.monthlyTrend,
                        selectedTab = selectedTrendTab,
                        onTabSelected = { selectedTrendTab = it }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryDistributionCard(
                            modifier = Modifier.weight(1f),
                            distributions = state.categoryDistribution
                        )
                        DifficultyDistributionCard(
                            modifier = Modifier.weight(1f),
                            distributions = state.difficultyDistribution
                        )
                    }
                }

                state.milestoneStats?.let { milestones ->
                    item {
                        MilestoneProgressSection(milestones = milestones)
                    }
                }

                item {
                    Text(
                        text = "动作成就",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (state.achievements.isEmpty()) {
                    item {
                        EmptyAchievementsCard()
                    }
                } else {
                    items(state.achievements, key = { it.exerciseId }) { achievement ->
                        AchievementCard(
                            achievement = achievement,
                            onClick = { selectedAchievement = achievement }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    selectedAchievement?.let { achievement ->
        AchievementDetailDialog(
            achievement = achievement,
            onDismiss = { selectedAchievement = null }
        )
    }
}

@Composable
private fun ComparisonSection(
    weekComparison: ComparisonData?,
    monthComparison: ComparisonData?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.CalendarMonth,
            value = "${weekComparison?.thisPeriod ?: 0}",
            label = "本周打卡天数",
            color = MaterialTheme.colorScheme.primary,
            comparison = weekComparison
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.LocalFireDepartment,
            value = "${monthComparison?.thisPeriod ?: 0}",
            label = "本月活跃天数",
            color = Warning,
            comparison = monthComparison
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color,
    comparison: ComparisonData? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            comparison?.let {
                Spacer(modifier = Modifier.height(4.dp))
                val changeColor = if (it.isPositive) Success else Error
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (it.isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = changeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (it.isPositive) "+" else ""}${it.change}天",
                        style = MaterialTheme.typography.labelSmall,
                        color = changeColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendSection(
    weeklyTrend: List<TrendDataPoint>,
    monthlyTrend: List<TrendDataPoint>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val trendData = if (selectedTab == 0) weeklyTrend else monthlyTrend
    val maxValue = trendData.maxOfOrNull { it.value } ?: 1

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "打卡趋势",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("周", "月").forEachIndexed { index, title ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = { onTabSelected(index) },
                            label = { Text(title, fontSize = 12.sp) },
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LineChart(
                data = trendData.map { it.value },
                labels = trendData.map { it.label },
                maxValue = maxValue.coerceAtLeast(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
        }
    }
}

@Composable
private fun LineChart(
    data: List<Int>,
    labels: List<String>,
    maxValue: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (data.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val stepX = width / (data.size - 1).coerceAtLeast(1)
            val scaleY = height / maxValue

            val path = Path()
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value * scaleY)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value * scaleY)
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryDistributionCard(
    modifier: Modifier = Modifier,
    distributions: List<CategoryDistribution>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "运动类型",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (distributions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                DonutChart(
                    data = distributions.map { it.percentage },
                    colors = listOf(BodyweightColor, StrengthColor, CardioColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    distributions.forEachIndexed { index, dist ->
                        val color = when (dist.category) {
                            ExerciseCategory.BODYWEIGHT -> BodyweightColor
                            ExerciseCategory.STRENGTH -> StrengthColor
                            ExerciseCategory.CARDIO -> CardioColor
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (dist.category) {
                                    ExerciseCategory.BODYWEIGHT -> "自重"
                                    ExerciseCategory.STRENGTH -> "力量"
                                    ExerciseCategory.CARDIO -> "有氧"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${dist.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas

        val strokeWidth = 20.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        var startAngle = -90f
        data.forEachIndexed { index, value ->
            val sweepAngle = value * 3.6f
            drawArc(
                color = colors.getOrElse(index) { onSurfaceVariantColor },
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun DifficultyDistributionCard(
    modifier: Modifier = Modifier,
    distributions: List<DifficultyDistribution>
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "成就难度",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (distributions.isEmpty() || distributions.all { it.count == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val maxCount = distributions.maxOfOrNull { it.count } ?: 1
                    distributions.forEach { dist ->
                        val color = when (dist.difficulty) {
                            AchievementDifficulty.EASY -> Success
                            AchievementDifficulty.MEDIUM -> Warning
                            AchievementDifficulty.HARD -> Error
                            AchievementDifficulty.EXTREME -> MaterialTheme.colorScheme.primary
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = dist.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(32.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = (dist.count.toFloat() / maxCount).coerceAtLeast(0.1f))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(color)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${dist.count}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(16.dp),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneProgressSection(milestones: MilestoneStats) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "里程碑进度",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${milestones.completedMilestones}/${milestones.totalMilestones}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(milestones.milestones) { milestone ->
                    MilestoneItem(milestone = milestone)
                }
            }
        }
    }
}

@Composable
private fun MilestoneItem(milestone: MilestoneInfo) {
    val color = when (milestone.difficulty) {
        AchievementDifficulty.EASY -> Success
        AchievementDifficulty.MEDIUM -> Warning
        AchievementDifficulty.HARD -> Error
        AchievementDifficulty.EXTREME -> MaterialTheme.colorScheme.primary
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = milestone.progress,
                modifier = Modifier.fillMaxSize(),
                color = if (milestone.isCompleted) Success else color,
                trackColor = color.copy(alpha = 0.15f),
                strokeWidth = 5.dp
            )
            if (milestone.isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Success,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "${(milestone.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = milestone.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!milestone.isCompleted) {
            Text(
                text = "${milestone.remaining}${milestone.unit}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun EmptyAchievementsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "开始打卡解锁成就",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    onClick: () -> Unit
) {
    val categoryColor = when (achievement.category) {
        ExerciseCategory.BODYWEIGHT -> BodyweightColor
        ExerciseCategory.STRENGTH -> StrengthColor
        ExerciseCategory.CARDIO -> CardioColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (achievement.category) {
                                ExerciseCategory.BODYWEIGHT -> Icons.Default.FitnessCenter
                                ExerciseCategory.STRENGTH -> Icons.Default.FitnessCenter
                                ExerciseCategory.CARDIO -> Icons.Default.DirectionsRun
                            },
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = achievement.exerciseName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (achievement.category) {
                                ExerciseCategory.BODYWEIGHT -> "自重训练"
                                ExerciseCategory.STRENGTH -> "力量训练"
                                ExerciseCategory.CARDIO -> "有氧运动"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (achievement.currentStreak > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Warning.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Warning,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${achievement.currentStreak}天",
                                style = MaterialTheme.typography.labelMedium,
                                color = Warning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementStat(label = "打卡", value = "${achievement.totalCheckIns}")
                AchievementStat(label = "组数", value = "${achievement.totalSets}")
                AchievementStat(label = "次数", value = "${achievement.totalReps}")
                achievement.maxWeight?.let {
                    AchievementStat(label = "重量", value = "${it.toInt()}kg")
                }
            }

            if (achievement.bestStreak > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "最佳连续: ${achievement.bestStreak}天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "点击查看详情 >",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AchievementStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementDetailDialog(
    achievement: Achievement,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val categoryColor = when (achievement.category) {
                    ExerciseCategory.BODYWEIGHT -> BodyweightColor
                    ExerciseCategory.STRENGTH -> StrengthColor
                    ExerciseCategory.CARDIO -> CardioColor
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CheckCircle,
                        value = "${achievement.totalCheckIns}",
                        label = "打卡次数",
                        color = Success
                    )
                    DetailStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        value = "${achievement.currentStreak}",
                        label = "当前连续",
                        color = Warning
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DetailStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        value = "${achievement.totalSets}",
                        label = "总组数",
                        color = categoryColor
                    )
                    DetailStatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Repeat,
                        value = "${achievement.totalReps}",
                        label = "总次数",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    achievement.maxWeight?.let { weight ->
                        DetailStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.MonitorWeight,
                            value = "${weight.toInt()}kg",
                            label = "最大重量",
                            color = StrengthColor
                        )
                    }
                    achievement.maxDuration?.let { duration ->
                        DetailStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            value = "${duration}min",
                            label = "最长时长",
                            color = CardioColor
                        )
                    }
                    if (achievement.maxWeight == null && achievement.maxDuration == null) {
                        DetailStatCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Star,
                            value = "${achievement.bestStreak}",
                            label = "最佳连续",
                            color = Success
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "时间统计",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "首次打卡",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (achievement.firstCheckInDate > 0)
                                        dateFormat.format(Date(achievement.firstCheckInDate))
                                    else "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "最近打卡",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (achievement.lastCheckInDate > 0)
                                        dateFormat.format(Date(achievement.lastCheckInDate))
                                    else "-",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}