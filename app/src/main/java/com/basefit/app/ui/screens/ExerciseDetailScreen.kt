package com.basefit.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basefit.app.data.entity.ResourceType
import com.basefit.app.ui.components.ResourceUploadSection
import com.basefit.app.ui.theme.*
import com.basefit.app.viewmodel.ExerciseDetailViewModel
import com.basefit.app.utils.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ExerciseDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    
    var description by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var resourceToDelete by remember { mutableStateOf<com.basefit.app.data.entity.ExerciseResource?>(null) }

    LaunchedEffect(exerciseId) {
        viewModel.loadExercise(exerciseId)
    }

    LaunchedEffect(state.exercise) {
        description = state.exercise?.description ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        state.exercise?.name ?: "动作详情",
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
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                state.exercise?.let { exercise ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (exercise.category) {
                                        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> Icons.Default.FitnessCenter
                                        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> Icons.Default.MonitorWeight
                                        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> Icons.Default.DirectionsRun
                                    },
                                    contentDescription = null,
                                    tint = when (exercise.category) {
                                        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor
                                        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor
                                        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when (exercise.category) {
                                    com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor.copy(alpha = 0.1f)
                                    com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor.copy(alpha = 0.1f)
                                    com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor.copy(alpha = 0.1f)
                                }
                            ) {
                                Text(
                                    text = when (exercise.category) {
                                        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> "自重训练"
                                        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> "力量训练"
                                        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> "有氧运动"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (exercise.category) {
                                        com.basefit.app.data.entity.ExerciseCategory.BODYWEIGHT -> BodyweightColor
                                        com.basefit.app.data.entity.ExerciseCategory.STRENGTH -> StrengthColor
                                        com.basefit.app.data.entity.ExerciseCategory.CARDIO -> CardioColor
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("动作描述") },
                        placeholder = { Text("添加动作描述、注意事项等...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Description, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.updateExerciseDescription(exerciseId, description)
                                Toast.makeText(context, "描述已保存", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存描述")
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Surface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ResourceUploadSection(
                        exerciseId = exerciseId,
                        resources = state.resources,
                        onAddResource = { type, path, thumbnail, name, size, duration ->
                            viewModel.addResource(type, path, thumbnail, name, size, duration)
                            Toast.makeText(context, "资源已添加", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteResource = { resource ->
                            resourceToDelete = resource
                            showDeleteConfirm = true
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.resources.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "资源列表",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                state.resources.forEach { resource ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = when (resource.resourceType) {
                                                ResourceType.GIF -> Icons.Default.Gif
                                                ResourceType.IMAGE -> Icons.Default.Image
                                                ResourceType.VIDEO -> Icons.Default.VideoFile
                                                ResourceType.LINK -> Icons.Default.Link
                                            },
                                            contentDescription = null,
                                            tint = when (resource.resourceType) {
                                                ResourceType.GIF -> Purple40
                                                ResourceType.IMAGE -> Green40
                                                ResourceType.VIDEO -> Orange40
                                                ResourceType.LINK -> Blue40
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = resource.displayName ?: when (resource.resourceType) {
                                                    ResourceType.GIF -> "GIF动图"
                                                    ResourceType.IMAGE -> "图片"
                                                    ResourceType.VIDEO -> "视频"
                                                    ResourceType.LINK -> "外部链接"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextPrimary
                                            )
                                            
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                resource.fileSize?.let { size ->
                                                    Text(
                                                        text = FileUtils.formatFileSize(size),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                                resource.duration?.let { duration ->
                                                    Text(
                                                        text = FileUtils.formatDuration(duration),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = {
                                                resourceToDelete = resource
                                                showDeleteConfirm = true
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = androidx.compose.ui.graphics.Color.Red
                                            )
                                        }
                                    }
                                    
                                    if (resource != state.resources.last()) {
                                        Divider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = Background
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && resourceToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm = false
                resourceToDelete = null
            },
            title = { Text("确认删除") },
            text = { 
                Text("确定要删除这个资源吗？${resourceToDelete?.displayName?.let { "\n$it" } ?: ""}") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resourceToDelete?.let { viewModel.deleteResource(it) }
                        Toast.makeText(context, "资源已删除", Toast.LENGTH_SHORT).show()
                        showDeleteConfirm = false
                        resourceToDelete = null
                    }
                ) {
                    Text("删除", color = androidx.compose.ui.graphics.Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    resourceToDelete = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}
