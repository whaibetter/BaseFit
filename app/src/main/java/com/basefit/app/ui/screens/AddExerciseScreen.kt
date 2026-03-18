package com.basefit.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.ExerciseCategory
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExerciseViewModel = viewModel()
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExerciseCategory.BODYWEIGHT) }

    val categories = listOf(
        ExerciseCategory.BODYWEIGHT to "自重训练",
        ExerciseCategory.STRENGTH to "力量训练",
        ExerciseCategory.CARDIO to "有氧运动"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "添加动作",
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
                .padding(16.dp)
        ) {
            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("动作名称") },
                placeholder = { Text("例如：俯卧撑") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category selection
            Text(
                text = "选择分类",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            categories.forEach { (category, label) ->
                val color = when (category) {
                    ExerciseCategory.BODYWEIGHT -> BodyweightColor
                    ExerciseCategory.STRENGTH -> StrengthColor
                    ExerciseCategory.CARDIO -> CardioColor
                }

                val isSelected = selectedCategory == category

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) color.copy(alpha = 0.1f) else Surface
                    ),
                    onClick = { selectedCategory = category }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = color,
                                unselectedColor = TextHint
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) color else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Preset exercises suggestion
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "常见动作参考",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val suggestions = when (selectedCategory) {
                        ExerciseCategory.BODYWEIGHT -> listOf("俯卧撑", "仰卧起坐", "深蹲", "引体向上", "平板支撑", "波比跳")
                        ExerciseCategory.STRENGTH -> listOf("哑铃弯举", "杠铃卧推", "硬拉", "哑铃推举", "划船", "腿举")
                        ExerciseCategory.CARDIO -> listOf("跑步", "跳绳", "骑行", "游泳", "登山机", "椭圆机")
                    }

                    suggestions.chunked(3).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { name = suggestion },
                                    label = { Text(suggestion) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining space
                            repeat(3 - row.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "请输入动作名称", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    viewModel.addExercise(name.trim(), selectedCategory)
                    Toast.makeText(context, "添加成功", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
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
    }
}
