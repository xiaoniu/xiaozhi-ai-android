package com.xiaozhi.ai.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

/**
 * WebSocket事件类型
 */
sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    data class TextMessage(val message: String) : WebSocketEvent()
    data class BinaryMessage(val data: ByteArray) : WebSocketEvent()
    data class Error(val error: String) : WebSocketEvent()
    object HelloReceived : WebSocketEvent()
    data class MCPMessage(val message: String) : WebSocketEvent()
}

/**
 * WebSocket管理器
 */
class WebSocketManager(private val context: Context) {

    companion object {
        private const val TAG = "WebSocketManager"
        private const val RECONNECT_DELAY = 2000L // 增加重连延迟
        private const val HELLO_TIMEOUT = 15000L // 延长握手超时到15秒
        private const val CONNECT_TIMEOUT = 15L // 连接超时15秒
        private const val WRITE_TIMEOUT = 15L // 写入超时15秒
    }

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var isHandshakeComplete = false
    private var shouldReconnect = true
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 会话管理
    private var sessionId: String? = null
    private var helloTimeoutJob: Job? = null

    // 连接参数，用于自动重连
    private var lastUrl: String? = null
    private var lastDeviceId: String? = null
    private var lastToken: String? = null

    private val _events = MutableSharedFlow<WebSocketEvent>(replay = 1)
    val events: SharedFlow<WebSocketEvent> = _events

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // 保持无限读取超时
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    /**
     * 连接WebSocket
     */
    fun connect(url: String, deviceId: String, token: String) {
        Log.d(TAG, "正在连接WebSocket: $url")

        // 保存连接参数用于自动重连
        lastUrl = url
        lastDeviceId = deviceId
        lastToken = token

        // 重置状态
        isHandshakeComplete = false
        sessionId = null

        val request = Request.Builder()
            .url(url)
            .addHeader("Device-Id", deviceId)
            .addHeader("Client-Id", deviceId)
            .addHeader("Protocol-Version", "1")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket连接成功，开始握手")
                isConnected = true
//                scope.launch {
//                    // 发送Hello消息
//                    sendHelloMessage()
//                    // 启动超时检查
//                    startHelloTimeout()
//                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "收到文本消息: $text")
                scope.launch {
                    handleTextMessage(text)
                    _events.emit(WebSocketEvent.TextMessage(text))
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "收到二进制消息，长度: ${bytes.size}")
                scope.launch {
                    _events.emit(WebSocketEvent.BinaryMessage(bytes.toByteArray()))
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket正在关闭: $code - $reason")
                reconnectAfterDisconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket已关闭: $code - $reason")
                reconnectAfterDisconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket连接失败", t)
                scope.launch {
                    _events.emit(WebSocketEvent.Error("连接失败"))
                }
                reconnectAfterDisconnect()
            }
        })
    }

    private fun reconnectAfterDisconnect() {
        Log.d(TAG, "正在尝试自动重连...")
        isConnected = false
        isHandshakeComplete = false
        sessionId = null
        helloTimeoutJob?.cancel()
        scope.launch {
            if (shouldReconnect && lastUrl != null && lastDeviceId != null && lastToken != null) {
                Log.d(TAG, "连接失败，准备自动重连...")
                delay(RECONNECT_DELAY)
                connect(lastUrl!!, lastDeviceId!!, lastToken!!)
            }
        }
    }

    /**
     * 处理文本消息
     */
    private fun handleTextMessage(text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val type = json.get("type")?.asString

            when (type) {
                "hello" -> {
                    handleHelloResponse(json)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析文本消息失败", e)
        }
    }

    /**
     * 处理Hello响应
     */
    private fun handleHelloResponse(json: JsonObject) {
        val transport = json.get("transport")?.asString
        if (transport == "websocket") {
            // 提取session_id
            sessionId = json.get("session_id")?.asString
            isHandshakeComplete = true
            helloTimeoutJob?.cancel()

            Log.d(TAG, "握手完成1，session_id: $sessionId")
            scope.launch {
                Log.d(TAG, "握手完成2，session_id: $sessionId")
                Log.d(TAG, "发送HelloReceived事件")
                _events.emit(WebSocketEvent.HelloReceived)
                Log.d(TAG, "发送Connected事件")
                _events.emit(WebSocketEvent.Connected)
                Log.d(TAG, "事件发送完成")
            }
        } else {
            Log.e(TAG, "服务器返回的transport不匹配: $transport")
            scope.launch {
                _events.emit(WebSocketEvent.Error("握手失败：transport不匹配"))
            }
        }
    }

    /**
     * 启动Hello超时检查
     */
    private fun startHelloTimeout() {
        helloTimeoutJob = scope.launch {
            delay(HELLO_TIMEOUT)
            if (!isHandshakeComplete) {
                Log.e(TAG, "Hello握手超时")
                _events.emit(WebSocketEvent.Error("握手超时"))
                disconnect()
            }
        }
    }

    /**
     * 发送Hello消息
     */
    private fun sendHelloMessage() {
        val hello = JsonObject().apply {
            addProperty("type", "hello")
            addProperty("version", 1)
            addProperty("transport", "websocket")
            add("audio_params", JsonObject().apply {
                addProperty("format", "opus")
                addProperty("sample_rate", 16000)
                addProperty("channels", 1)
                addProperty("frame_duration", 60)
            })
        }
        sendTextMessage(gson.toJson(hello))
    }

    /**
     * 发送开始监听消息
     */
    fun sendStartListening(mode: String = "auto") {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "listen")
            addProperty("state", "start")
            addProperty("mode", mode)
        }
        sendTextMessage(gson.toJson(message))
    }

    /**
     * 发送停止监听消息
     */
    fun sendStopListening() {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "listen")
            addProperty("state", "stop")
        }
        sendTextMessage(gson.toJson(message))
    }

    /**
     * 发送唤醒词检测消息
     */
    fun sendWakeWordDetected(text: String) {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "listen")
            addProperty("state", "detect")
            addProperty("text", text)
            addProperty("source", "text")
        }
        sendTextMessage(gson.toJson(message))
    }

    /**
     * 发送中断消息
     */
    fun sendAbort(reason: String = "user_interrupt") {
        val message = JsonObject().apply {
            sessionId?.let { addProperty("session_id", it) }
            addProperty("type", "abort")
            addProperty("reason", reason)
        }
        sendTextMessage(gson.toJson(message))
    }

    /**
     * 发送文本消息
     */
    fun sendTextMessage(message: String) {
        if (isConnected && webSocket != null) {
            try {
                val success = webSocket!!.send(message)
                if (success) {
                    Log.d(TAG, "发送文本消息: $message")
                } else {
                    Log.w(TAG, "发送文本消息失败，WebSocket可能已关闭")
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送文本消息异常", e)
            }
        } else {
            Log.w(TAG, "WebSocket未连接，无法发送消息")
        }
    }

    /**
     * 发送二进制消息
     */
    fun sendBinaryMessage(data: ByteArray) {
        if (isConnected && isHandshakeComplete && webSocket != null) {
            try {
                val success = webSocket!!.send(ByteString.of(*data))
                if (success) {
                    Log.d(TAG, "发送二进制消息，长度: ${data.size}")
                } else {
                    Log.w(TAG, "发送二进制消息失败，WebSocket可能已关闭")
                }
            } catch (e: Exception) {
                Log.e(TAG, "发送二进制消息异常", e)
            }
        } else {
            Log.w(TAG, "WebSocket未就绪，无法发送二进制消息")
        }
    }

    /**
     * 发送文本请求（兼容旧接口）
     */
    fun sendTextRequest(text: String) {
        sendWakeWordDetected(text)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        shouldReconnect = false
        helloTimeoutJob?.cancel()
        webSocket?.close(1000, "正常关闭")
        webSocket = null
        isConnected = false
        isHandshakeComplete = false
        sessionId = null
        // 清理连接参数
        lastUrl = null
        lastDeviceId = null
        lastToken = null
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = isConnected && isHandshakeComplete

    /**
     * 获取会话ID
     */
    fun getSessionId(): String? = sessionId

    /**
     * 重新启用自动重连
     */
    fun enableReconnect() {
        shouldReconnect = true
    }

    /**
     * 禁用自动重连
     */
    fun disableReconnect() {
        shouldReconnect = false
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
}