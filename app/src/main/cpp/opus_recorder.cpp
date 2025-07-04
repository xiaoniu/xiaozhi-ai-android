#include <jni.h>
#include <string>
#include <android/log.h>
#include <opus/opus.h>

#define LOG_TAG "OpusJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Opus编码器句柄
static OpusEncoder *encoderHandle = nullptr;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_xiaozhi_ai_audio_utils_OpusEncoder_nativeInitEncoder(JNIEnv *env, jobject thiz,
                                                        jint sample_rate, jint channels,
                                                        jint application) {
    int error;
    OpusEncoder *encoder = opus_encoder_create(sample_rate, channels, application, &error);

    if (error != OPUS_OK || encoder == nullptr) {
        LOGE("Failed to create encoder: %s", opus_strerror(error));
        return 0;
    }

    opus_encoder_ctl(encoder, OPUS_SET_BITRATE(64000));  // 64 kbps
    opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY(10));  // 0-10, 10是最高质量

    LOGI("Opus encoder initialized: sample_rate=%d, channels=%d", sample_rate, channels);
    return (jlong) (intptr_t) encoder;
}

JNIEXPORT jint JNICALL
Java_com_xiaozhi_ai_audio_utils_OpusEncoder_nativeEncodeBytes(JNIEnv *env, jobject thiz,
                                                        jlong encoder_handle,
                                                        jbyteArray input_buffer,
                                                        jint input_size,
                                                        jbyteArray output_buffer,
                                                        jint max_output_size) {
    OpusEncoder *encoder = (OpusEncoder *) (intptr_t) encoder_handle;
    if (encoder == nullptr) {
        LOGE("Encoder handle is null");
        return -1;
    }

    jbyte *input = env->GetByteArrayElements(input_buffer, nullptr);
    jbyte *output = env->GetByteArrayElements(output_buffer, nullptr);

    opus_int16 *pcm = (opus_int16 *) input;
    int frame_size = input_size / 2; // 16位samples的数量

    int result = opus_encode(encoder, pcm, frame_size,
                             (unsigned char *) output, max_output_size);

    env->ReleaseByteArrayElements(input_buffer, input, JNI_ABORT);
    env->ReleaseByteArrayElements(output_buffer, output, 0);

    if (result < 0) {
        LOGE("Encoding failed: %s", opus_strerror(result));
        return -1;
    }

    return result;
}

JNIEXPORT void JNICALL
Java_com_xiaozhi_ai_audio_utils_OpusEncoder_nativeReleaseEncoder(JNIEnv *env, jobject thiz,
                                                           jlong encoder_handle) {
    OpusEncoder *encoder = (OpusEncoder *) (intptr_t) encoder_handle;
    if (encoder != nullptr) {
        opus_encoder_destroy(encoder);
        LOGI("Opus encoder released");
    }
}

} // extern "C"