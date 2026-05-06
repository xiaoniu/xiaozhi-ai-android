package com.xiaozhi.ai.ui

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xiaozhi.ai.data.ConfigManager
import com.xiaozhi.ai.viewmodel.ConversationState
import com.xiaozhi.ai.viewmodel.ConversationViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val AuraBgTop = Color(0xFFF8F9FF)
private val AuraBgBottom = Color(0xFFFBF5FF)
private val AuraPrimary = Color(0xFF674BB5)
private val AuraSecondary = Color(0xFF64A8FE)
private val AuraTertiary = Color(0xFFF170B4)
private val AuraText = Color(0xFF121C2A)
private val AuraSubText = Color(0xFF6E6A78)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel = viewModel()
) {
    val context = LocalContext.current

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS
        )
    )

    val state by viewModel.state.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showActivationDialog by viewModel.showActivationDialog.collectAsState()
    val activationCode by viewModel.activationCode.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var currentConfig by remember {
        mutableStateOf(ConfigManager(context).loadConfig())
    }

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (showSettings) {
        SettingsScreen(
            config = currentConfig,
            onConfigChange = { newConfig ->
                currentConfig = newConfig
                ConfigManager(context).saveConfig(newConfig)
                viewModel.updateConfig(newConfig)
                showSettings = false
            },
            onBack = { showSettings = false }
        )
    } else {
        MainConversationContent(
            state = state,
            isConnected = isConnected,
            textInput = textInput,
            onTextInputChange = { textInput = it },
            hasPermissions = permissionsState.allPermissionsGranted,
            onShowSettings = { showSettings = true },
            showActivationDialog = showActivationDialog,
            activationCode = activationCode,
            isMuted = isMuted,
            onToggleMute = { viewModel.toggleMute() },
            errorMessage = errorMessage,
            viewModel = viewModel
        )
    }
}

@Composable
private fun MainConversationContent(
    state: ConversationState,
    isConnected: Boolean,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    hasPermissions: Boolean,
    onShowSettings: () -> Unit,
    showActivationDialog: Boolean,
    activationCode: String?,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    errorMessage: String?,
    viewModel: ConversationViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentWindowInsets = WindowInsets.ime,
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                isMuted = isMuted,
                onShowSettings = onShowSettings,
                onToggleMute = onToggleMute
            )
        },
        bottomBar = {
            BottomInputBar(
                textInput = textInput,
                onTextChange = onTextInputChange,
                focusRequester = focusRequester,
                onKeyboardClick = {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
                onSendText = {
                    if (textInput.isNotBlank()) {
                        viewModel.sendTextMessage(textInput)
                        onTextInputChange("")
                    }
                },
                onHangup = { viewModel.interrupt() }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(AuraBgTop, AuraBgBottom)
                    )
                )
                .padding(padding)
        ) {
            AuroraDecor()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                CenterPanel(state = state, isConnected = isConnected)
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFBA1A1A),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 132.dp, start = 24.dp, end = 24.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showActivationDialog && activationCode != null) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "设备激活",
                    color = AuraText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = "激活码：", color = AuraSubText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF3EEFF)
                    ) {
                        Text(
                            text = activationCode,
                            modifier = Modifier.padding(16.dp),
                            color = AuraPrimary,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onActivationConfirmed() },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraPrimary)
                ) {
                    Text("我已激活", color = Color.White)
                }
            },
            containerColor = Color(0xFFFDFBFF)
        )
    }
}

@Composable
private fun TopBar(
    isMuted: Boolean,
    onShowSettings: () -> Unit,
    onToggleMute: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.45f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShowSettings, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "设置",
                        tint = AuraPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Aura AI",
                    color = AuraPrimary,
                    style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                )
            }

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.75f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "字幕",
                        color = AuraSubText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (isMuted) Color(0xFFE9E7F3) else Color(0xFFE9E9FF)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(onClick = onToggleMute, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "CC",
                                    color = if (isMuted) Color(0xFF7A7583) else AuraPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuroraDecor() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopCenter)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AuraPrimary.copy(alpha = 0.16f),
                            AuraSecondary.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(540.dp)
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AuraPrimary.copy(alpha = 0.14f),
                            AuraTertiary.copy(alpha = 0.09f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun CenterPanel(
    state: ConversationState,
    isConnected: Boolean
) {
    val pulse = rememberInfiniteTransition(label = "orbPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(288.dp)
                .scale(if (state == ConversationState.LISTENING || state == ConversationState.SPEAKING) scale else 1f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color(0xFFF0E8FF).copy(alpha = 0.68f)
                        )
                    ),
                    shape = CircleShape
                )
                .shadow(24.dp, CircleShape, ambientColor = AuraPrimary.copy(alpha = 0.15f))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
                    val bars = listOf(28.dp, 54.dp, 78.dp, 42.dp, 64.dp)
                    bars.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(10.dp)
                                .height(h)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            AuraPrimary.copy(alpha = 0.45f),
                                            AuraPrimary.copy(alpha = 0.88f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = stateTitle(state, isConnected),
            color = AuraText,
            style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Black)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stateSubtitle(state, isConnected),
            color = AuraSubText,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFD8D5E2), CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "您可以开始说话",
            color = AuraSubText.copy(alpha = 0.45f),
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium)
        )
    }
}

private fun stateTitle(state: ConversationState, isConnected: Boolean): String {
    if (!isConnected) return "连接中..."
    return when (state) {
        ConversationState.CONNECTING -> "连接中..."
        ConversationState.LISTENING -> "正在倾听..."
        ConversationState.PROCESSING -> "正在思考..."
        ConversationState.SPEAKING -> "正在回答..."
        ConversationState.IDLE -> "准备就绪"
    }
}

private fun stateSubtitle(state: ConversationState, isConnected: Boolean): String {
    if (!isConnected) return "请稍候"
    return when (state) {
        ConversationState.CONNECTING -> "正在建立连接"
        ConversationState.LISTENING -> "Aura 正在为您服务"
        ConversationState.PROCESSING -> "正在生成回复"
        ConversationState.SPEAKING -> "Aura 正在回复您"
        ConversationState.IDLE -> "点击麦克风开始"
    }
}

@Composable
private fun SmallGlassAction(
    icon: @Composable () -> Unit,
    enablePulse: Boolean = false,
    onClick: () -> Unit,
    contentOverlay: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        color = if (enablePulse) Color(0x40FFFFFF) else Color(0x2AFFFFFF),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.65f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
                icon()
            }
            contentOverlay?.invoke()
        }
    }
}

@Composable
private fun HoldToTalkOverlay(
    isConnected: Boolean,
    hasPermissions: Boolean,
    viewModel: ConversationViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(isConnected, hasPermissions) {
                awaitEachGesture {
                    awaitFirstDown()
                    if (!isConnected || !hasPermissions) {
                        return@awaitEachGesture
                    }

                    pressed = true
                    var started = false
                    longPressJob = coroutineScope.launch {
                        delay(180)
                        if (pressed) {
                            viewModel.startListening()
                            started = true
                        }
                    }

                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })

                    pressed = false
                    longPressJob?.cancel()
                    if (started) {
                        viewModel.stopListening()
                    }
                }
            }
    )
}

@Composable
private fun BottomInputBar(
    textInput: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onKeyboardClick: () -> Unit,
    onSendText: () -> Unit,
    onHangup: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(64.dp),
            shape = RoundedCornerShape(36.dp),
            color = Color.White.copy(alpha = 0.82f),
            tonalElevation = 0.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (textInput.isBlank()) {
                        Text(
                            text = "输入消息...",
                            color = AuraSubText.copy(alpha = 0.5f),
                            style = TextStyle(fontSize = 16.sp)
                        )
                    }
                    BasicTextField(
                        value = textInput,
                        onValueChange = onTextChange,
                        singleLine = true,
                        textStyle = TextStyle(color = AuraText, fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSendText() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
                IconButton(onClick = onKeyboardClick) {
                    Icon(
                        imageVector = Icons.Outlined.Keyboard,
                        contentDescription = "键盘",
                        tint = AuraSubText
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(82.dp),
                shape = CircleShape,
                color = Color(0xFFBD1620),
                shadowElevation = 8.dp
            ) {
                IconButton(onClick = onHangup, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "END",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
