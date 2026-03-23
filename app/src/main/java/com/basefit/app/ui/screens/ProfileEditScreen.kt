package com.basefit.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.basefit.app.data.entity.ProfileEditHistory
import com.basefit.app.data.entity.UserProfile
import com.basefit.app.data.repository.FitRepository
import com.basefit.app.data.utils.AvatarManager
import com.basefit.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { FitRepository.getRepository(context) }

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var avatarBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSavingAvatar by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAvatarOptionsDialog by remember { mutableStateOf(false) }
    var editHistory by remember { mutableStateOf<List<ProfileEditHistory>>(emptyList()) }

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<Long?>(null) }
    var avatarPath by remember { mutableStateOf<String?>(null) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }

    var showGenderMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            scope.launch {
                isSavingAvatar = true
                try {
                    val savedPath = AvatarManager.processAndSaveAvatar(
                        context = context,
                        sourceUri = selectedUri,
                        userId = 1
                    )
                    if (savedPath != null) {
                        avatarPath = savedPath
                        avatarBitmap = AvatarManager.LocalStorageAdapter(context).loadAvatar(1)
                        Toast.makeText(context, "头像更新成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "头像保存失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "头像处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSavingAvatar = false
                }
            }
        }
        showAvatarOptionsDialog = false
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let { takenBitmap ->
            scope.launch {
                isSavingAvatar = true
                try {
                    val circularBitmap = AvatarManager.createCircularBitmap(takenBitmap)
                    val storageAdapter = AvatarManager.LocalStorageAdapter(context)
                    val savedPath = storageAdapter.saveAvatar(circularBitmap, 1)
                    if (savedPath != null) {
                        avatarPath = savedPath
                        avatarBitmap = circularBitmap
                        Toast.makeText(context, "头像更新成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "头像保存失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "头像处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isSavingAvatar = false
                }
            }
        }
        showAvatarOptionsDialog = false
    }

    LaunchedEffect(Unit) {
        profile = repository.getUserProfileOnce()
        profile?.let {
            name = it.name
            phone = it.phone ?: ""
            email = it.email ?: ""
            gender = it.gender ?: ""
            birthDate = it.birthDate
            avatarPath = it.avatarPath
            avatarBitmap = it.avatarPath?.let { path ->
                AvatarManager.LocalStorageAdapter(context).loadAvatar(1)
            }
        }
        isLoading = false
    }

    LaunchedEffect(showHistoryDialog) {
        if (showHistoryDialog) {
            repository.getRecentProfileHistory(20).collect {
                editHistory = it
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    if (showAuthDialog) {
        AuthDialog(
            onDismiss = { showAuthDialog = false },
            onAuthenticated = {
                showAuthDialog = false
                scope.launch {
                    val oldProfile = profile ?: UserProfile()
                    val newProfile = UserProfile(
                        id = 1,
                        name = name.trim(),
                        phone = phone.trim().ifEmpty { null },
                        email = email.trim().ifEmpty { null },
                        gender = gender.ifEmpty { null },
                        birthDate = birthDate,
                        avatarPath = avatarPath,
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateUserProfileWithHistory(oldProfile, newProfile)
                    profile = newProfile
                    Toast.makeText(context, "修改成功", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showHistoryDialog) {
        ProfileEditHistoryDialog(
            history = editHistory,
            dateFormat = dateFormat,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = birthDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    birthDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
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

    if (showAvatarOptionsDialog) {
        AvatarOptionsDialog(
            onDismiss = { showAvatarOptionsDialog = false },
            onSelectFromGallery = {
                galleryLauncher.launch("image/*")
            },
            onTakePhoto = {
                cameraLauncher.launch(null)
            },
            onRemoveAvatar = {
                scope.launch {
                    AvatarManager.LocalStorageAdapter(context).deleteAvatar(1)
                    avatarBitmap = null
                    avatarPath = null
                    Toast.makeText(context, "头像已移除", Toast.LENGTH_SHORT).show()
                }
                showAvatarOptionsDialog = false
            },
            hasAvatar = avatarBitmap != null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑资料", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.History, contentDescription = "修改历史")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSavingAvatar) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else if (avatarBitmap != null) {
                                        Image(
                                            bitmap = avatarBitmap!!.asImageBitmap(),
                                            contentDescription = "头像",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (avatarBitmap != null) {
                                    TextButton(
                                        onClick = { showAvatarOptionsDialog = true }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("更换头像")
                                    }
                                    TextButton(
                                        onClick = {
                                            scope.launch {
                                                AvatarManager.LocalStorageAdapter(context).deleteAvatar(1)
                                                avatarBitmap = null
                                                avatarPath = null
                                                Toast.makeText(context, "头像已移除", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("移除", color = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    Button(
                                        onClick = { showAvatarOptionsDialog = true }
                                    ) {
                                        Icon(
                                            Icons.Default.AddAPhoto,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("添加头像")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "基本信息",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    nameError = null
                                },
                                label = { Text("姓名 *") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = nameError != null,
                                supportingText = nameError?.let { { Text(it) } },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            ExposedDropdownMenuBox(
                                expanded = showGenderMenu,
                                onExpandedChange = { showGenderMenu = it }
                            ) {
                                OutlinedTextField(
                                    value = when (gender) {
                                        "male" -> "男"
                                        "female" -> "女"
                                        "other" -> "其他"
                                        else -> ""
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("性别") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showGenderMenu)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Wc, contentDescription = null)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = showGenderMenu,
                                    onDismissRequest = { showGenderMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("男") },
                                        onClick = {
                                            gender = "male"
                                            showGenderMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("女") },
                                        onClick = {
                                            gender = "female"
                                            showGenderMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("其他") },
                                        onClick = {
                                            gender = "other"
                                            showGenderMenu = false
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = birthDate?.let { dateFormat.format(Date(it)) } ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("出生日期") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Cake, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "联系方式",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    phone = it.filter { c -> c.isDigit() || c == '+' }
                                    phoneError = null
                                },
                                label = { Text("手机号码") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = phoneError != null,
                                supportingText = phoneError?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null)
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    emailError = null
                                },
                                label = { Text("电子邮箱") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = emailError != null,
                                supportingText = emailError?.let { { Text(it) } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                }
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            var hasError = false
                            if (name.isBlank()) {
                                nameError = "姓名不能为空"
                                hasError = true
                            } else if (name.length < 2) {
                                nameError = "姓名至少2个字符"
                                hasError = true
                            }
                            if (phone.isNotBlank() && !isValidPhone(phone)) {
                                phoneError = "请输入有效的手机号码"
                                hasError = true
                            }
                            if (email.isNotBlank() && !isValidEmail(email)) {
                                emailError = "请输入有效的邮箱地址"
                                hasError = true
                            }

                            if (!hasError) {
                                showAuthDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存修改", fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AvatarOptionsDialog(
    onDismiss: () -> Unit,
    onSelectFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveAvatar: () -> Unit,
    hasAvatar: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("更换头像") },
        text = {
            Column {
                ListItem(
                    headlineContent = { Text("从相册选择") },
                    leadingContent = {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onSelectFromGallery)
                )
                ListItem(
                    headlineContent = { Text("拍照") },
                    leadingContent = {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onTakePhoto)
                )
                if (hasAvatar) {
                    Divider()
                    ListItem(
                        headlineContent = {
                            Text("移除头像", color = MaterialTheme.colorScheme.error)
                        },
                        leadingContent = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable(onClick = onRemoveAvatar)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AuthDialog(
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val correctPin = "1234"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "身份验证",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请输入PIN码确认修改",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text("PIN码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
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
                            if (pin == correctPin) {
                                onAuthenticated()
                            } else {
                                error = "PIN码错误，请重试"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("确认")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditHistoryDialog(
    history: List<ProfileEditHistory>,
    dateFormat: SimpleDateFormat,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "修改历史",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "暂无修改记录",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(history) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = getFieldDisplayName(item.fieldName),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = dateFormat.format(Date(item.editedAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        Text(
                                            text = item.oldValue ?: "空",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = item.newValue ?: "空",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
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
}

private fun getFieldDisplayName(fieldName: String): String {
    return when (fieldName) {
        "name" -> "姓名"
        "phone" -> "手机号码"
        "email" -> "电子邮箱"
        "gender" -> "性别"
        "birthDate" -> "出生日期"
        "avatar" -> "头像"
        else -> fieldName
    }
}

private fun isValidPhone(phone: String): Boolean {
    val phoneRegex = "^[+]?[0-9]{10,15}$".toRegex()
    return phoneRegex.matches(phone)
}

private fun isValidEmail(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return emailRegex.matches(email)
}