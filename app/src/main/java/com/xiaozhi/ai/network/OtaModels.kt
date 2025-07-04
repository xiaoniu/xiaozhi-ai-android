package com.xiaozhi.ai.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OTA设备上报请求数据类
 */
@Serializable
data class DeviceReportRequest(
    @SerialName("version")
    val version: Int,
    
    @SerialName("language")
    val language: String = "zh-CN",
    
    @SerialName("flash_size")
    val flashSize: Int,
    
    @SerialName("minimum_free_heap_size")
    val minimumFreeHeapSize: Int,
    
    @SerialName("mac_address")
    val macAddress: String,
    
    @SerialName("uuid")
    val uuid: String,
    
    @SerialName("chip_model_name")
    val chipModelName: String,
    
    @SerialName("chip_info")
    val chipInfo: ChipInfo,
    
    @SerialName("application")
    val application: Application,
    
    @SerialName("partition_table")
    val partitionTable: List<Partition>,
    
    @SerialName("ota")
    val ota: OtaInfo,
    
    @SerialName("board")
    val board: BoardInfo
) {
    @Serializable
    data class ChipInfo(
        @SerialName("model")
        val model: Int,
        
        @SerialName("cores")
        val cores: Int,
        
        @SerialName("revision")
        val revision: Int,
        
        @SerialName("features")
        val features: Int
    )
    
    @Serializable
    data class Application(
        @SerialName("name")
        val name: String,
        
        @SerialName("version")
        val version: String,
        
        @SerialName("compile_time")
        val compileTime: String,
        
        @SerialName("idf_version")
        val idfVersion: String,
        
        @SerialName("elf_sha256")
        val elfSha256: String
    )
    
    @Serializable
    data class Partition(
        @SerialName("label")
        val label: String,
        
        @SerialName("type")
        val type: Int,
        
        @SerialName("subtype")
        val subtype: Int,
        
        @SerialName("address")
        val address: Int,
        
        @SerialName("size")
        val size: Int
    )
    
    @Serializable
    data class OtaInfo(
        @SerialName("label")
        val label: String
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
        val timeZone: String,
        
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