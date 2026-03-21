package com.basefit.app.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    viewModel: RecordViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

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
                    containerColor = Background,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Background),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Month selector
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
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, "下个月")
                    }
                }
            }

            // Calendar
            item {
                CalendarGrid(
                    year = state.currentYear,
                    month = state.currentMonth,
                    checkInData = state.calendarData,
                    selectedDate = state.selectedDate,
                    onDayClick = { day ->
                        val calendar = Calendar.getInstance()
                        calendar.set(state.currentYear, state.currentMonth - 1, day, 0, 0, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val dayTimestamp = calendar.timeInMillis
                        viewModel.selectDate(dayTimestamp)
                    }
                )
            }

            // Selected date records or monthly records
            if (state.selectedDate != null) {
                // Selected date header
                item {
                    val selectedDateFormat = SimpleDateFormat("MM月dd日 EEEE", Locale.getDefault())
                    val selectedDateText = selectedDateFormat.format(Date(state.selectedDate!!))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "选中日期",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = selectedDateText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Primary
                                )
                            }
                            TextButton(onClick = { viewModel.clearSelectedDate() }) {
                                Text("清除选择")
                            }
                        }
                    }
                }

                // Selected date check-ins
                if (state.selectedDateCheckIns.isNotEmpty()) {
                    items(state.selectedDateCheckIns, key = { "selected_${it.checkIn.id}" }) { item ->
                        CheckInRecordCard(
                            item = item,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "该日期暂无打卡记录",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextHint
                            )
                        }
                    }
                }
            } else {
                // Monthly records header
                item {
                    Text(
                        text = "本月记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
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
                } else if (state.checkIns.isEmpty()) {
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
                                    tint = TextHint,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "本月暂无记录",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(state.checkIns, key = { "month_${it.checkIn.id}" }) { item ->
                        CheckInRecordCard(
                            item = item,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Bottom spacer
            item {
                Spacer(modifier = Modifier.height(16.dp))
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
    onDayClick: (Int) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    // Convert Sunday=1 to Monday=0
    val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Week day headers
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
                        color = TextSecondary
                    )
                }
            }
        }

        // Calendar days
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

                            // Get timestamp for this day
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
                                            isSelected -> Primary
                                            checkInCount > 0 -> Success.copy(alpha = 0.2f)
                                            isToday -> Primary.copy(alpha = 0.1f)
                                            else -> Surface
                                        }
                                    )
                                    .clickable { onDayClick(currentDay) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$currentDay",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = when {
                                        isSelected -> Color.White
                                        checkInCount > 0 -> Success
                                        isToday -> Primary
                                        else -> TextPrimary
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
        colors = CardDefaults.cardColors(containerColor = Surface)
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
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.checkIn.completedSets}组 × ${item.checkIn.completedReps}次" +
                            if (item.checkIn.weight != null) " · ${item.checkIn.weight}kg" else "" +
                            if (item.checkIn.durationMinutes != null) " · ${item.checkIn.durationMinutes}分钟" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Text(
                text = dateFormat.format(Date(item.checkIn.date)),
                style = MaterialTheme.typography.bodySmall,
                color = TextHint
            )
        }
    }
}