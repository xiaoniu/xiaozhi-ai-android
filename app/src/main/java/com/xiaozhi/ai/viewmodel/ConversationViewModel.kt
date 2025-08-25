package com.xiaozhi.ai.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.xiaozhi.ai.audio.AudioEvent
import com.xiaozhi.ai.audio.EnhancedAudioManager
import com.xiaozhi.ai.data.ConfigManager
import com.xiaozhi.ai.data.Message
import com.xiaozhi.ai.data.MessageRole
import com.xiaozhi.ai.data.XiaozhiConfig
import com.xiaozhi.ai.network.WebSocketEvent
import com.xiaozhi.ai.network.WebSocketManager
import com.xiaozhi.ai.network.OtaService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 对话状态
 */
enum class ConversationState {
    IDLE,           // 空闲
    CONNECTING,     // 连接中
    LISTENING,      // 聆听中
    PROCESSING,     // 处理中
    SPEAKING        // 说话中
}

/**
 * 对话ViewModel
 */
class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConversationViewModel"
    }

    private val gson = Gson()
    private val webSocketManager = WebSocketManager(application)
    private val audioManager = EnhancedAudioManager(application)
    private val otaService = OtaService()

    // 状态管理
    private val _state = MutableStateFlow(ConversationState.IDLE)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // 激活弹窗状态
    private val _showActivationDialog = MutableStateFlow(false)
    val showActivationDialog: StateFlow<Boolean> = _showActivationDialog.asStateFlow()
    
    private val _activationCode = MutableStateFlow<String?>(null)
    val activationCode: StateFlow<String?> = _activationCode.asStateFlow()

    // 静音状态管理
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // 配置管理
    private val configManager = ConfigManager(application)
    private var config = configManager.loadConfig()
    
    // 多轮对话支持
    private var isAutoMode = false
    private var currentUserMessage: String? = null

    init {
        initializeServices()
    }

    /**
     * 初始化服务
     */
    @SuppressLint("MissingPermission")
    private fun initializeServices() {
        // 首先启动事件监听，确保不会错过任何事件
        startEventListening()
        
        // 初始化音频管理器
        if (!audioManager.initialize()) {
            _errorMessage.value = "音频系统初始化失败"
            return
        }

        // 执行OTA检查
        performOtaCheck()
    }
    
    /**
     * 启动事件监听
     */
    private fun startEventListening() {
        // 监听WebSocket事件 - 确保在WebSocket连接之前就开始监听
        viewModelScope.launch {
            Log.d(TAG, "开始监听WebSocket事件")
            webSocketManager.events.collect { event ->
                handleWebSocketEvent(event)
            }
        }

        // 监听音频事件
        viewModelScope.launch {
            audioManager.audioEvents.collect { event ->
                handleAudioEvent(event)
            }
        }
    }

    /**
     * 执行OTA检查
     */
    private fun performOtaCheck() {
        viewModelScope.launch {
            try {
                // 检查OTA URL是否配置
                if (config.otaUrl.isBlank()) {
                    Log.w(TAG, "OTA URL未配置，跳过OTA检查")
                    return@launch
                }
                
                Log.d(TAG, "开始执行OTA检查...")
                val result = otaService.reportDeviceAndGetOta(
                    clientId = config.uuid,
                    deviceId = config.macAddress,
                    otaUrl = config.otaUrl
                )
                
                result.onSuccess { otaResponse ->
                    Log.d(TAG, "OTA检查成功")
                    Log.d(TAG, "服务器时间: ${otaResponse.serverTime.timestamp}")
                    Log.d(TAG, "固件版本: ${otaResponse.firmware.version}")
                    Log.d(TAG, "WebSocket URL: ${otaResponse.websocket.url}")
                    
                    // 更新WebSocket URL（如果服务器返回了新的URL）
                    updateWebSocketUrl(otaResponse.websocket.url)
                    
                    // 处理激活信息（如果是首次激活）
                    otaResponse.activation?.let { activation ->
                        Log.d(TAG, "设备激活信息: ${activation.message}")
                        Log.d(TAG, "激活码: ${activation.code}")
                        // 设备未激活，显示激活弹窗
                        _activationCode.value = activation.code
                        _showActivationDialog.value = true
                        return@onSuccess // 不连接WebSocket，等待用户确认激活
                    }
                    
                    // OTA检查完成后连接WebSocket（仅在没有activation时）
                    connectToServer()
                }.onFailure { exception ->
                    Log.e(TAG, "OTA检查失败", exception)
                    _errorMessage.value = "OTA检查失败: ${exception.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "OTA检查异常", e)
                _errorMessage.value = "OTA检查异常: ${e.message}"
            }
        }
    }
    
    /**
     * 用户确认激活后连接WebSocket
     */
    fun onActivationConfirmed() {
        Log.d(TAG, "用户确认激活，开始连接WebSocket")
        _showActivationDialog.value = false
        _activationCode.value = null
        connectToServer()
    }
    
    /**
     * 关闭激活弹窗
     */
    fun dismissActivationDialog() {
        _showActivationDialog.value = false
        _activationCode.value = null
    }
    
    /**
     * 更新WebSocket URL
     */
    private fun updateWebSocketUrl(newUrl: String) {
        if (newUrl.isNotEmpty() && newUrl != config.websocketUrl) {
            Log.d(TAG, "更新WebSocket URL: $newUrl")
            // 更新配置中的WebSocket URL
            val updatedConfig = config.copy(websocketUrl = newUrl)
            updateConfig(updatedConfig)
        }
    }

    /**
     * 连接到服务器
     */
    private fun connectToServer() {
        _state.value = ConversationState.CONNECTING
        
        // 如果websocketUrl为空，先请求OTA接口获取websocketUrl
        if (config.websocketUrl.isBlank()) {
            Log.d(TAG, "WebSocket URL为空，先执行OTA检查获取URL")
            performOtaCheckForWebSocketUrl()
        } else {
            // 直接使用配置的websocketUrl连接
            Log.d(TAG, "使用配置的WebSocket URL连接: ${config.websocketUrl}")
            webSocketManager.connect(
                url = config.websocketUrl,
                deviceId = config.macAddress,
                token = config.token
            )
        }
    }
    
    /**
     * 专门用于获取WebSocket URL的OTA检查
     */
    private fun performOtaCheckForWebSocketUrl() {
        viewModelScope.launch {
            try {
                // 检查OTA URL是否配置
                if (config.otaUrl.isBlank()) {
                    Log.w(TAG, "OTA URL未配置，无法获取WebSocket URL")
                    _errorMessage.value = "OTA URL未配置，无法连接服务器"
                    _state.value = ConversationState.IDLE
                    return@launch
                }
                
                Log.d(TAG, "执行OTA检查以获取WebSocket URL...")
                val result = otaService.reportDeviceAndGetOta(
                    clientId = config.uuid,
                    deviceId = config.macAddress,
                    otaUrl = config.otaUrl
                )
                
                result.onSuccess { otaResponse ->
                    Log.d(TAG, "获取WebSocket URL成功: ${otaResponse.websocket.url}")
                    
                    // 更新WebSocket URL
                    updateWebSocketUrl(otaResponse.websocket.url)
                    
                    // 处理激活信息（如果是首次激活）
                    otaResponse.activation?.let { activation ->
                        Log.d(TAG, "设备激活信息: ${activation.message}")
                        Log.d(TAG, "激活码: ${activation.code}")
                        // 设备未激活，显示激活弹窗
                        _activationCode.value = activation.code
                        _showActivationDialog.value = true
                        return@onSuccess // 不连接WebSocket，等待用户确认激活
                    }
                    
                    // 使用获取到的WebSocket URL连接（仅在没有activation时）
                    webSocketManager.connect(
                        url = otaResponse.websocket.url,
                        deviceId = config.macAddress,
                        token = config.token
                    )
                }.onFailure { exception ->
                    Log.e(TAG, "获取WebSocket URL失败", exception)
                    _errorMessage.value = "获取WebSocket URL失败: ${exception.message}"
                    _state.value = ConversationState.IDLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取WebSocket URL异常", e)
                _errorMessage.value = "获取WebSocket URL异常: ${e.message}"
                _state.value = ConversationState.IDLE
            }
        }
    }
    
    /**
     * 更新配置并重连
     */
    fun updateConfig(newConfig: XiaozhiConfig) {
        val oldConfig = config
        config = newConfig
        configManager.saveConfig(newConfig)
        Log.d(TAG, "配置已更新")
        
        // 如果WebSocket相关配置发生变化，需要重连
        if (oldConfig.websocketUrl != newConfig.websocketUrl || 
            oldConfig.macAddress != newConfig.macAddress ||
            oldConfig.token != newConfig.token) {
            Log.d(TAG, "WebSocket配置发生变化，执行重连")
            reconnect()
        }
    }

    /**
     * 处理WebSocket事件
     */
    private fun handleWebSocketEvent(event: WebSocketEvent) {
        Log.d(TAG, "收到WebSocket事件: ${event::class.simpleName}")
        when (event) {
            is WebSocketEvent.HelloReceived -> {
                Log.d(TAG, "握手完成")
            }
            
            is WebSocketEvent.Connected -> {
                Log.d(TAG, "WebSocket连接成功")
                _isConnected.value = true
                _state.value = ConversationState.IDLE
                _errorMessage.value = null
            }

            is WebSocketEvent.Disconnected -> {
                Log.d(TAG, "WebSocket连接断开")
                _isConnected.value = false
                _state.value = ConversationState.IDLE
                audioManager.stopRecording()
                audioManager.stopPlaying()
            }

            is WebSocketEvent.TextMessage -> {
                handleTextMessage(event.message)
            }

            is WebSocketEvent.BinaryMessage -> {
                handleBinaryMessage(event.data)
            }
            
            is WebSocketEvent.MCPMessage -> {
                handleMCPMessage(event.message)
            }

            is WebSocketEvent.Error -> {
                Log.e(TAG, "WebSocket错误: ${event.error}")
                _errorMessage.value = event.error
                _state.value = ConversationState.IDLE
                audioManager.stopRecording()
                audioManager.stopPlaying()
            }
        }
    }

    /**
     * 处理文本消息
     */
    private fun handleTextMessage(message: String) {
        try {
            val json = gson.fromJson(message, JsonObject::class.java)
            val type = json.get("type")?.asString
            val sessionId = json.get("session_id")?.asString

            Log.d(TAG, "处理消息类型: $type, session_id: $sessionId")

            when (type) {
                "stt" -> {
                    // 语音转文本结果
                    val text = json.get("text")?.asString
                    if (!text.isNullOrEmpty() && !text.contains("请登录控制面板")) {
                        currentUserMessage = text
                        addMessage(Message(
                            role = MessageRole.USER,
                            content = text
                        ))
                        // STT结果表示用户说话结束，停止录音
                        audioManager.stopRecording()
                        _state.value = ConversationState.PROCESSING
                    }
                }
                
                "llm" -> {
                    // 大语言模型表情结果
                    val emotion = json.get("emotion")?.asString
                    val text = json.get("text")?.asString
                    Log.d(TAG, "收到表情: $emotion, 文本: $text")
                    // 可以在这里处理表情显示
                }
                
                "tts" -> {
                    val state = json.get("state")?.asString
                    when (state) {
                        "sentence_start" -> {
                            // TTS句子开始，显示要播放的文本
                            val text = json.get("text")?.asString
                            if (!text.isNullOrEmpty()) {
                                addMessage(Message(
                                    role = MessageRole.ASSISTANT,
                                    content = text
                                ))
                            }
                        }
                        "start" -> {
                            // TTS开始播放
                            _state.value = ConversationState.SPEAKING
                            Log.d(TAG, "开始TTS播放")
                        }
                        "stop" -> {
                            // TTS播放结束
                            audioManager.stopPlaying()
                            Log.d(TAG, "TTS播放结束")
                            
                            // 根据模式决定下一步
                            if (isAutoMode) {
                                // 自动模式：继续下一轮对话
                                startNextRound()
                            } else {
                                // 手动模式：回到空闲状态
                                _state.value = ConversationState.IDLE
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析文本消息失败", e)
        }
    }

    /**
     * 处理二进制消息（音频数据）
     */
    private fun handleBinaryMessage(data: ByteArray) {
        
        Log.d(TAG, "收到二进制消息，长度: ${data.size}")
        // 只有在非静音状态下才播放音频数据
        if (!_isMuted.value) {
            audioManager.playAudio(data)
        } else {
            Log.d(TAG, "静音模式，跳过音频播放")
        }
    }

    /**
     * 处理音频事件
     */
    private fun handleAudioEvent(event: AudioEvent) {
        when (event) {
            is AudioEvent.AudioData -> {
                // 只有在聆听状态才发送音频数据
                if (_state.value == ConversationState.LISTENING) {
                    webSocketManager.sendBinaryMessage(event.data)
                }
            }
            is AudioEvent.Error -> {
                Log.e(TAG, "音频错误: ${event.message}")
                _errorMessage.value = event.message
                stopListening()
            }
        }
    }

    /**
     * 开始聆听（手动模式）
     */
    fun startListening() {
        if (_state.value != ConversationState.IDLE || !_isConnected.value) {
            return
        }

        isAutoMode = false
        _state.value = ConversationState.LISTENING
        audioManager.startRecording()
        
        // 发送开始聆听消息
        webSocketManager.sendStartListening("manual")
        Log.d(TAG, "开始手动聆听")
    }

    /**
     * 开始自动对话模式
     */
    fun startAutoConversation() {
        if (_state.value != ConversationState.IDLE || !_isConnected.value) {
            return
        }

        isAutoMode = true
        _state.value = ConversationState.LISTENING
        audioManager.startRecording()
        
        // 发送开始聆听消息
        webSocketManager.sendStartListening("auto")
        Log.d(TAG, "开始自动对话模式")
    }

    /**
     * 停止聆听
     */
    fun stopListening() {
        if (_state.value != ConversationState.LISTENING) {
            return
        }

        audioManager.stopRecording()
        _state.value = ConversationState.PROCESSING
        
        // 发送停止聆听消息
        webSocketManager.sendStopListening()
        Log.d(TAG, "停止聆听")
    }

    /**
     * 取消当前录音并发送中止信号（可选原因）
     * 用于上滑取消等场景：立即停止录音、停止Opus数据传输，并发送 type=abort 给服务器。
     */
    fun cancelListeningWithAbort(reason: String = "user_interrupt") {
        // 将状态置为IDLE，确保 handleAudioEvent 不再发送后续音频帧
        if (_state.value == ConversationState.LISTENING) {
            _state.value = ConversationState.IDLE
        }
        // 停止录音，确保底层不再采集与编码音频
        audioManager.stopRecording()

        // 发送中止信号到服务器，包含 session_id 与原因
        webSocketManager.sendAbort(reason)

        Log.d(TAG, "取消录音并发送中止: $reason")
    }

    /**
     * 开始下一轮对话（自动模式）
     */
    private fun startNextRound() {
        if (!isAutoMode || !_isConnected.value) {
            _state.value = ConversationState.IDLE
            return
        }

        _state.value = ConversationState.LISTENING
        audioManager.startRecording()
        
        // 发送开始聆听消息
        webSocketManager.sendStartListening("auto")
        Log.d(TAG, "开始下一轮自动对话")
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(text: String) {
        if (!_isConnected.value || text.isBlank()) {
            return
        }
        // 发送唤醒词检测消息
        webSocketManager.sendTextRequest(text)
        _state.value = ConversationState.PROCESSING
        Log.d(TAG, "发送文本消息: $text")
    }

    /**
     * 发送初始化消息（设备激活时使用，不添加到对话列表）
     */
    private fun sendInitializationMessage() {
        // 发送"初始化"文本消息，但不添加到对话列表
        webSocketManager.sendTextRequest("初始化")
        Log.d(TAG, "发送设备初始化消息")
    }

    /**
     * 打断当前对话
     */
    fun interrupt() {
        audioManager.stopPlaying()
        audioManager.stopRecording()
        
        // 发送中断消息
        webSocketManager.sendAbort("user_interrupt")
        
        // 退出自动模式
        isAutoMode = false
        _state.value = ConversationState.IDLE
        Log.d(TAG, "用户打断对话")
    }

    /**
     * 停止自动对话模式
     */
    fun stopAutoConversation() {
        isAutoMode = false
        audioManager.stopRecording()
        audioManager.stopPlaying()
        
        // 发送中断消息
        webSocketManager.sendAbort("stop_auto_mode")
        
        _state.value = ConversationState.IDLE
        Log.d(TAG, "停止自动对话模式")
    }

    /**
     * 添加消息到列表
     */
    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 清除对话历史
     */
    fun clearMessages() {
        _messages.value = emptyList()
    }

    /**
     * 重新连接
     */
    fun reconnect() {
        webSocketManager.disconnect()
        connectToServer()
    }

    /**
     * 测试音频播放
     */
    fun testAudioPlayback() {
        audioManager.testAudioPlayback()
    }

    /**
     * 切换静音状态
     */
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        Log.d(TAG, "静音状态切换为: ${_isMuted.value}")
        
        // 如果切换到静音状态，停止当前播放
        if (_isMuted.value) {
            audioManager.stopPlaying()
        }
    }

    /**
     * 处理MCP消息
     */
    private fun handleMCPMessage(message: String) {
        Log.d(TAG, "收到MCP消息: $message")
        
        // 添加MCP消息到对话列表（可选）
        addMessage(Message(
            role = MessageRole.SYSTEM,
            content = "MCP: $message"
        ))
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.cleanup()
        webSocketManager.cleanup()
    }
}