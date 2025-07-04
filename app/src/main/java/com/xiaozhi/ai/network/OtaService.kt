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
     * 创建设备上报请求数据
     */
    private fun createDeviceReportRequest(clientId: String, deviceId: String): DeviceReportRequest {
        return DeviceReportRequest(
            version = 1,
            language = "zh-CN",
            flashSize = 0,
            minimumFreeHeapSize = 0,
            macAddress = deviceId,
            uuid = clientId,
            chipModelName = "",
            chipInfo = DeviceReportRequest.ChipInfo(
                model = 0,
                cores = 0,
                revision = 0,
                features = 0
            ),
            application = DeviceReportRequest.Application(
                name = "xiaozhi-android-watch",
                version = "2.0.0",
                compileTime = "2025-06-19 10:00:00",
                idfVersion = "4.4.3",
                elfSha256 = "1234567890abcdef1234567890abcdef1234567890abcdef"
            ),
            partitionTable = listOf(
                DeviceReportRequest.Partition(
                    label = "",
                    type = 0,
                    subtype = 0,
                    address = 0,
                    size = 0
                )
            ),
            ota = DeviceReportRequest.OtaInfo(
                label = "xiaozhi-android-watch"
            ),
            board = DeviceReportRequest.BoardInfo(
                type = "xiaozhi-android-watch",
                name = "xiaozhi-android-watch",
                ssid = "xiaozhi-android-watch",
                rssi = 0,
                channel = 0,
                ip = "192.168.1.1",
                mac = deviceId
            )
        )
    }
}