package com.xiaozhi.ai

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xiaozhi.ai.data.ConfigManager
import com.xiaozhi.ai.ui.ConversationScreen
import com.xiaozhi.ai.ui.SettingsScreen
import com.xiaozhi.ai.ui.theme.DarkColorScheme
import com.xiaozhi.ai.ui.theme.YTheme
import com.xiaozhi.ai.viewmodel.ConversationViewModel

sealed class Screen(val route: String) {
    object Conversation : Screen("conversation")
    object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d(TAG, "应用启动，开始初始化...")
        
        setContent {
            YTheme(
                darkTheme = true
            ) {
                val context = LocalContext.current
                val configManager = remember { ConfigManager(context) }
                val navController = rememberNavController()
                
                // 获取当前配置
                val currentConfig = configManager.loadConfig()
                
                // 决定初始页面：如果未配置 OTA 或 WSS，跳转到设置页面
                val startDestination = if (currentConfig.otaUrl.isBlank() && currentConfig.websocketUrl.isBlank()) {
                    Screen.Settings.route
                } else {
                    Screen.Conversation.route
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkColorScheme.background
                ) {
                    val conversationViewModel: ConversationViewModel = viewModel()
                    
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(Screen.Conversation.route) {
                            ConversationScreen(
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                },
                                viewModel = conversationViewModel
                            )
                        }
                        
                        composable(Screen.Settings.route) {
                            var editedConfig by remember { mutableStateOf(configManager.loadConfig()) }
                            SettingsScreen(
                                config = editedConfig,
                                onConfigChange = { newConfig ->
                                    configManager.saveConfig(newConfig)
                                    editedConfig = newConfig
                                    conversationViewModel.updateConfig(newConfig)
                                    
                                    // 如果配置已完成，返回或跳转到对话页面
                                    if (newConfig.otaUrl.isNotBlank() || newConfig.websocketUrl.isNotBlank()) {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        } else {
                                            navController.navigate(Screen.Conversation.route) {
                                                popUpTo(Screen.Settings.route) { inclusive = true }
                                            }
                                        }
                                    }
                                },
                                onBack = {
                                    val config = configManager.loadConfig()
                                    if (config.otaUrl.isNotBlank() || config.websocketUrl.isNotBlank()) {
                                        if (navController.previousBackStackEntry != null) {
                                            navController.popBackStack()
                                        } else {
                                            navController.navigate(Screen.Conversation.route) {
                                                popUpTo(Screen.Settings.route) { inclusive = true }
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}