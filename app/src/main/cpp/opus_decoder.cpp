#include <jni.h>
#include <string>
#include <android/log.h>
#include <opus/opus.h>

#define LOG_TAG "OpusJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static OpusDecoder* decoderHandle = nullptr;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_xiaozhi_ai_audio_utils_OpusDecoder_nativeInitDecoder(JNIEnv *env, jobject thiz,
                                                        jint sample_rate, jint channels) {
    int error;
    OpusDecoder *decoder = opus_decoder_create(sample_rate, channels, &error);

    if (error != OPUS_OK || decoder == nullptr) {
        LOGE("Failed to create decoder: %s", opus_strerror(error));
        return 0;
    }

    LOGI("Opus decoder initialized: sample_rate=%d, channels=%d", sample_rate, channels);
    return (jlong)(intptr_t)decoder;
}

JNIEXPORT jint JNICALL
Java_com_xiaozhi_ai_audio_utils_OpusDecoder_nativeDecodeBytes(JNIEnv *env, jobject thiz,
                                                        jlong decoder_handle,
                                                        jbyteArray input_buffer,
                                                        jint input_size,
                                                        jbyteArray output_buffer,
                                                        jint max_output_size) {
    OpusDecoder *decoder = (OpusDecoder*)(intptr_t)decoder_handle;
    if (decoder == nullptr) {
        LOGE("Decoder handle is null");
        return -1;
    }

    jbyte *input = env->GetByteArrayElements(input_buffer, nullptr);
    jbyte *output = env->GetByteArrayElements(output_buffer, nullptr);

    int frame_size = max_output_size / 2; // 16-bit PCM
    int result = opus_decode(decoder, (unsigned char*)input, input_size,
                             (opus_int16*)output, frame_size, 0);

    env->ReleaseByteArrayElements(input_buffer, input, JNI_ABORT);
    env->ReleaseByteArrayElements(output_buffer, output, 0);

    if (result < 0) {
        LOGE("Decoding failed: %s", opus_strerror(result));
        return -1;
    }

    return result * 2; // 返回字节数（每个样本2字节）
}

JNIEXPORT void JNICALL
Java_com_xiaozhi_ai_audio_utils_OpusDecoder_nativeReleaseDecoder(JNIEnv *env, jobject thiz,
                                                           jlong decoder_handle) {
    OpusDecoder *decoder = (OpusDecoder*)(intptr_t)decoder_handle;
    if (decoder != nullptr) {
        opus_decoder_destroy(decoder);
        LOGI("Opus decoder released");
    }
}

} // extern "C"