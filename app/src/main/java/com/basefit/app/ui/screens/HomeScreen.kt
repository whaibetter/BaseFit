package com.basefit.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToPlan: () -> Unit,
    onNavigateToCheckIn: (Long, Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
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
                contentColor = Color.White
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

            // Today's Plan
            Text(
                text = "今日计划",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.todayPlans.isEmpty()) {
                EmptyPlanCard(onAddPlan = onNavigateToPlan)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.todayPlans, key = { it.weekPlan.id }) { item ->
                        TodayPlanCard(
                            item = item,
                            onQuickCheckIn = { exerciseId, sets, reps ->
                                viewModel.quickCheckIn(exerciseId, sets, reps)
                                Toast.makeText(context, "打卡成功!", Toast.LENGTH_SHORT).show()
                            },
                            onDetailCheckIn = { exerciseId ->
                                val today = getDayStart(System.currentTimeMillis())
                                onNavigateToCheckIn(exerciseId, today)
                            }
                        )
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
    onDetailCheckIn: (Long) -> Unit
) {
    val categoryColor = when (item.exercise.category) {
        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor
        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor
        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) 
                Success.copy(alpha = 0.1f) 
            else 
                Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.exercise.name.first().toString(),
                    color = categoryColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "目标: ${item.weekPlan.targetSets}组 × ${item.weekPlan.targetReps}次",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (item.isCompleted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "已完成: ${item.completedSets}组 × ${item.completedReps}次",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            }

            // Action buttons
            if (item.isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "已完成",
                    tint = Success,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Row {
                    // Quick check-in
                    IconButton(
                        onClick = {
                            onQuickCheckIn(
                                item.exercise.id,
                                item.weekPlan.targetSets,
                                item.weekPlan.targetReps
                            )
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Primary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "快速打卡",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Detail check-in
                    IconButton(
                        onClick = { onDetailCheckIn(item.exercise.id) },
                        modifier = Modifier
                            .size(40.dp)
                            .background(TextSecondary.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "详细打卡",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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

private fun getDayStart(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
