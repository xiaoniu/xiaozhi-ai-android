# 小智AI Android客户端

一个基于小智的Android智能语音对话应用，支持实时语音交互、文本对话和多轮对话功能。

## 📱 功能特性

- **实时语音对话**: 支持语音录制、实时传输和TTS播放
- **文本交互**: 支持文本输入和显示
- **多轮对话**: 支持自动和手动两种对话模式
- **音频处理**: 集成Opus编解码、回声消除和降噪
- **WebSocket通信**: 基于WebSocket的实时双向通信
- **状态管理**: 完整的对话状态流转和错误处理

## 🛠️ 技术栈

### 核心框架
- **Kotlin**
- **Jetpack Compose**

### 网络通信
- **OkHttp**
- **Gson**

### 音频处理
- **Opus编解码**: 1.3.1 - 高质量音频压缩
- **Native C++**: CMake + NDK音频处理
- **AudioRecord/AudioTrack**: Android原生音频API
- **回声消除**: AcousticEchoCanceler
- **降噪处理**: NoiseSuppressor

### 异步处理
- **Kotlin Coroutines**
- **Flow**
- **ViewModel**

### 导航
- **Navigation Compose**: 2.7.6 - 声明式导航

## 🏗️ 项目架构

```
app/src/main/java/com/xiaozhi/ai/
├── audio/                    # 音频处理模块
│   ├── EnhancedAudioManager.kt    # 增强音频管理器
│   ├── OpusCodec.kt              # Opus编解码器
│   └── utils/                    # 音频工具类
├── network/                  # 网络通信模块
│   └── WebSocketManager.kt      # WebSocket管理器
├── ui/                      # UI界面模块
│   ├── ConversationScreen.kt    # 对话界面
│   └── theme/               # 主题配置
├── viewmodel/               # 视图模型
│   └── ConversationViewModel.kt  # 对话视图模型
└── MainActivity.kt          # 主活动
```

### Native模块
```
app/src/main/cpp/
├── opus_encoder.cpp         # Opus编码器JNI
├── opus_decoder.cpp         # Opus解码器JNI
└── CMakeLists.txt          # CMake构建配置
```

## 📋 系统要求

- **Android版本**: Android 10 (API 29) 及以上
- **权限要求**:
  - `RECORD_AUDIO` - 录音权限
  - `INTERNET` - 网络访问
  - `ACCESS_NETWORK_STATE` - 网络状态
  - `MODIFY_AUDIO_SETTINGS` - 音频设置

## 📸 应用截图

### 应用界面展示

![对话](./screenshot/v1.0/Screenshot_20250704_174534.png)

![设置](./screenshot/v1.0/Screenshot_20250704_174622.png)

## 📚 文档

- [API通信协议](./doc/api.md) - WebSocket通信协议详细说明
- [对话流程](./doc/flow.md) - 对话流程实现和状态管理
- [MCP协议](./doc/mcp.md) - MCP工具调用协议

## 🤝 贡献

欢迎提交Issue和Pull Request来改进项目！

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🔗 相关链接

- [Opus音频编解码](https://opus-codec.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [OkHttp WebSocket](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/)