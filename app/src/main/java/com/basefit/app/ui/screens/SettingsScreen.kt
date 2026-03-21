package com.basefit.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.Exercise
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.data.repository.FitRepository
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ExerciseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddExercise: () -> Unit,
    viewModel: ExerciseViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showExerciseList by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<Exercise?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "设置",
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
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Exercise Management
                item {
                    SettingsItem(
                        icon = Icons.Default.FitnessCenter,
                        title = "动作管理",
                        subtitle = "添加或删除动作",
                        onClick = { showExerciseList = true }
                    )
                }

                // Export Data
                item {
                    SettingsItem(
                        icon = Icons.Default.FileDownload,
                        title = "导出数据",
                        subtitle = "导出为JSON文件",
                        onClick = {
                            scope.launch {
                                val fileName = "basefit_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                                val repository = FitRepository.getRepository(context)
                                val success = repository.exportData(context, fileName)
                                Toast.makeText(
                                    context,
                                    if (success) "导出成功: $fileName" else "导出失败",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }

                // About
                item {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "关于",
                        subtitle = "BaseFit v1.0",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }

        // Exercise List Dialog
        if (showExerciseList) {
            AlertDialog(
                onDismissRequest = { showExerciseList = false },
                title = { Text("动作管理") },
                text = {
                    Column {
                        if (state.exercises.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无动作，点击下方添加", color = TextSecondary)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.height(300.dp)
                            ) {
                                items(state.exercises, key = { it.id }) { exercise ->
                                    ExerciseListItem(
                                        exercise = exercise,
                                        onDelete = { showDeleteDialog = exercise }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onNavigateToAddExercise) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("添加动作")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExerciseList = false }) {
                        Text("关闭")
                    }
                }
            )
        }

        // Delete Confirmation Dialog
        showDeleteDialog?.let { exercise ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("确认删除") },
                text = { Text("确定要删除「${exercise.name}」吗？相关的计划和记录也会被删除。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteExercise(exercise)
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

        // About Dialog
        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = { 
                    Text(
                        "BaseFit",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "版本 1.0.0",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "一款简洁的健身打卡应用",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "记录你的每一次训练，见证你的成长",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextHint
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = Divider
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "功能特点：",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("• 自定义动作管理", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("• 灵活的周计划设置", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("• 挑战计划激励", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            Text("• 数据统计与导出", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAboutDialog = false }) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextHint
            )
        }
    }
}

@Composable
private fun ExerciseListItem(
    exercise: Exercise,
    onDelete: () -> Unit
) {
    val categoryColor = when (exercise.category) {
        ExerciseCategory.BODYWEIGHT -> BodyweightColor
        ExerciseCategory.STRENGTH -> StrengthColor
        ExerciseCategory.CARDIO -> CardioColor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = exercise.name.first().toString(),
                color = categoryColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleSmall,
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

        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                tint = Error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
