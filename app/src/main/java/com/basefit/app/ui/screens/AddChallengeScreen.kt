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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.Exercise
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.PlanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChallengeScreen(
    onNavigateBack: () -> Unit,
    viewModel: PlanViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var challengeName by remember { mutableStateOf("") }
    var targetDays by remember { mutableStateOf("") }
    var targetSets by remember { mutableStateOf("") }
    var targetReps by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    val presetDays = listOf(7, 14, 21, 30, 60, 90)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "添加挑战",
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

            // Challenge name
            OutlinedTextField(
                value = challengeName,
                onValueChange = { challengeName = it },
                label = { Text("挑战名称") },
                placeholder = { Text("例如：30天俯卧撑挑战") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Challenge duration
            Text(
                text = "挑战天数",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetDays.forEach { days ->
                    val isSelected = targetDays == days.toString()
                    FilterChip(
                        selected = isSelected,
                        onClick = { targetDays = days.toString() },
                        label = { Text("${days}天") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetDays,
                onValueChange = { targetDays = it.filter { c -> c.isDigit() } },
                label = { Text("自定义天数") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Start date
            Text(
                text = "开始日期",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = Primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = dateFormat.format(Date(startDate)),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextHint
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
                    label = { Text("每天组数") },
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
                        challengeName.isBlank() -> 
                            Toast.makeText(context, "请输入挑战名称", Toast.LENGTH_SHORT).show()
                        targetDays.isBlank() -> 
                            Toast.makeText(context, "请输入挑战天数", Toast.LENGTH_SHORT).show()
                        targetSets.isBlank() -> 
                            Toast.makeText(context, "请输入组数", Toast.LENGTH_SHORT).show()
                        targetReps.isBlank() -> 
                            Toast.makeText(context, "请输入次数", Toast.LENGTH_SHORT).show()
                        else -> {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = startDate
                            calendar.set(Calendar.HOUR_OF_DAY, 0)
                            calendar.set(Calendar.MINUTE, 0)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            val start = calendar.timeInMillis
                            calendar.add(Calendar.DAY_OF_MONTH, targetDays.toInt() - 1)
                            val end = calendar.timeInMillis

                            viewModel.addChallenge(
                                exerciseId = selectedExercise!!.id,
                                name = challengeName.trim(),
                                startDate = start,
                                endDate = end,
                                targetSets = targetSets.toInt(),
                                targetReps = targetReps.toInt()
                            )
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
                    "开始挑战",
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

        // Date picker dialog
        if (showDatePicker) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = startDate
            
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = startDate
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                startDate = it
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("取消")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
