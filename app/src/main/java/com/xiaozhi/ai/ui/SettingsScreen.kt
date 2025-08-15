package com.xiaozhi.ai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaozhi.ai.data.ConfigManager
import com.xiaozhi.ai.data.XiaozhiConfig
import com.xiaozhi.ai.ui.theme.DarkColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    config: XiaozhiConfig,
    onConfigChange: (XiaozhiConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { ConfigManager(context) }
    var editedConfig by remember { mutableStateOf(config) }
    var showAddMcpDialog by remember { mutableStateOf(false) }
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

    // 验证配置的函数（用于返回按钮）
    fun validateConfigForBack(): Boolean {
        if (configManager.isConfigComplete(editedConfig)) {
            return true
        } else {
            val missingFields = configManager.getMissingFields(editedConfig)
            validationMessage = "配置未完成，请填写以下必填项：\n${missingFields.joinToString("、")}"
            showValidationDialog = true
            return false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkColorScheme.background,
        topBar = {
            // 顶部标题栏 - 优化设计
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkColorScheme.primary,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                                onBack()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = DarkColorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "小智设置",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkColorScheme.onPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { validateAndSaveConfig() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkColorScheme.onPrimary.copy(alpha = 0.1f),
                            contentColor = DarkColorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "保存",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkColorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Server配置",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = editedConfig.name,
                        onValueChange = { editedConfig = editedConfig.copy(name = it) },
                        label = { Text("设备名称", color = Color.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkColorScheme.primary,
                            focusedLabelColor = DarkColorScheme.primary,
                            focusedLeadingIconColor = DarkColorScheme.primary,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editedConfig.otaUrl,
                        onValueChange = {
                            editedConfig = editedConfig.copy(otaUrl = it)
                        },
                        label = { Text("OTA地址", color = Color.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkColorScheme.primary,
                            focusedLabelColor = DarkColorScheme.primary,
                            focusedLeadingIconColor = DarkColorScheme.primary,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editedConfig.websocketUrl,
                        onValueChange = {
                            editedConfig = editedConfig.copy(websocketUrl = it)
                        },
                        label = { Text("WSS地址", color = Color.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkColorScheme.primary,
                            focusedLabelColor = DarkColorScheme.primary,
                            focusedLeadingIconColor = DarkColorScheme.primary,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editedConfig.macAddress,
                        onValueChange = {
                            editedConfig = editedConfig.copy(macAddress = it)
                        },
                        label = { Text("MAC地址", color = Color.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkColorScheme.primary,
                            focusedLabelColor = DarkColorScheme.primary,
                            focusedLeadingIconColor = DarkColorScheme.primary,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    // 生成新的随机MAC地址
                                    val newMacAddress = (1..6).joinToString(":") {
                                        "%02x".format((0..255).random())
                                    }
                                    editedConfig = editedConfig.copy(
                                        macAddress = newMacAddress
                                    )
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "重新生成",
                                    tint = DarkColorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editedConfig.token,
                        onValueChange = { editedConfig = editedConfig.copy(token = it) },
                        label = { Text("Token", color = Color.Black) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkColorScheme.primary,
                            focusedLabelColor = DarkColorScheme.primary,
                            focusedLeadingIconColor = DarkColorScheme.primary,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(6.dp),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Tips:\n1.设备名称和Token不影响对话\n2.Mac地址可随机生成\n3.OTA地址和WSS地址至少填一个",
                color = Color.Gray,
                fontSize = 12.sp
            )

        }
    }

    // 验证提示对话框
    if (showValidationDialog) {
        AlertDialog(
            onDismissRequest = { showValidationDialog = false },
            title = {
                Text(
                    text = "配置验证",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            text = {
                Text(
                    text = validationMessage,
                    color = Color.Black
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showValidationDialog = false }
                ) {
                    Text(
                        "确定",
                        color = DarkColorScheme.primary
                    )
                }
            },
            containerColor = DarkColorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        )
    }
}