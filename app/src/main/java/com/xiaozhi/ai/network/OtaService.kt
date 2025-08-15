package com.xiaozhi.ai.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OTA服务类，负责处理设备上报和获取OTA信息
 */
class OtaService {
    companion object {
        private const val TAG = "OtaService"
        private const val TIMEOUT_SECONDS = 30L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    /**
     * 向服务器上报设备信息并获取OTA响应
     */
    suspend fun reportDeviceAndGetOta(clientId: String, deviceId: String, otaUrl: String? = null): Result<OtaResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val url = otaUrl?.takeIf { it.isNotBlank() } ?: ""
                val deviceRequest = createDeviceReportRequest(clientId, deviceId)
                val requestBodyString = json.encodeToString(DeviceReportRequest.serializer(), deviceRequest)
                val requestBody = requestBodyString.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Client-Id", clientId)
                    .addHeader("Device-Id", deviceId)
                    .addHeader("Content-Type", "application/json")
                    .build()

                Log.d(TAG, "发送OTA请求到: $url")
                Log.d(TAG, "请求头 - Client-Id: $clientId, Device-Id: $deviceId")
                Log.d(TAG, "请求体数据: $requestBodyString")

                val response = client.newCall(request).execute()
                
                Log.d(TAG, "收到响应 - 状态码: ${response.code}, 消息: ${response.message}")
                Log.d(TAG, "响应头: ${response.headers}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        Log.d(TAG, "OTA响应成功 - 状态码: ${response.code}")
                        Log.d(TAG, "响应体数据: $responseBody")
                        val otaResponse = json.decodeFromString(OtaResponse.serializer(), responseBody)
                        Log.d(TAG, "解析后的OTA响应对象: $otaResponse")
                        Result.success(otaResponse)
                    } else {
                        Log.e(TAG, "OTA响应体为空 - 状态码: ${response.code}")
                        Result.failure(Exception("响应体为空"))
                    }
                } else {
                    val errorBody = response.body?.string() ?: "未知错误"
                    Log.e(TAG, "OTA请求失败 - 状态码: ${response.code}, 消息: ${response.message}")
                    Log.e(TAG, "错误响应体: $errorBody")
                    Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "OTA请求异常", e)
                Result.failure(e)
            }
        }
    }

    /**
     * 创建设备上报请求数据 - 针对非ESP32设备的最小请求
     */
    private fun createDeviceReportRequest(clientId: String, deviceId: String): DeviceReportRequest {
        return DeviceReportRequest(
            application = DeviceReportRequest.Application(
                version = "2.0.0",
                elfSha256 = "c8a8ecb6d6fbcda682494d9675cd1ead240ecf38bdde75282a42365a0e396033"
            ),
            board = DeviceReportRequest.BoardInfo(
                type = "wifi", // 标识为Wi-Fi设备类型
                name = "xiaozhi-android",
                ssid = "卧室", // Wi-Fi SSID，如需要可从系统获取
                rssi = -55, // Wi-Fi信号强度，如需要可从系统获取
                channel = 1, // Wi-Fi频道，如需要可从系统获取
                ip = "192.168.1.11", // IP地址，如需要可从系统获取
                mac = deviceId // 使用设备ID作为MAC地址
            )
        )
    }
}