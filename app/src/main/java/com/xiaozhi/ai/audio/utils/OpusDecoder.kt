package com.xiaozhi.ai.audio.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OpusDecoder(
    private val sampleRate: Int,
    private val channels: Int,
    frameSizeMs: Int
) {
    companion object {
        private const val TAG = "OpusDecoder"

        init {
            System.loadLibrary("app")
        }
    }

    private var nativeDecoderHandle: Long = 0
    private val frameSize: Int = (sampleRate * frameSizeMs) / 1000

    init {
        nativeDecoderHandle = nativeInitDecoder(sampleRate, channels)
        if (nativeDecoderHandle == 0L) {
            throw IllegalStateException("Failed to initialize Opus decoder")
        }
    }

    // 使用协程进行解码，运行在 IO 线程
    suspend fun decode(opusData: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        val maxPcmSize = frameSize * channels * 2 // 16-bit PCM
        val pcmBuffer = ByteArray(maxPcmSize)

        val decodedBytes = nativeDecodeBytes(
            nativeDecoderHandle,
            opusData,
            opusData.size,
            pcmBuffer,
            maxPcmSize
        )

        if (decodedBytes > 0) {
            if (decodedBytes < pcmBuffer.size) {
                pcmBuffer.copyOf(decodedBytes)
            } else {
                pcmBuffer
            }
        } else {
            Log.e(TAG, "Failed to decode frame")
            null
        }
    }

    fun release() {
        if (nativeDecoderHandle != 0L) {
            nativeReleaseDecoder(nativeDecoderHandle)
            nativeDecoderHandle = 0
        }
    }

    protected fun finalize() {
        release()
    }

    private external fun nativeInitDecoder(sampleRate: Int, channels: Int): Long
    private external fun nativeDecodeBytes(
        decoderHandle: Long,
        inputBuffer: ByteArray,
        inputSize: Int,
        outputBuffer: ByteArray,
        maxOutputSize: Int
    ): Int

    private external fun nativeReleaseDecoder(decoderHandle: Long)
}