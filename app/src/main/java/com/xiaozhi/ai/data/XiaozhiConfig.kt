package com.xiaozhi.ai.data

/**
 * 小智配置数据类
 */
data class XiaozhiConfig(
    val id: String,
    val name: String,
    val otaUrl: String,
    val websocketUrl: String = "",
    val macAddress: String,
    val uuid: String,
    val token: String,
    val mcpEnabled: Boolean = false,
    val mcpServers: List<McpServer> = emptyList()
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(): XiaozhiConfig {
            return XiaozhiConfig(
                id = "default",
                name = "测试",
                otaUrl = "",
                websocketUrl = "",
                macAddress = "",
                uuid = generateRandomUuid(),
                token = "test-token",
                mcpEnabled = false,
                mcpServers = listOf(
                    McpServer("示例MCP服务器", "ws://example.com/mcp", false)
                )
            )
        }
        
        /**
         * 生成随机MAC地址
         */
        private fun generateRandomMacAddress(): String {
            val random = java.util.Random()
            val mac = ByteArray(6)
            random.nextBytes(mac)
            return mac.joinToString(":") { "%02X".format(it) }
        }
        
        /**
         * 生成随机UUID
         */
        fun generateRandomUuid(): String {
            return java.util.UUID.randomUUID().toString()
        }
    }
}

/**
 * MCP服务器配置
 */
data class McpServer(
    val name: String,
    val url: String,
    val enabled: Boolean = true
)