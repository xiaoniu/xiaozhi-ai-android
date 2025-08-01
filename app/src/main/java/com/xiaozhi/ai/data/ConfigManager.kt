package com.xiaozhi.ai.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 配置管理器，负责配置的存储和读取
 */
class ConfigManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "xiaozhi_config"
        private const val KEY_CONFIG = "config"
    }
    
    /**
     * 保存配置
     */
    fun saveConfig(config: XiaozhiConfig) {
        val configJson = gson.toJson(config)
        sharedPreferences.edit()
            .putString(KEY_CONFIG, configJson)
            .apply()
    }
    
    /**
     * 读取配置
     */
    fun loadConfig(): XiaozhiConfig {
        val configJson = sharedPreferences.getString(KEY_CONFIG, null)
        return if (configJson != null) {
            try {
                gson.fromJson(configJson, XiaozhiConfig::class.java)
            } catch (e: Exception) {
                XiaozhiConfig.createDefault()
            }
        } else {
            XiaozhiConfig.createDefault()
        }
    }
    
    /**
     * 检查配置是否完整
     */
    fun isConfigComplete(config: XiaozhiConfig): Boolean {
        return config.name.isNotBlank() &&
               (config.otaUrl.isNotBlank() || config.websocketUrl.isNotBlank()) &&
               config.macAddress.isNotBlank() &&
               config.token.isNotBlank()
    }
    
    /**
     * 获取缺失的配置项
     */
    fun getMissingFields(config: XiaozhiConfig): List<String> {
        val missingFields = mutableListOf<String>()
        
        if (config.name.isBlank()) missingFields.add("设备名称")
        if (config.otaUrl.isBlank() && config.websocketUrl.isBlank()) missingFields.add("OTA地址或WSS地址(至少填一个)")
        if (config.macAddress.isBlank()) missingFields.add("MAC地址")
        if (config.token.isBlank()) missingFields.add("Token")
        
        return missingFields
    }
}