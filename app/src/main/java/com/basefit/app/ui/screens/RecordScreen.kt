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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onNavigateToCheckIn: (Long, Long) -> Unit,
    viewModel: RecordViewModel = viewModel(),
    bottomBarPadding: PaddingValues = PaddingValues()
) {
    val state by viewModel.state.collectAsState()
    val selectedDate = state.selectedDate
    val lifecycleOwner = LocalLifecycleOwner.current

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

    val displayedCheckIns = remember(state.checkIns, selectedDate) {
        if (selectedDate != null) {
            state.checkIns.filter { it.checkIn.date == selectedDate }
        } else {
            state.checkIns
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "打卡记录",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, "上个月")
                    }
                    Text(
                        text = "${state.currentYear}年${state.currentMonth}月",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, "下个月")
                    }
                }
            }

            item {
                val selDate = selectedDate
                CalendarGrid(
                    year = state.currentYear,
                    month = state.currentMonth,
                    checkInData = state.calendarData,
                    selectedDate = selDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedDate != null) {
                            val dateFormat = SimpleDateFormat("M月d日", Locale.getDefault())
                            "${dateFormat.format(Date(selectedDate))}的记录"
                        } else {
                            "本月记录"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (selectedDate != null) {
                        TextButton(onClick = { viewModel.selectDate(null) }) {
                            Text("查看全部", color = MaterialTheme.colorScheme.primary)
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "清除选择",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (displayedCheckIns.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedDate != null) "当天暂无记录" else "本月暂无记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(displayedCheckIns, key = { it.checkIn.id }) { item ->
                    CheckInRecordCard(
                        item = item,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    checkInData: Map<Long, Int>,
    selectedDate: Long?,
    onDateSelected: (Long?) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            weekDays.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val totalCells = ((daysInMonth + offset + 6) / 7) * 7
        var dayCounter = 1

        for (cellIndex in 0 until totalCells step 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (rowCell in 0 until 7) {
                    val index = cellIndex + rowCell

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index >= offset && dayCounter <= daysInMonth) {
                            val currentDay = dayCounter

                            calendar.set(year, month - 1, currentDay, 0, 0, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            val dayTimestamp = calendar.timeInMillis
                            val checkInCount = checkInData[dayTimestamp] ?: 0

                            val today = Calendar.getInstance()
                            val isToday = today.get(Calendar.YEAR) == year &&
                                    today.get(Calendar.MONTH) == month - 1 &&
                                    today.get(Calendar.DAY_OF_MONTH) == currentDay

                            val isSelected = selectedDate == dayTimestamp

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            checkInCount > 0 -> Success.copy(alpha = 0.2f)
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        if (checkInCount > 0) {
                                            onDateSelected(if (isSelected) null else dayTimestamp)
                                        } else {
                                            val dateFormat = SimpleDateFormat("M月d日", Locale.getDefault())
                                            Toast.makeText(context, "${dateFormat.format(Date(dayTimestamp))}暂无打卡记录", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$currentDay",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        checkInCount > 0 -> Success
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isToday || checkInCount > 0 || isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInRecordCard(
    item: CheckInWithExercise,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (item.exercise.category) {
        ExerciseCategory.BODYWEIGHT -> BodyweightColor
        ExerciseCategory.STRENGTH -> StrengthColor
        ExerciseCategory.CARDIO -> CardioColor
    }

    val dateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.exercise.name.first().toString(),
                    color = categoryColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.exercise.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.checkIn.completedSets}组 × ${item.checkIn.completedReps}次" +
                            if (item.checkIn.weight != null) " · ${item.checkIn.weight}kg" else "" +
                            if (item.checkIn.durationMinutes != null) " · ${item.checkIn.durationMinutes}分钟" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = dateFormat.format(Date(item.checkIn.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}