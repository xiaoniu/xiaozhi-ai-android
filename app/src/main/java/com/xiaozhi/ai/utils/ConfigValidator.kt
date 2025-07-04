package com.xiaozhi.ai.utils

import android.content.Context
import com.xiaozhi.ai.data.ConfigManager

/**
 * 配置验证工具类
 */
class ConfigValidator(private val context: Context) {
    private val configManager = ConfigManager(context)
    
    /**
     * 检查是否需要跳转到设置页面
     * @return true 如果需要跳转到设置页面，false 如果配置完整可以继续
     */
    fun shouldNavigateToSettings(): Boolean {
        val config = configManager.loadConfig()
        return config.otaUrl.isBlank()
    }
    
    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(): Boolean {
        val config = configManager.loadConfig()
        return configManager.isConfigComplete(config)
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig() = configManager.loadConfig()
}