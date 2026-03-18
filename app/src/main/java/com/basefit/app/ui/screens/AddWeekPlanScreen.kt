package com.basefit.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.Exercise
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.PlanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWeekPlanScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlanViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    val selectedDays = remember { mutableStateListOf<Int>() }
    var targetSets by remember { mutableStateOf("") }
    var targetReps by remember { mutableStateOf("") }

    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "添加周计划",
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
            // Exercise selection
            Text(
                text = "选择动作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                onClick = { showExercisePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (selectedExercise != null) {
                        val categoryColor = when (selectedExercise!!.category) {
                            ExerciseCategory.BODYWEIGHT -> BodyweightColor
                            ExerciseCategory.STRENGTH -> StrengthColor
                            ExerciseCategory.CARDIO -> CardioColor
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(categoryColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text(
                                text = selectedExercise!!.name.first().toString(),
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = selectedExercise!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = TextHint
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "点击选择动作",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextHint
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextHint
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Day selection
            Text(
                text = "选择日期",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayNames.forEachIndexed { index, dayName ->
                    val isSelected = selectedDays.contains(index + 1)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                selectedDays.remove(index + 1)
                            } else {
                                selectedDays.add(index + 1)
                            }
                        },
                        label = { 
                            Text(
                                dayName.replace("周", ""),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            ) 
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Target input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = targetSets,
                    onValueChange = { targetSets = it.filter { c -> c.isDigit() } },
                    label = { Text("组数") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = targetReps,
                    onValueChange = { targetReps = it.filter { c -> c.isDigit() } },
                    label = { Text("每组次数") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save button
            Button(
                onClick = {
                    when {
                        selectedExercise == null -> 
                            Toast.makeText(context, "请选择动作", Toast.LENGTH_SHORT).show()
                        selectedDays.isEmpty() -> 
                            Toast.makeText(context, "请选择至少一天", Toast.LENGTH_SHORT).show()
                        targetSets.isBlank() -> 
                            Toast.makeText(context, "请输入组数", Toast.LENGTH_SHORT).show()
                        targetReps.isBlank() -> 
                            Toast.makeText(context, "请输入次数", Toast.LENGTH_SHORT).show()
                        else -> {
                            selectedDays.forEach { day ->
                                viewModel.addWeekPlan(
                                    exerciseId = selectedExercise!!.id,
                                    dayOfWeek = day,
                                    targetSets = targetSets.toInt(),
                                    targetReps = targetReps.toInt()
                                )
                            }
                            Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "保存",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Exercise picker dialog
        if (showExercisePicker) {
            AlertDialog(
                onDismissRequest = { showExercisePicker = false },
                title = { Text("选择动作") },
                text = {
                    if (state.exercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("暂无动作，请先添加动作", color = TextSecondary)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            state.exercises.forEach { exercise ->
                                val categoryColor = when (exercise.category) {
                                    ExerciseCategory.BODYWEIGHT -> BodyweightColor
                                    ExerciseCategory.STRENGTH -> StrengthColor
                                    ExerciseCategory.CARDIO -> CardioColor
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .clickable {
                                            selectedExercise = exercise
                                            showExercisePicker = false
                                        },
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                categoryColor.copy(alpha = 0.15f),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Text(
                                            text = exercise.name.first().toString(),
                                            color = categoryColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = exercise.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showExercisePicker = false }) {
                        Text("关闭")
                    }
                }
            )
        }
    }
}
