### 修订后的WebSocket对话流程实现

基于 `flow.md` 文档，我们已经重新修订了Android客户端的对话流程实现。以下是主要的改进和实现细节：

## 1. 建立连接流程的改进

### 1.1 完整的握手机制
- **WebSocketManager** 现在实现了完整的握手流程
- 连接建立后发送 `hello` 消息
- 等待服务器返回 `hello` 响应并验证 `transport: "websocket"`
- 提取并保存 `session_id` 用于后续通信
- 实现10秒握手超时机制

```kotlin
// 握手流程
private fun handleHelloResponse(json: JsonObject) {
    val transport = json.get("transport")?.asString
    if (transport == "websocket") {
        sessionId = json.get("session_id")?.asString
        isHandshakeComplete = true
        // 握手完成，触发Connected事件
    }
}
```

### 1.2 请求头规范化
按照文档要求，请求头使用正确的格式：
- `Device-Id`: 设备MAC地址
- `Client-Id`: 设备UUID
- `Protocol-Version`: "1"
- `Authorization`: "Bearer <token>"

## 2. 文本交互流程的实现

### 2.1 文本输入处理
```kotlin
fun sendTextMessage(text: String) {
    // 发送唤醒词检测消息
    webSocketManager.sendWakeWordDetected(text)
    _state.value = ConversationState.PROCESSING
}

// 对应的消息格式
{
  "session_id": "ce6867b9",
  "type": "listen",
  "state": "detect", 
  "text": "你好",
  "source": "text"
}
```

### 2.2 服务器响应处理
完整实现了所有服务器响应类型的处理：

- **STT响应**: 显示语音识别结果
- **LLM响应**: 处理表情和情感信息
- **TTS响应**:
   - `sentence_start`: 显示要播放的文本
   - `start`: 进入Speaking状态
   - `stop`: 根据模式决定下一步动作

## 3. 双向语音多轮对话流程

### 3.1 手动对话模式
```kotlin
fun startListening() {
    isAutoMode = false
    _state.value = ConversationState.LISTENING
    audioManager.startRecording()
    webSocketManager.sendStartListening("manual")
}
```

对应消息：
```json
{
  "session_id": "123",
  "type": "listen", 
  "state": "start",
  "mode": "manual"
}
```

### 3.2 自动对话模式
```kotlin
fun startAutoConversation() {
    isAutoMode = true
    _state.value = ConversationState.LISTENING
    audioManager.startRecording()
    webSocketManager.sendStartListening("auto")
}

private fun startNextRound() {
    if (isAutoMode) {
        // TTS结束后自动开始下一轮
        _state.value = ConversationState.LISTENING
        audioManager.startRecording()
        webSocketManager.sendStartListening("auto")
    }
}
```

### 3.3 状态流转
实现了完整的状态流转：

1. **IDLE → LISTENING**: 用户点击开始聆听或自动模式
2. **LISTENING → PROCESSING**: 收到STT结果，停止录音
3. **PROCESSING → SPEAKING**: 收到TTS start消息
4. **SPEAKING → IDLE/LISTENING**:
   - 手动模式：回到IDLE
   - 自动模式：开始下一轮LISTENING

## 4. 音频处理改进

### 4.1 音频数据发送控制
```kotlin
private fun handleAudioEvent(event: AudioEvent) {
    when (event) {
        is AudioEvent.AudioData -> {
            // 只有在聆听状态才发送音频数据
            if (_state.value == ConversationState.LISTENING) {
                webSocketManager.sendBinaryMessage(event.data)
            }
        }
    }
}
```

### 4.2 二进制消息去重
实现了音频数据去重机制，防止重复播放：
```kotlin
private val processedBinaryHashes = mutableSetOf<Int>()

private fun handleBinaryMessage(data: ByteArray) {
    val dataHash = data.contentHashCode()
    if (!processedBinaryHashes.contains(dataHash)) {
        processedBinaryHashes.add(dataHash)
        audioManager.playAudio(data)
    }
}
```

## 5. 中断和错误处理

### 5.1 用户中断
```kotlin
fun interrupt() {
    audioManager.stopPlaying()
    audioManager.stopRecording()
    webSocketManager.sendAbort("user_interrupt")
    isAutoMode = false
    _state.value = ConversationState.IDLE
}
```

对应消息：
```json
{
  "session_id": "xxx",
  "type": "abort",
  "reason": "user_interrupt"
}
```

### 5.2 连接错误处理
- 握手超时检测
- 自动重连机制
- 连接断开时清理音频状态

## 6. UI界面改进

### 6.1 新增功能按钮
- **自动对话**: 启动多轮自动对话模式
- **停止自动**: 退出自动对话模式
- **打断**: 中断当前TTS播放

### 6.2 状态指示
- 实时显示连接状态
- 清晰的对话状态指示（聆听中、处理中、回复中）
- 权限状态提示

## 7. 会话管理

### 7.1 Session ID管理
- 从服务器hello响应中提取session_id
- 所有后续消息都包含session_id
- 支持多会话管理

### 7.2 消息格式标准化
所有消息都遵循文档规范：
```kotlin
fun sendStartListening(mode: String = "auto") {
    val message = JsonObject().apply {
        sessionId?.let { addProperty("session_id", it) }
        addProperty("type", "listen")
        addProperty("state", "start") 
        addProperty("mode", mode)
    }
    sendTextMessage(gson.toJson(message))
}
```

## 8. 主要改进总结

1. **完整握手流程**: 实现了标准的WebSocket握手和超时处理
2. **会话管理**: 支持session_id管理和多会话
3. **多轮对话**: 支持自动和手动两种对话模式
4. **状态管理**: 清晰的状态流转和错误处理
5. **音频优化**: 改进的音频数据处理和去重机制
6. **UI增强**: 更丰富的用户交互界面
7. **消息规范**: 严格按照API文档实现消息格式

这些改进使得Android客户端完全符合 `flow.md` 文档中定义的通信流程，提供了更稳定和功能完整的对话体验。 