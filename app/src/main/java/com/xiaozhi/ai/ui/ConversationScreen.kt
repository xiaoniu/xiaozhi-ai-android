package com.xiaozhi.ai.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.xiaozhi.ai.R
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.xiaozhi.ai.ui.theme.TechLightBlue80
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xiaozhi.ai.data.ConfigManager
import com.xiaozhi.ai.data.Message
import com.xiaozhi.ai.data.MessageRole
import com.xiaozhi.ai.ui.theme.ConnectedGreen
import com.xiaozhi.ai.ui.theme.ConnectionRed
import com.xiaozhi.ai.ui.theme.DarkColorScheme
import com.xiaozhi.ai.utils.ConfigValidator
import com.xiaozhi.ai.viewmodel.ConversationState
import com.xiaozhi.ai.viewmodel.ConversationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configManager = remember { ConfigManager(context) }
    val configValidator = remember { ConfigValidator(context) }

    // 权限管理
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    )

    // 状态收集
    val state by viewModel.state.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showActivationDialog by viewModel.showActivationDialog.collectAsState()
    val activationCode by viewModel.activationCode.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    // 文本输入状态和导航状态
    var textInput by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var currentConfig by remember {
        mutableStateOf(configManager.loadConfig())
    }
    val listState = rememberLazyListState()

    // 自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 请求权限
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // 显示设置页面或主界面
    if (showSettings) {
        SettingsScreen(
            config = currentConfig,
            onConfigChange = { newConfig ->
                currentConfig = newConfig
                configManager.saveConfig(newConfig)
                // 更新ViewModel中的配置
                viewModel.updateConfig(newConfig)
                showSettings = false
            },
            onBack = {
                showSettings = false
            }
        )
    } else {
        MainConversationContent(
            state = state,
            isConnected = isConnected,
            messages = messages,
            errorMessage = errorMessage,
            textInput = textInput,
            onTextInputChange = { textInput = it },
            listState = listState,
            hasPermissions = permissionsState.allPermissionsGranted,
            onShowSettings = { showSettings = true },
            showActivationDialog = showActivationDialog,
            activationCode = activationCode,
            isMuted = isMuted,
            onToggleMute = { viewModel.toggleMute() },
            viewModel = viewModel
        )
    }
}

@Composable
fun MainConversationContent(
    state: ConversationState,
    isConnected: Boolean,
    messages: List<Message>,
    errorMessage: String?,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    hasPermissions: Boolean,
    onShowSettings: () -> Unit,
    showActivationDialog: Boolean,
    activationCode: String?,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    viewModel: ConversationViewModel
) {

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        containerColor = Color.White, // 设置背景为白色
        topBar = {
            // 顶部标题栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White, // 白色背景
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：留空
                    Spacer(modifier = Modifier.width(48.dp))

                    // 右侧：功能按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 历史记录按钮 (替换原来的设置按钮位置，暂时映射到设置功能)
                        IconButton(
                            onClick = onShowSettings,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "历史记录",
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF1F2937) // Gray 900
                            )
                        }

                        // 静音按钮
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                ),
                                modifier = Modifier.size(24.dp),
                                contentDescription = if (isMuted) "取消静音" else "静音",
                                tint = Color(0xFF1F2937) // Gray 900
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                // 底部输入区域
                PrototypeBottomInputArea(
                    textInput = textInput,
                    onTextChange = onTextInputChange,
                    onSendText = {
                        if (textInput.isNotBlank()) {
                            viewModel.sendTextMessage(textInput)
                            onTextInputChange("")
                        }
                    },
                    state = state,
                    isConnected = isConnected,
                    hasPermissions = hasPermissions,
                    viewModel = viewModel
                )
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 错误消息显示
            errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color(0xFFFEF2F2), // Light Red
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        IconButton(
                            onClick = { viewModel.clearError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    MessageItem(message = message)
                }
            }
        }
    }

    // 激活弹窗
    if (showActivationDialog && activationCode != null) {
        AlertDialog(
            onDismissRequest = { /* 不允许点击外部关闭 */ },
            title = {
                Text(
                    text = "设备激活",
                    fontWeight = FontWeight.Bold,
                    color = DarkColorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "激活码：",
                        color = DarkColorScheme.onSurface,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = DarkColorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = activationCode,
                            modifier = Modifier.padding(16.dp),
                            color = DarkColorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onActivationConfirmed() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkColorScheme.primary
                    )
                ) {
                    Text(
                        text = "我已激活",
                        color = DarkColorScheme.onPrimary
                    )
                }
            },
            containerColor = DarkColorScheme.surface,
            titleContentColor = DarkColorScheme.onSurface,
            textContentColor = DarkColorScheme.onSurface
        )
    }
}

@Composable
fun MessageItem(message: Message) {
    val isUser = message.role == MessageRole.USER
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI头像 - 更小更简洁
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White), // 确保头像背景清晰
                contentAlignment = Alignment.Center
            ) {
                 Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground), // 使用应用图标
                    contentDescription = null,
                    tint = Color.Unspecified, // 保持原色
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            Surface(
                color = if (isUser)
                    Color(0xFF3B82F6) // Blue 500
                else
                    Color.White,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 18.dp
                ),
                shadowElevation = 1.dp,
                border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)) else null // Gray 200 border for AI
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (isUser)
                        Color.White
                    else
                        Color(0xFF1F2937), // Gray 900
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }

            Text(
                text = timeFormat.format(Date(message.timestamp)),
                fontSize = 10.sp,
                color = Color(0xFF9CA3AF), // Gray 400
                modifier = Modifier.padding(
                    start = if (isUser) 0.dp else 8.dp,
                    end = if (isUser) 8.dp else 0.dp,
                    top = 4.dp
                )
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // 用户头像
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB)), // Gray 200
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF6B7280), // Gray 500
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PrototypeBottomInputArea(
    textInput: String,
    onTextChange: (String) -> Unit,
    onSendText: () -> Unit,
    state: ConversationState,
    isConnected: Boolean,
    hasPermissions: Boolean,
    viewModel: ConversationViewModel
) {
    // 底部背景：白色渐变到透明（这里简化为白色背景，上方有一点阴影或渐变）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.0f),
                        Color.White
                    ),
                    startY = 0f,
                    endY = 50f
                )
            )
            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 状态指示器
            AnimatedVisibility(
                visible = state != ConversationState.IDLE,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (state) {
                        ConversationState.LISTENING -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFEF4444), CircleShape) // Red 500
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "正在聆听...",
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        ConversationState.PROCESSING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1F2937)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "思考中...",
                                color = Color(0xFF1F2937),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        ConversationState.SPEAKING -> {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF1F2937), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "正在回复...",
                                color = Color(0xFF1F2937),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        else -> {}
                    }
                }
            }

            // 药丸形输入栏
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(8.dp, CircleShape, spotColor = Color(0x14000000)),
                shape = CircleShape,
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF3F4F6))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧相机按钮
                    IconButton(
                        onClick = { /* TODO: Camera action */ },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Transparent, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = "相机",
                            tint = Color(0xFF1F2937), // Gray 900
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // 中间输入框
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textInput.isEmpty()) {
                            Text(
                                text = "发消息或按住说话...",
                                color = Color(0xFF9CA3AF), // Gray 400
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = textInput,
                            onValueChange = onTextChange,
                            textStyle = TextStyle(
                                color = Color(0xFF374151), // Gray 700
                                fontSize = 15.sp
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { onSendText() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 右侧按钮组
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 语音按钮 (带长按逻辑)
                        VoiceActionButton(
                            isConnected = isConnected,
                            hasPermissions = hasPermissions,
                            viewModel = viewModel
                        )

                        // 加号按钮
                        IconButton(
                            onClick = { /* TODO: More actions */ },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "更多",
                                tint = Color(0xFF1F2937),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceActionButton(
    isConnected: Boolean,
    hasPermissions: Boolean,
    viewModel: ConversationViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var isPressed by remember { mutableStateOf(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    // 录音按钮
    IconButton(
        onClick = { /* Click handled by pointerInput below for consistency */ },
        modifier = Modifier
            .size(40.dp)
            .background(
                if (isPressed) Color(0xFFF3F4F6) else Color.Transparent,
                CircleShape
            )
            .pointerInput(hasPermissions, isConnected) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (hasPermissions && isConnected) {
                        isPressed = true
                        var started = false
                        longPressJob = coroutineScope.launch {
                            delay(200) // 稍微延迟防误触
                            if (isPressed) {
                                viewModel.startListening()
                                started = true
                            }
                        }

                        // 等待抬起
                        do {
                            val event = awaitPointerEvent()
                            // 这里可以添加上滑取消逻辑，为简化暂略
                        } while (event.changes.any { it.pressed })

                        isPressed = false
                        longPressJob?.cancel()
                        if (started) {
                            viewModel.stopListening()
                        }
                    }
                }
            }
    ) {
        Icon(
            imageVector = Icons.Outlined.Mic,
            contentDescription = "语音",
            tint = if (isPressed) Color(0xFF3B82F6) else Color(0xFF1F2937), // Blue 500 when pressed
            modifier = Modifier.size(20.dp)
        )
    }
}
