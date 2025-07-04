package com.xiaozhi.ai.audio

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import com.xiaozhi.ai.audio.utils.OpusDecoder
import com.xiaozhi.ai.audio.utils.OpusEncoder
import com.xiaozhi.ai.audio.utils.OpusStreamPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

/**
 * Opus编解码器使用示例
 * 展示如何使用真正的Opus编解码器进行音频处理
 */
class OpusUsageExample(private val context: Context) {
    companion object {
        private const val TAG = "OpusUsageExample"
        private const val SAMPLE_RATE = 16000
        private const val CHANNELS = 1
        private const val FRAME_DURATION_MS = 60
    }

    /**
     * 示例1：基本的编解码操作
     */
    suspend fun basicEncodeDecodeExample() {
        Log.d(TAG, "=== 基本编解码示例 ===")
        
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, FRAME_DURATION_MS)
        val decoder = OpusDecoder(SAMPLE_RATE, CHANNELS, FRAME_DURATION_MS)
        
        try {
            // 创建一些示例PCM数据（静音）
            val frameSize = SAMPLE_RATE * FRAME_DURATION_MS / 1000 * 2 // 16位
            val pcmData = ByteArray(frameSize) // 静音数据
            
            Log.d(TAG, "原始PCM数据大小: ${pcmData.size} 字节")
            
            // 编码
            val encodedData = encoder.encode(pcmData)
            if (encodedData != null) {
                Log.d(TAG, "编码后数据大小: ${encodedData.size} 字节")
                Log.d(TAG, "压缩比: ${String.format("%.2f", pcmData.size.toFloat() / encodedData.size)}")
                
                // 解码
                val decodedData = decoder.decode(encodedData)
                if (decodedData != null) {
                    Log.d(TAG, "解码后数据大小: ${decodedData.size} 字节")
                    Log.d(TAG, "数据完整性: ${if (decodedData.size == pcmData.size) "✓" else "✗"}")
                } else {
                    Log.e(TAG, "解码失败")
                }
            } else {
                Log.e(TAG, "编码失败")
            }
        } finally {
            encoder.release()
            decoder.release()
        }
    }

    /**
     * 示例2：流式播放
     */
    suspend fun streamPlaybackExample() {
        Log.d(TAG, "=== 流式播放示例 ===")
        
        val encoder = OpusEncoder(SAMPLE_RATE, CHANNELS, FRAME_DURATION_MS)
        val streamPlayer = OpusStreamPlayer(SAMPLE_RATE, CHANNELS, FRAME_DURATION_MS)
        val decoder = OpusDecoder(SAMPLE_RATE, CHANNELS, FRAME_DURATION_MS)
        
        try {
            // 创建一个模拟的音频数据流
            val audioDataFlow = MutableSharedFlow<ByteArray?>()
            
            // 创建PCM数据流（通过解码Opus数据）
            val pcmFlow = flow {
                audioDataFlow.collect { opusData ->
                    opusData?.let {
                        val pcmData = decoder.decode(it)
                        emit(pcmData)
                    }
                }
            }
            
            // 开始流式播放
            streamPlayer.start(pcmFlow)
            Log.d(TAG, "开始流式播放")
            
            // 模拟发送一些音频帧
            val frameSize = SAMPLE_RATE * FRAME_DURATION_MS / 1000 * 2
            repeat(10) { frameIndex ->
                // 创建一些变化的音频数据（简单的正弦波模拟）
                val pcmData = generateSineWave(frameSize, frameIndex)
                val opusData = encoder.encode(pcmData)
                
                if (opusData != null) {
                    audioDataFlow.emit(opusData)
                    Log.d(TAG, "发送音频帧 $frameIndex")
                    delay(FRAME_DURATION_MS.toLong()) // 模拟实时播放
                }
            }
            
            // 等待播放完成
            streamPlayer.waitForPlaybackCompletion()
            Log.d(TAG, "流式播放完成")
            
        } finally {
            streamPlayer.release()
            encoder.release()
            decoder.release()
        }
    }

    /**
     * 示例3：使用OpusCodec对象
     */
    suspend fun opusCodecExample() {
        Log.d(TAG, "=== OpusCodec对象示例 ===")
        
        // 初始化OpusCodec
        if (OpusCodec.initialize()) {
            Log.d(TAG, "OpusCodec初始化成功")
            Log.d(TAG, "采样率: ${OpusCodec.getSampleRate()}")
            Log.d(TAG, "声道数: ${OpusCodec.getChannels()}")
            Log.d(TAG, "帧大小: ${OpusCodec.getFrameSize()} 样本")
            Log.d(TAG, "帧大小: ${OpusCodec.getFrameSizeInBytes()} 字节")
            Log.d(TAG, "帧时长: ${OpusCodec.getFrameDurationMs()} 毫秒")
            
            try {
                // 创建测试数据
                val pcmData = ByteArray(OpusCodec.getFrameSizeInBytes())
                
                // 编码
                val encodedData = OpusCodec.encode(pcmData)
                Log.d(TAG, "编码: ${pcmData.size} -> ${encodedData.size} 字节")
                
                // 解码
                val decodedData = OpusCodec.decode(encodedData)
                Log.d(TAG, "解码: ${encodedData.size} -> ${decodedData.size} 字节")
                
            } finally {
                // 释放资源
                OpusCodec.release()
                Log.d(TAG, "OpusCodec资源已释放")
            }
        } else {
            Log.e(TAG, "OpusCodec初始化失败")
        }
    }

    /**
     * 示例4：使用增强版音频管理器
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    suspend fun enhancedAudioManagerExample() {
        Log.d(TAG, "=== 增强版音频管理器示例 ===")
        
        val audioManager = EnhancedAudioManager(context)
        
        try {
            if (audioManager.initialize()) {
                Log.d(TAG, "增强版音频管理器初始化成功")
                
                // 监听音频事件
                val job = CoroutineScope(Dispatchers.IO).launch {
                    audioManager.audioEvents.collect { event ->
                        when (event) {
                            is AudioEvent.AudioData -> {
                                Log.d(TAG, "收到音频数据: ${event.data.size} 字节")
                                // 可以在这里处理音频数据，比如发送到服务器
                            }
                            is AudioEvent.Error -> {
                                Log.e(TAG, "音频错误: ${event.message}")
                            }
                        }
                    }
                }
                
                // 模拟录音和播放流程
                Log.d(TAG, "开始录音...")
                audioManager.startRecording()
                delay(3000) // 录音3秒
                
                Log.d(TAG, "停止录音")
                audioManager.stopRecording()
                
                job.cancel()
            } else {
                Log.e(TAG, "增强版音频管理器初始化失败")
            }
        } finally {
            audioManager.cleanup()
        }
    }

    /**
     * 生成简单的正弦波数据（用于测试）
     */
    private fun generateSineWave(sizeInBytes: Int, frameIndex: Int): ByteArray {
        val samples = sizeInBytes / 2 // 16位 = 2字节
        val pcmData = ByteArray(sizeInBytes)
        val frequency = 440.0 // A4音符
        val amplitude = 8000.0 // 振幅
        
        for (i in 0 until samples) {
            val time = (frameIndex * samples + i).toDouble() / SAMPLE_RATE
            val sample = (amplitude * kotlin.math.sin(2 * kotlin.math.PI * frequency * time)).toInt().toShort()
            
            // 转换为小端字节序
            pcmData[i * 2] = (sample.toInt() and 0xFF).toByte()
            pcmData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
        }
        
        return pcmData
    }

    /**
     * 运行所有示例
     */
    suspend fun runAllExamples() {
        Log.d(TAG, "开始运行Opus使用示例...")
        
        try {
            basicEncodeDecodeExample()
            delay(1000)
            
            streamPlaybackExample()
            delay(1000)
            
            opusCodecExample()
            delay(1000)
            
            // 注意：音频管理器示例需要录音权限
            // enhancedAudioManagerExample()
            
        } catch (e: Exception) {
            Log.e(TAG, "运行示例时出错", e)
        }
        
        Log.d(TAG, "所有示例运行完成")
    }
}