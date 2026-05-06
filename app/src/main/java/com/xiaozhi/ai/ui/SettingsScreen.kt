package com.xiaozhi.ai.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaozhi.ai.data.ConfigManager
import com.xiaozhi.ai.data.XiaozhiConfig

// Aetheric Harmony Design System Colors
object AethericColors {
    val Primary = Color(0xFF674BB5)
    val PrimaryContainer = Color(0xFFA78BFA)
    val Secondary = Color(0xFF0060AC)
    val SecondaryContainer = Color(0xFF64A8FE)
    val Background = Color(0xFFF8F9FF)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFEFF4FF)
    val OnSurface = Color(0xFF121C2A)
    val OnSurfaceVariant = Color(0xFF494552)
    val OutlineVariant = Color(0xFFCAC4D4)
    val OnPrimary = Color(0xFFFFFFFF)
    
    val AuroraGradients = listOf(
        Color(0xFFE8DDFF),
        Color(0xFFD4E3FF),
        Color(0xFFFFD8E7),
        Color(0xFFE6EEFF)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: XiaozhiConfig,
    onConfigChange: (XiaozhiConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { ConfigManager(context) }
    var editedConfig by remember { 
        mutableStateOf(
            if (config.otaUrl.isBlank() && config.websocketUrl.isBlank()) {
                config.copy(
                    name = "Android",
                    otaUrl = "https://api.tenclass.net/xiaozhi/ota/",
                    websocketUrl = "wss://api.tenclass.net/xiaozhi/v1/",
                    macAddress = (1..6).joinToString(":") {
                        "%02x".format((0..255).random())
                    }
                )
            } else {
                config
            }
        )
    }
    var showValidationDialog by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    // 验证配置的函数
    fun validateAndSaveConfig(): Boolean {
        if (configManager.isConfigComplete(editedConfig)) {
            configManager.saveConfig(editedConfig)
            onConfigChange(editedConfig)
            return true
        } else {
            val missingFields = configManager.getMissingFields(editedConfig)
            validationMessage = "请填写以下必填项：\n${missingFields.joinToString("、")}"
            showValidationDialog = true
            return false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AethericColors.Background)
    ) {
        // Aurora Background Effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .alpha(0.4f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = AethericColors.AuroraGradients[0],
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(0f, 0f)
                )
                drawCircle(
                    color = AethericColors.AuroraGradients[1],
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                )
                drawCircle(
                    color = AethericColors.AuroraGradients[2],
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(size.width, size.height)
                )
                drawCircle(
                    color = AethericColors.AuroraGradients[3],
                    radius = size.width * 0.8f,
                    center = androidx.compose.ui.geometry.Offset(0f, size.height)
                )
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "小智设置",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AethericColors.OnSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = AethericColors.OnSurface
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { validateAndSaveConfig() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AethericColors.Primary
                            )
                        ) {
                            Text(
                                "保存",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Server Configuration Section
                item {
                    SettingsSection(title = "Server配置") {
                        SettingsInput(
                            label = "设备名称",
                            value = editedConfig.name,
                            onValueChange = { editedConfig = editedConfig.copy(name = it) },
                            placeholder = "测试"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsInput(
                            label = "OTA地址",
                            value = editedConfig.otaUrl,
                            onValueChange = { editedConfig = editedConfig.copy(otaUrl = it) },
                            placeholder = "e.g., https://api.aura-ai.com"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsInput(
                            label = "WSS地址",
                            value = editedConfig.websocketUrl,
                            onValueChange = { editedConfig = editedConfig.copy(websocketUrl = it) },
                            placeholder = "WSS地址"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsInput(
                            label = "MAC地址",
                            value = editedConfig.macAddress,
                            onValueChange = { editedConfig = editedConfig.copy(macAddress = it) },
                            placeholder = "MAC地址",
                            trailingIcon = {
                                IconButton(onClick = {
                                    val newMac = (1..6).joinToString(":") {
                                        "%02x".format((0..255).random())
                                    }
                                    editedConfig = editedConfig.copy(macAddress = newMac)
                                }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "刷新",
                                        tint = AethericColors.Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SettingsInput(
                            label = "Token",
                            value = editedConfig.token,
                            onValueChange = { editedConfig = editedConfig.copy(token = it) },
                            placeholder = "test-token"
                        )
                    }
                }

                // Tips Section
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Tips:",
                            color = AethericColors.OnSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "1.默认连接小智官方服务",
                            color = AethericColors.OnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            "2.设备名称和Token不影响对话",
                            color = AethericColors.OnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        Text(
                            "3.Mac地址可随机生成",
                            color = AethericColors.OnSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Validation Dialog
    if (showValidationDialog) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            title = { Text("配置验证", fontWeight = FontWeight.Bold) },
            text = { Text(validationMessage) },
            confirmButton = {
                TextButton(onClick = { showValidationDialog = false }) {
                    Text("确定", color = AethericColors.Primary)
                }
            },
            containerColor = AethericColors.Surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AethericColors.OnSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingsInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isPassword: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AethericColors.OnSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { 
                Text(
                    placeholder, 
                    color = AethericColors.OnSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.SansSerif
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AethericColors.Primary,
                unfocusedBorderColor = AethericColors.OutlineVariant.copy(alpha = 0.5f),
                focusedContainerColor = Color.White.copy(alpha = 0.9f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.5f),
                focusedTextColor = AethericColors.OnSurface,
                unfocusedTextColor = AethericColors.OnSurface
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            trailingIcon = trailingIcon,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif
            )
        )
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = AethericColors.OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                title,
                fontSize = 16.sp,
                color = AethericColors.OnSurface
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                value,
                fontSize = 14.sp,
                color = AethericColors.OnSurfaceVariant.copy(alpha = 0.6f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = AethericColors.OnSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}