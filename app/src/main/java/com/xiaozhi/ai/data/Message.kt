package com.xiaozhi.ai.data

import java.util.*

/**
 * 消息角色枚举
 */
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

/**
 * 消息数据类
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAudio: Boolean = false
)