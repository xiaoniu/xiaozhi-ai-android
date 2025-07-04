package com.xiaozhi.ai.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.xiaozhi.ai.audio.utils.OpusDecoder
import com.xiaozhi.ai.audio.utils.OpusEncoder
import com.xiaozhi.ai.audio.utils.OpusStreamPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow

/**
 * 音频事件
 */
sealed class AudioEvent {
    data class AudioData(val data: ByteArray) : AudioEvent()
    data class Error(val message: String) : AudioEvent()
}

/**
 * 增强版音频管理器
 * 使用真正的Opus编解码器和流式播放
 */
class EnhancedAudioManager(private val context: Context) {
    companion object {
        private const val TAG = "EnhancedAudioManager"
        private const val RECORD_SAMPLE_RATE = 16000
        private const val PLAY_SAMPLE_RATE = 24000
        private const val CHANNELS = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_DURATION_MS = 60
        private const val FRAME_SIZE = RECORD_SAMPLE_RATE * FRAME_DURATION_MS / 1000 * 2 // 16bit = 2 bytes
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isPlayingState = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _audioEvents = MutableSharedFlow<AudioEvent>()
    val audioEvents: SharedFlow<AudioEvent> = _audioEvents

    // AEC和NS处理器
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    // Opus编解码器
    private var opusEncoder: OpusEncoder? = null
    private var opusDecoder: OpusDecoder? = null
    private var streamPlayer: OpusStreamPlayer? = null
    
    // 音频播放流
    private val _audioPlaybackFlow = MutableSharedFlow<ByteArray>()
    private var playbackJob: Job? = null
    private var isPlaybackSetup = false

    /**
     * 初始化音频系统
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initialize(): Boolean {
        return try {
            // 初始化Opus编解码器
            opusEncoder = OpusEncoder(RECORD_SAMPLE_RATE, 1, FRAME_DURATION_MS)
            opusDecoder = OpusDecoder(PLAY_SAMPLE_RATE, 1, FRAME_DURATION_MS)
            streamPlayer = OpusStreamPlayer(PLAY_SAMPLE_RATE, 1, FRAME_DURATION_MS, context)
            
            setupAudioRecord()
            // 不在初始化时启动播放流，而是在需要时启动
            Log.d(TAG, "增强版音频系统初始化成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "增强版音频系统初始化失败", e)
            false
        }
    }

    /**
     * 设置录音器
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun setupAudioRecord() {
        if (!checkPermissions()) {
            throw SecurityException("缺少录音权限")
        }

        val bufferSize = AudioRecord.getMinBufferSize(RECORD_SAMPLE_RATE, CHANNELS, AUDIO_FORMAT)
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            RECORD_SAMPLE_RATE,
            CHANNELS,
            AUDIO_FORMAT,
            bufferSize * 2
        )

        // 设置AEC和NS
        setupAudioEffects()
    }

    /**
     * 设置音频效果（AEC+NS）
     */
    private fun setupAudioEffects() {
        audioRecord?.let { record ->
            try {
                // 回音消除
                if (AcousticEchoCanceler.isAvailable()) {
                    acousticEchoCanceler = AcousticEchoCanceler.create(record.audioSessionId)
                    acousticEchoCanceler?.enabled = true
                    Log.d(TAG, "AEC已启用")
                } else {
                    Log.w(TAG, "设备不支持AEC")
                }

                // 噪声抑制
                if (NoiseSuppressor.isAvailable()) {
                    noiseSuppressor = NoiseSuppressor.create(record.audioSessionId)
                    noiseSuppressor?.enabled = true
                    Log.d(TAG, "NS已启用")
                } else {
                    Log.w(TAG, "设备不支持NS")
                }
            } catch (e: Exception) {
                Log.e(TAG, "设置音频效果失败", e)
            }
        }
    }

    /**
     * 设置音频播放流
     */
    private fun setupAudioPlayback() {
        // 防止重复设置播放流
        if (isPlaybackSetup) {
            Log.d(TAG, "播放流已经设置，跳过重复设置")
            return
        }
        
        isPlaybackSetup = true
        Log.d(TAG, "首次设置音频播放流")
        
        // 创建持续的PCM数据流
        val pcmFlow = flow {
            _audioPlaybackFlow.collect { opusData ->
                try {
                    opusDecoder?.let { decoder ->
                        val pcmData = decoder.decode(opusData)
                        pcmData?.let { 
                            emit(it)
                            Log.d(TAG, "解码音频数据，PCM大小: ${it.size}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解码音频数据失败", e)
                }
            }
        }
        
        // 启动播放器
        streamPlayer?.start(pcmFlow)
        Log.d(TAG, "音频播放流已设置并启动")
    }

    /**
     * 开始录音
     */
    fun startRecording() {
        if (isRecording) return

        audioRecord?.let { record ->
            try {
                record.startRecording()
                isRecording = true
                Log.d(TAG, "开始录音")

                scope.launch {
                    val buffer = ByteArray(FRAME_SIZE)
                    while (isRecording) {
                        val bytesRead = record.read(buffer, 0, buffer.size)
                        if (bytesRead > 0) {
                            // 使用真正的Opus编码器
                            opusEncoder?.let { encoder ->
                                val opusData = encoder.encode(buffer.copyOf(bytesRead))
                                opusData?.let { 
                                    _audioEvents.emit(AudioEvent.AudioData(it))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "录音失败", e)
                scope.launch {
                    _audioEvents.emit(AudioEvent.Error("录音失败: ${e.message}"))
                }
            }
        }
    }

    /**
     * 停止录音
     */
    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        audioRecord?.stop()
        Log.d(TAG, "停止录音")
    }

    /**
     * 播放音频数据（单次播放）
     */
    fun playAudio(audioData: ByteArray) {
        scope.launch {
            try {
                // 确保播放器已经启动
                if (!isPlayingState) {
                    isPlayingState = true
                    Log.d(TAG, "首次播放音频，设置播放流")
                    setupAudioPlayback()
                }
                
                // 直接发送到播放流
                _audioPlaybackFlow.emit(audioData)
                Log.d(TAG, "发送音频数据到播放流，长度: ${audioData.size}")
            } catch (e: Exception) {
                Log.e(TAG, "播放音频失败", e)
            }
        }
    }

    /**
     * 停止播放
     */
    fun stopPlaying() {
        isPlayingState = false
        isPlaybackSetup = false
        streamPlayer?.stop()
        
        Log.d(TAG, "停止播放并重置状态")
    }

    /**
     * 开始流式播放
     */
    fun startStreamPlayback(opusDataFlow: SharedFlow<ByteArray>) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            isPlayingState = true
            opusDataFlow.collect { opusData ->
                _audioPlaybackFlow.emit(opusData)
            }
        }
        Log.d(TAG, "开始流式播放")
    }

    /**
     * 停止流式播放
     */
    fun stopStreamPlayback() {
        isPlayingState = false
        isPlaybackSetup = false
        playbackJob?.cancel()
        streamPlayer?.stop()
        
        Log.d(TAG, "停止流式播放并重置状态")
    }

    /**
     * 等待播放完成
     */
    suspend fun waitForPlaybackCompletion() {
        streamPlayer?.waitForPlaybackCompletion()
    }

    /**
     * 检查权限
     */
    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stopRecording()
        stopStreamPlayback()
        
        acousticEchoCanceler?.release()
        noiseSuppressor?.release()
        
        audioRecord?.release()
        
        // 释放Opus编解码器资源
        opusEncoder?.release()
        opusDecoder?.release()
        streamPlayer?.release()
        
        scope.cancel()
        Log.d(TAG, "增强版音频资源已清理")
    }

    /**
     * 获取录音状态
     */
    fun isRecording(): Boolean = isRecording

    /**
     * 获取播放状态
     */
    fun isPlaying(): Boolean = isPlayingState
    
    /**
     * 测试音频播放（生成一个简单的测试音调）
     */
    fun testAudioPlayback() {
        scope.launch {
            try {
                // 创建一个独立的测试播放器
                val testPlayer = OpusStreamPlayer(PLAY_SAMPLE_RATE, 1, FRAME_DURATION_MS, context)
                
                // 生成一个440Hz的测试音调（A4音符）
                val sampleRate = PLAY_SAMPLE_RATE
                val duration = 1.0 // 1秒
                val frequency = 440.0
                val samples = (sampleRate * duration).toInt()
                val pcmData = ByteArray(samples * 2) // 16-bit = 2 bytes per sample
                
                for (i in 0 until samples) {
                    val sample = (32767 * kotlin.math.sin(2 * kotlin.math.PI * frequency * i / sampleRate)).toInt().toShort()
                    pcmData[i * 2] = (sample.toInt() and 0xFF).toByte()
                    pcmData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
                }
                
                // 直接发送PCM数据到测试播放器
                val testFlow = flow {
                    emit(pcmData)
                }
                
                testPlayer.start(testFlow)
                Log.d(TAG, "开始播放测试音调")
                
                // 等待播放完成
                delay(1500)
                testPlayer.stop()
                testPlayer.release()
                Log.d(TAG, "测试音调播放完成")
                
            } catch (e: Exception) {
                Log.e(TAG, "测试音频播放失败", e)
            }
        }
    }
}