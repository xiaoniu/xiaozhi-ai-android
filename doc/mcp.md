# MCP流程
```mermaid
sequenceDiagram
    participant Device as 设备端
    participant Server as 服务器

    Note over Device: 1. 设备启动与初始化
    Device->>Server: 连接Wi-Fi (GUOHONGN)
    Device->>Server: WebSocket握手 (ws://192.168.1.207:8000)
    
    Note over Device: 2. 注册MCP工具
    loop 静态注册工具
        Device->>Device: Add tool: self.get_device_status
        Device->>Device: Add tool: self.audio_speaker.set_volume
        Device->>Device: Add tool: self.music.play_song
    end

    Note over Server: 3. 服务器初始化MCP
    Server->>Device: MCP初始化请求 (initialize)
    Device-->>Server: 响应协议版本和能力 (protocolVersion: 2024-11-05)

    Note over Server: 4. 查询工具列表
    Server->>Device: 请求工具列表 (tools/list)
    Device-->>Server: 返回工具详情 (含description/inputSchema)

    Note over Server: 5. 动态工具调用示例
    Server->>Device: 调用播放工具 (tools/call: play_song, song_name="曹操")
    alt 资源获取成功
        Device->>Device: 播放音乐
        Device-->>Server: 返回成功结果
    else 资源获取失败
        Device-->>Server: 返回错误 ("获取音乐资源失败")
    end

    Note over Device: 6. 状态同步
    Device->>Server: 状态变更 (listening → speaking)
```
# MCP数据
## 开启MCP
### Client发送
```json
{"type":"hello","version":1,"features":{"mcp":true},"transport":"websocket","audio_params":{"format":"opus","sample_rate":16000,"channels":1,"frame_duration":60}}
```
### Server响应
```json
 {"type": "hello", "version": 1, "transport": "websocket", "audio_params": {"format": "opus", "sample_rate": 16000, "channels": 1, "frame_duration": 60}, "session_id": "0d051203-c9be-4ae0-8911-dffdc540f06c"}
 ```
## MCP初始化请求
### Server发送
```json
{
        "type": "mcp",
        "payload":      {
                "jsonrpc":      "2.0",
                "id":   1,
                "method":       "initialize",
                "params":       {
                        "protocolVersion":      "2024-11-05",
                        "capabilities": {
                                "roots":        {
                                        "listChanged":  true
                                },
                                "sampling":     {
                                },
                                "vision":       {
                                        "url":  "http://172.19.0.2:8003/mcp/vision/explain",
                                        "token":        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJkYXRhIjoicTliVEhVVTVjTXVvcVY4dW5NazhqU1pYT2lpYW1rMWFNckZDYmVDWWZDV1dZV01maHZNWjY3RkxWWmYxMTlyQ0YyZHcwVHZtSWthaDhFQ3VhakJBUWdiUE5FNlpjMzd2TFVhVDhHZjdlb0R5RjlGdk81d0NCQT09In0.LednJ_rzkVP2EOn1ZB7tFQucmatFnaS9b84-KVErF4o"
                                }
                        },
                        "clientInfo":   {
                                "name": "XiaozhiClient",
                                "version":      "1.0.0"
                        }
                }
        }
}
```
```json
{
        "type": "mcp",
        "payload":      {
                "jsonrpc":      "2.0",
                "id":   2,
                "method":       "tools/list"
        }
}
```

### Client响应
```json
{"session_id":"d23bb4a8-079e-4772-a9bf-e8878965aacb","type":"mcp","payload":{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2024-11-05","capabilities":{"tools":{}},"serverInfo":{"name":"jiuchuan-s3","version":"1.7.5"}}}}
```
```json
{"session_id":"d23bb4a8-079e-4772-a9bf-e8878965aacb","type":"mcp","payload":{"jsonrpc":"2.0","id":2,"result":{"tools":[{"name":"self.get_device_status","description":"Provides the real-time information of the device, including the current status of the audio speaker, screen, battery, network, etc.\nUse this tool for: \n1. Answering questions about current condition (e.g. what is the current volume of the audio speaker?)\n2. As the first step to control the device (e.g. turn up / down the volume of the audio speaker, etc.)","inputSchema":{"type":"object","properties":{}}},{"name":"self.audio_speaker.set_volume","description":"Set the volume of the audio speaker. If the current volume is unknown, you must call `self.get_device_status` tool first and then call this tool.","inputSchema":{"type":"object","properties":{"volume":{"type":"integer","minimum":0,"maximum":100}},"required":["volume"]}},{"name":"self.screen.set_brightness","description":"Set the brightness of the screen.","inputSchema":{"type":"object","properties":{"brightness":{"type":"integer","minimum":0,"maximum":100}},"required":["brightness"]}},{"name":"self.screen.set_theme","description":"Set the theme of the screen. The theme can be `light` or `dark`.","inputSchema":{"type":"object","properties":{"theme":{"type":"string"}},"required":["theme"]}},{"name":"self.music.play_song","description":"播放指定的歌曲。当用户要求播放音乐时使用此工具，会自动获取歌曲详情并开始流式播放。\n参数:\n  `song_name`: 要播放的歌曲名称。\n返回:\n  播放状态信息，不需确认，立刻播放歌曲。","inputSchema":{"type":"object","properties":{"song_name":{"type":"string"}},"required":["song_name"]}}]}}}
```
## MCP工具调用
### Server发送
```json
{
        "type": "mcp",
        "payload":      {
                "jsonrpc":      "2.0",
                "id":   1,
                "method":       "tools/call",
                "params":       {
                        "name": "self.music.play_song",
                        "arguments":    {
                                "song_name":    "曹操"
                        }
                }
        }
}
```
### Client响应(失败案例)
```json
{"session_id":"dc2ce04a-7db8-4842-8214-1b7ea927102e","type":"mcp","payload":{"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"{\"success\": false, \"message\": \"获取音乐资源失败\"}"}],"isError":false}}}
```

