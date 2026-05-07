package com.xiaozhi.ai.ui

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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
    onNavigateToSettings: () -> Unit,
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
    val showSubtitles by viewModel.showSubtitles.collectAsState()

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    MainConversationContent(
        state = state,
        isConnected = isConnected,
        hasPermissions = permissionsState.allPermissionsGranted,
        onShowSettings = onNavigateToSettings,
        showActivationDialog = showActivationDialog,
        activationCode = activationCode,
        showSubtitles = showSubtitles,
        onToggleSubtitles = { viewModel.toggleSubtitles() },
        errorMessage = errorMessage,
        viewModel = viewModel
    )
}

@Composable
private fun MainConversationContent(
    state: ConversationState,
    isConnected: Boolean,
    hasPermissions: Boolean,
    onShowSettings: () -> Unit,
    showActivationDialog: Boolean,
    activationCode: String?,
    showSubtitles: Boolean,
    onToggleSubtitles: () -> Unit,
    errorMessage: String?,
    viewModel: ConversationViewModel
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentWindowInsets = WindowInsets.ime,
        containerColor = Color.Transparent,
        topBar = {
            TopBar(
                showSubtitles = showSubtitles,
                onShowSettings = onShowSettings,
                onToggleSubtitles = onToggleSubtitles
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
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(60.dp))
                CenterPanel(state = state, isConnected = isConnected)
                
                Spacer(modifier = Modifier.height(40.dp))
                
                CallControlBar(
                    isConnected = isConnected,
                    onStartCall = { viewModel.connect() },
                    onHangup = { viewModel.disconnect() }
                )
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
    showSubtitles: Boolean,
    onShowSettings: () -> Unit,
    onToggleSubtitles: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clickable(onClick = onShowSettings)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "设置",
                tint = AuraPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = "Aura AI",
            color = AuraPrimary,
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .clickable(onClick = onToggleSubtitles)
                .padding(8.dp)
        ) {
            Icon(
                painter = painterResource(
                    id = if (showSubtitles) com.xiaozhi.ai.R.drawable.subtitle_on else com.xiaozhi.ai.R.drawable.subtitle_off
                ),
                contentDescription = "字幕",
                tint = if (showSubtitles) AuraPrimary else AuraSubText,
                modifier = Modifier.size(24.dp)
            )
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
                .size(240.dp)
                .scale(if (state == ConversationState.LISTENING || state == ConversationState.SPEAKING) scale else 1f)
                .shadow(24.dp, CircleShape, ambientColor = AuraPrimary.copy(alpha = 0.15f))
                .background(Color.White.copy(alpha = 0.3f), CircleShape)
                .border(BorderStroke(4.dp, Color.White), CircleShape)
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(id = com.xiaozhi.ai.R.drawable.doubao),
                contentDescription = "Doubao",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(26.dp))
        Text(
            text = stateTitle(state, isConnected),
            color = AuraText,
            style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Black)
        )
    }
}

private fun stateTitle(state: ConversationState, isConnected: Boolean): String {
    if (!isConnected) {
        return if (state == ConversationState.CONNECTING) "连接中..." else "点击后开始对话"
    }
    return when (state) {
        ConversationState.CONNECTING -> "连接中..."
        ConversationState.LISTENING -> "正在倾听..."
        ConversationState.PROCESSING -> "正在思考..."
        ConversationState.SPEAKING -> "正在回答..."
        ConversationState.IDLE -> "准备就绪"
    }
}

private fun stateSubtitle(state: ConversationState, isConnected: Boolean): String {
    if (!isConnected) {
        return if (state == ConversationState.CONNECTING) "正在建立连接" else ""
    }
    return when (state) {
        ConversationState.CONNECTING -> "正在建立连接"
        ConversationState.LISTENING -> "正在为您服务"
        ConversationState.PROCESSING -> "正在生成回复"
        ConversationState.SPEAKING -> "正在回复您"
        ConversationState.IDLE -> ""
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
private fun CallControlBar(
    isConnected: Boolean,
    onStartCall: () -> Unit,
    onHangup: () -> Unit
) {
    val backgroundColor = if (isConnected) Color(0xFFBD1620) else Color(0xFF4CAF50)
    val icon = if (isConnected) Icons.Outlined.Close else Icons.Outlined.Call
    val onClick = if (isConnected) onHangup else onStartCall

    Box(
        modifier = Modifier
            .padding(vertical = 14.dp)
            .size(82.dp)
            .shadow(8.dp, CircleShape)
            .background(backgroundColor, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = if (isConnected) "挂断" else "通话",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}
