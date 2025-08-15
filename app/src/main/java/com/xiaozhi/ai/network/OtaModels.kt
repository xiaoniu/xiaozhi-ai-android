package com.xiaozhi.ai.network

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OTA设备上报请求数据类
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DeviceReportRequest(
    @SerialName("application")
    val application: Application,
    
    @SerialName("board")
    val board: BoardInfo
) {
    
    @Serializable
    data class Application(
        @SerialName("version")
        val version: String,
        
        @SerialName("elf_sha256")
        val elfSha256: String
    )
    
    @Serializable
    data class BoardInfo(
        @SerialName("type")
        val type: String,
        
        @SerialName("name")
        val name: String? = null,
        
        @SerialName("ssid")
        val ssid: String,
        
        @SerialName("rssi")
        val rssi: Int,
        
        @SerialName("channel")
        val channel: Int,
        
        @SerialName("ip")
        val ip: String,
        
        @SerialName("mac")
        val mac: String
    )
}

/**
 * OTA响应数据类
 */
@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class OtaResponse(
    @SerialName("server_time")
    val serverTime: ServerTime,
    
    @SerialName("activation")
    val activation: Activation? = null, // 只有在第一次激活时才会返回
    
    @SerialName("firmware")
    val firmware: Firmware,
    
    @SerialName("websocket")
    val websocket: WebSocket
) {
    @Serializable
    data class ServerTime(
        @SerialName("timestamp")
        val timestamp: Long,
        
        @SerialName("timeZone")
        val timeZone: String? = null,
        
        @SerialName("timezone_offset")
        val timezoneOffset: Int
    )
    
    @Serializable
    data class Activation(
        @SerialName("code")
        val code: String,
        
        @SerialName("message")
        val message: String,
        
        @SerialName("challenge")
        val challenge: String
    )
    
    @Serializable
    data class Firmware(
        @SerialName("version")
        val version: String,
        
        @SerialName("url")
        val url: String
    )
    
    @Serializable
    data class WebSocket(
        @SerialName("url")
        val url: String
    )
}