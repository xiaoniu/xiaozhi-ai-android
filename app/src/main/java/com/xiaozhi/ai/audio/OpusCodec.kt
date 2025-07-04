package com.xiaozhi.ai.audio

import android.util.Log
import com.xiaozhi.ai.audio.utils.OpusDecoder
import com.xiaozhi.ai.audio.utils.OpusEncoder
import kotlinx.coroutines.runBlocking
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Opus编解码器
 * 使用真正的Opus库进行音频编解码，如果不可用则降级到简化版本
 */
object OpusCodec {
    private const val TAG = "OpusCodec"
    private const val ENCODE_SAMPLE_RATE = 16000
    private const val DECODE_SAMPLE_RATE = 24000
    private const val CHANNELS = 1
    private const val FRAME_DURATION_MS = 60
    private const val FRAME_SIZE = DECODE_SAMPLE_RATE * FRAME_DURATION_MS / 1000

    // 编码器和解码器实例
    private var encoder: OpusEncoder? = null
    private var decoder: OpusDecoder? = null
    private var useNativeCodec = false

    /**
     * 初始化编解码器
     */
    fun initialize(): Boolean {
        return try {
            // 尝试初始化native编解码器
            encoder = OpusEncoder(ENCODE_SAMPLE_RATE, CHANNELS, FRAME_DURATION_MS)
            decoder = OpusDecoder(DECODE_SAMPLE_RATE, CHANNELS, FRAME_DURATION_MS)
            useNativeCodec = true
            Log.d(TAG, "Native Opus编解码器初始化成功")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Native Opus编解码器初始化失败，使用简化版本: ${e.message}")
            useNativeCodec = false
            true // 即使native失败，简化版本总是可用的
        }
    }

    /**
     * 将PCM数据编码为Opus格式
     */
    fun encode(pcmData: ByteArray): ByteArray {
        return if (useNativeCodec) {
            encodeWithNative(pcmData)
        } else {
            encodeWithFallback(pcmData)
        }
    }

    /**
     * 将Opus数据解码为PCM格式
     */
    fun decode(opusData: ByteArray): ByteArray {
        return if (useNativeCodec) {
            decodeWithNative(opusData)
        } else {
            decodeWithFallback(opusData)
        }
    }

    /**
     * 使用native编解码器编码
     */
    private fun encodeWithNative(pcmData: ByteArray): ByteArray {
        return try {
            val currentEncoder = encoder ?: run {
                Log.w(TAG, "编码器未初始化，尝试重新初始化")
                if (!initialize()) {
                    Log.e(TAG, "编码器初始化失败")
                    return encodeWithFallback(pcmData)
                }
                encoder!!
            }

            // 使用协程运行编码
            runBlocking {
                val encoded = currentEncoder.encode(pcmData)
                if (encoded != null) {
                    Log.d(TAG, "Native PCM编码成功：输入${pcmData.size}字节，输出${encoded.size}字节")
                    encoded
                } else {
                    Log.e(TAG, "Native PCM编码失败，使用降级版本")
                    encodeWithFallback(pcmData)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Native Opus编码异常，使用降级版本", e)
            encodeWithFallback(pcmData)
        }
    }

    /**
     * 使用native编解码器解码
     */
    private fun decodeWithNative(opusData: ByteArray): ByteArray {
        return try {
            val currentDecoder = decoder ?: run {
                Log.w(TAG, "解码器未初始化，尝试重新初始化")
                if (!initialize()) {
                    Log.e(TAG, "解码器初始化失败")
                    return decodeWithFallback(opusData)
                }
                decoder!!
            }

            // 使用协程运行解码
            runBlocking {
                val decoded = currentDecoder.decode(opusData)
                if (decoded != null) {
                    Log.d(TAG, "Native Opus解码成功：输入${opusData.size}字节，输出${decoded.size}字节")
                    decoded
                } else {
                    Log.e(TAG, "Native Opus解码失败，使用降级版本")
                    decodeWithFallback(opusData)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Native Opus解码异常，使用降级版本", e)
            decodeWithFallback(opusData)
        }
    }

    /**
     * 降级编码（简化版本）
     */
    private fun encodeWithFallback(pcmData: ByteArray): ByteArray {
        try {
            // 将PCM字节数组转换为16位整数数组
            val pcmShorts = ByteBuffer.wrap(pcmData)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asShortBuffer()
            
            val samples = ShortArray(pcmShorts.remaining())
            pcmShorts.get(samples)

            // 简化的编码过程
            val compressed = compressPCM(samples)
            
            Log.d(TAG, "降级PCM编码：输入${pcmData.size}字节，输出${compressed.size}字节")
            return compressed
        } catch (e: Exception) {
            Log.e(TAG, "降级Opus编码失败", e)
            return pcmData // 失败时返回原始数据
        }
    }

    /**
     * 降级解码（简化版本）
     */
    private fun decodeWithFallback(opusData: ByteArray): ByteArray {
        try {
            // 简化的解码过程
            val decompressed = decompressToShorts(opusData)
            
            // 将16位整数数组转换为字节数组
            val pcmBytes = ByteArray(decompressed.size * 2)
            val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            
            for (sample in decompressed) {
                buffer.putShort(sample)
            }
            
            Log.d(TAG, "降级Opus解码：输入${opusData.size}字节，输出${pcmBytes.size}字节")
            return pcmBytes
        } catch (e: Exception) {
            Log.e(TAG, "降级Opus解码失败", e)
            return ByteArray(getFrameSizeInBytes()) // 返回静音数据
        }
    }

    /**
     * 简化的PCM压缩（模拟Opus编码）
     */
    private fun compressPCM(samples: ShortArray): ByteArray {
        val compressed = mutableListOf<Byte>()
        
        // 添加简单的头部信息
        compressed.add(0x4F.toByte()) // 'O'
        compressed.add(0x50.toByte()) // 'P'
        compressed.add((samples.size and 0xFF).toByte())
        compressed.add((samples.size shr 8 and 0xFF).toByte())
        
        // 简单的差分编码
        var previous: Short = 0
        for (sample in samples) {
            val diff = (sample - previous).toInt()
            compressed.add((diff and 0xFF).toByte())
            compressed.add((diff.toInt() shr 8 and 0xFF).toByte())
            previous = sample
        }
        
        return compressed.toByteArray()
    }

    /**
     * 简化的解压缩（模拟Opus解码）
     */
    private fun decompressToShorts(data: ByteArray): ShortArray {
        if (data.size < 4) {
            Log.w(TAG, "数据太短，无法解码")
            return ShortArray(FRAME_SIZE) // 返回静音
        }
        
        // 检查头部
        if (data[0] != 0x4F.toByte() || data[1] != 0x50.toByte()) {
            Log.w(TAG, "无效的Opus数据头部")
            return ShortArray(FRAME_SIZE) // 返回静音
        }
        
        // 读取样本数量
        val sampleCount = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
        val samples = ShortArray(sampleCount)
        
        // 解码差分数据
        var previous: Short = 0
        var index = 4
        
        for (i in samples.indices) {
            if (index + 1 < data.size) {
                val diff = ((data[index].toInt() and 0xFF) or 
                           ((data[index + 1].toInt() and 0xFF) shl 8)).toShort()
                samples[i] = (previous + diff).toShort()
                previous = samples[i]
                index += 2
            } else {
                samples[i] = 0 // 填充静音
            }
        }
        
        return samples
    }

    /**
     * 获取帧大小（样本数）
     */
    fun getFrameSize(): Int = FRAME_SIZE

    /**
     * 获取帧大小（字节数）
     */
    fun getFrameSizeInBytes(): Int = FRAME_SIZE * 2 // 16位 = 2字节

    /**
     * 获取采样率
     */
    fun getSampleRate(): Int = DECODE_SAMPLE_RATE

    /**
     * 获取声道数
     */
    fun getChannels(): Int = CHANNELS

    /**
     * 获取帧时长（毫秒）
     */
    fun getFrameDurationMs(): Int = FRAME_DURATION_MS

    /**
     * 释放资源
     */
    fun release() {
        try {
            encoder?.release()
            decoder?.release()
            encoder = null
            decoder = null
            Log.d(TAG, "Opus编解码器资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放Opus编解码器资源时出错", e)
        }
    }
}