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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.basefit.app.data.demo.DemoData
import com.basefit.app.data.entity.BodyMetric
import com.basefit.app.data.entity.BodyMetricType
import com.basefit.app.data.entity.BodyMetricTrend
import com.basefit.app.data.entity.NormalRange
import com.basefit.app.data.repository.FitRepository
import com.basefit.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FitRepository.getRepository(context) }

    var selectedMetricType by remember { mutableStateOf<BodyMetricType?>(null) }
    var latestMetrics by remember { mutableStateOf<Map<BodyMetricType, BodyMetric>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTrendPeriod by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        val actualMetrics = repository.getAllLatestMetrics()
        latestMetrics = if (actualMetrics.isEmpty()) {
            DemoData.getDemoBodyMetrics()
        } else {
            actualMetrics
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("身体指标", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加记录", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        if (isLoading) {
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
                    Text(
                        text = "指标总览",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(BodyMetricType.entries.toList()) { type ->
                            val metric = latestMetrics[type]
                            val normalRange = NormalRange.getRange(type)
                            val isNormal = metric?.let {
                                normalRange?.let { range -> it.value in range.min..range.max }
                            } ?: true

                            MetricOverviewCard(
                                type = type,
                                value = metric?.value,
                                unit = metric?.unit ?: getUnitForType(type),
                                isNormal = isNormal,
                                isRecorded = metric != null,
                                onClick = { selectedMetricType = type }
                            )
                        }
                    }
                }

                selectedMetricType?.let { type ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricDetailSection(
                            type = type,
                            repository = repository,
                            selectedPeriod = selectedTrendPeriod,
                            onPeriodSelected = { selectedTrendPeriod = it },
                            latestMetric = latestMetrics[type]
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    NormalRangeGuideSection()
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AddMetricDialog(
            onDismiss = { showAddDialog = false },
            onSave = { type, value, notes ->
                scope.launch {
                    val metric = BodyMetric(
                        type = type,
                        value = value,
                        unit = getUnitForType(type),
                        recordDate = System.currentTimeMillis(),
                        source = "manual",
                        notes = notes
                    )
                    repository.saveBodyMetric(metric)
                    latestMetrics = repository.getAllLatestMetrics()
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
private fun MetricOverviewCard(
    type: BodyMetricType,
    value: Float?,
    unit: String,
    isNormal: Boolean,
    isRecorded: Boolean,
    onClick: () -> Unit
) {
    val color = when {
        !isRecorded -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        isNormal -> Success
        else -> Warning
    }

    Card(
        modifier = Modifier
            .width(100.dp)
            .height(120.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isRecorded) color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = getIconForType(type),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = getDisplayNameForType(type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (value != null) "${formatValue(value)}$unit" else "--",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRecorded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            if (isRecorded && !isNormal) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Warning.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "异常",
                        style = MaterialTheme.typography.labelSmall,
                        color = Warning,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricDetailSection(
    type: BodyMetricType,
    repository: FitRepository,
    selectedPeriod: Int,
    onPeriodSelected: (Int) -> Unit,
    latestMetric: BodyMetric?
) {
    var trendData by remember { mutableStateOf<BodyMetricTrend?>(null) }
    val periodDays = listOf(7, 30, 90, 365)

    LaunchedEffect(type, selectedPeriod) {
        trendData = repository.getBodyMetricTrendData(type, periodDays[selectedPeriod])
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = getDisplayNameForType(type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                latestMetric?.let { metric ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "最新: ${formatValue(metric.value)}${metric.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("7天", "30天", "90天", "1年").forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedPeriod == index,
                        onClick = { onPeriodSelected(index) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(28.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            trendData?.let { trend ->
                trend.change?.let { change ->
                    val isPositive = change >= 0
                    val changeColor = when {
                        type == BodyMetricType.WEIGHT || type == BodyMetricType.BODY_FAT -> if (isPositive) Warning else Success
                        type == BodyMetricType.STEPS || type == BodyMetricType.SLEEP -> if (isPositive) Success else Warning
                        else -> if (isPositive) Success else Warning
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "较上期${if (isPositive) "增长" else "下降"} ${formatValue(abs(change))}${trend.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = changeColor
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (trend.dataPoints.isNotEmpty()) {
                    MetricTrendChart(
                        dataPoints = trend.dataPoints.map { it.value },
                        labels = trend.dataPoints.map {
                            SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(it.date))
                        },
                        normalMin = trend.normalMin,
                        normalMax = trend.normalMax,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                } else {
                    EmptyChartPlaceholder()
                }
            }
        }
    }
}

@Composable
private fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.ShowChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "点击右下角添加记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricTrendChart(
    dataPoints: List<Float>,
    labels: List<String>,
    normalMin: Float?,
    normalMax: Float?,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val normalRangeColor = Success.copy(alpha = 0.2f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (dataPoints.isEmpty()) return@Canvas

            val width = size.width
            val height = size.height
            val minValue = (dataPoints.minOrNull() ?: 0f).coerceAtLeast(normalMin ?: 0f) * 0.9f
            val maxValue = (dataPoints.maxOrNull() ?: 100f).coerceAtMost(normalMax ?: 100f) * 1.1f
            val valueRange = (maxValue - minValue).coerceAtLeast(1f)
            val stepX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

            normalMin?.let { min ->
                normalMax?.let { max ->
                    val minY = height - ((min - minValue) / valueRange * height)
                    val maxY = height - ((max - minValue) / valueRange * height)
                    drawRect(
                        color = normalRangeColor,
                        topLeft = Offset(0f, maxY),
                        size = androidx.compose.ui.geometry.Size(width, minY - maxY)
                    )
                }
            }

            val path = Path()
            dataPoints.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - minValue) / valueRange * height)
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(width = 3.dp.toPx())
            )

            dataPoints.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - minValue) / valueRange * height)
                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEachIndexed { index, label ->
                if (index == 0 || index == labels.lastIndex || labels.size <= 5) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        normalMin?.let { min ->
            normalMax?.let { max ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Success.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "正常范围: $min - $max",
                            style = MaterialTheme.typography.labelSmall,
                            color = Success,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NormalRangeGuideSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "健康指标参考范围",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            NormalRange.NORMAL_RANGES.take(6).forEach { range ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getIconForType(range.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getDisplayNameForType(range.type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${range.min} - ${range.max} ${range.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMetricDialog(
    onDismiss: () -> Unit,
    onSave: (BodyMetricType, Float, String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(BodyMetricType.WEIGHT) }
    var value by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "添加记录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = getDisplayNameForType(selectedType),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("指标类型") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        leadingIcon = {
                            Icon(
                                getIconForType(selectedType),
                                contentDescription = null
                            )
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BodyMetricType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            getIconForType(type),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(getDisplayNameForType(type))
                                    }
                                },
                                onClick = {
                                    selectedType = type
                                    value = ""
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("数值") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = { Text(getUnitForType(selectedType)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                NormalRange.getRange(selectedType)?.let { range ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "正常范围: ${range.min} - ${range.max} ${range.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            val numValue = value.toFloatOrNull()
                            if (numValue == null) {
                                error = "请输入有效数值"
                            } else {
                                error = null
                                onSave(selectedType, numValue, notes.ifBlank { null })
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

private fun getDisplayNameForType(type: BodyMetricType): String {
    return when (type) {
        BodyMetricType.WEIGHT -> "体重"
        BodyMetricType.HEIGHT -> "身高"
        BodyMetricType.BMI -> "BMI"
        BodyMetricType.BODY_FAT -> "体脂率"
        BodyMetricType.MUSCLE_MASS -> "肌肉量"
        BodyMetricType.BMR -> "基础代谢"
        BodyMetricType.STEPS -> "步数"
        BodyMetricType.SLEEP -> "睡眠"
        BodyMetricType.HEART_RATE -> "心率"
        BodyMetricType.BLOOD_PRESSURE_SYSTOLIC -> "收缩压"
        BodyMetricType.BLOOD_PRESSURE_DIASTOLIC -> "舒张压"
    }
}

private fun getUnitForType(type: BodyMetricType): String {
    return when (type) {
        BodyMetricType.WEIGHT, BodyMetricType.MUSCLE_MASS -> "kg"
        BodyMetricType.HEIGHT -> "cm"
        BodyMetricType.BMI -> ""
        BodyMetricType.BODY_FAT, BodyMetricType.BMR -> "%"
        BodyMetricType.STEPS -> "步"
        BodyMetricType.SLEEP -> "小时"
        BodyMetricType.HEART_RATE -> "bpm"
        BodyMetricType.BLOOD_PRESSURE_SYSTOLIC, BodyMetricType.BLOOD_PRESSURE_DIASTOLIC -> "mmHg"
    }
}

private fun getIconForType(type: BodyMetricType): ImageVector {
    return when (type) {
        BodyMetricType.WEIGHT -> Icons.Default.MonitorWeight
        BodyMetricType.HEIGHT -> Icons.Default.Height
        BodyMetricType.BMI -> Icons.Default.Calculate
        BodyMetricType.BODY_FAT -> Icons.Default.Percent
        BodyMetricType.MUSCLE_MASS -> Icons.Default.FitnessCenter
        BodyMetricType.BMR -> Icons.Default.LocalFireDepartment
        BodyMetricType.STEPS -> Icons.Default.DirectionsWalk
        BodyMetricType.SLEEP -> Icons.Default.Bedtime
        BodyMetricType.HEART_RATE -> Icons.Default.Favorite
        BodyMetricType.BLOOD_PRESSURE_SYSTOLIC, BodyMetricType.BLOOD_PRESSURE_DIASTOLIC -> Icons.Default.Bloodtype
    }
}

private fun formatValue(value: Float): String {
    return if (value == value.toLong().toFloat()) {
        value.toLong().toString()
    } else {
        String.format("%.1f", value)
    }
}