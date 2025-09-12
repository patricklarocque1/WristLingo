#include "whisper_bridge.h"

#include <android/log.h>
#include <vector>
#include <memory>
#include <cstring>

// whisper.cpp headers
#include "whisper.h"

#define LOG_TAG "WhisperBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
    struct WhisperContextHolder {
        whisper_context* context { nullptr };
        std::vector<float> pcmBuffer; // accumulate as floats for whisper
        int sampleRate { 16000 };
    };

    WhisperContextHolder* fromPtr(jlong ptr) {
        return reinterpret_cast<WhisperContextHolder*>(ptr);
    }

    jlong toPtr(WhisperContextHolder* holder) {
        return reinterpret_cast<jlong>(holder);
    }
}

JNIEXPORT jlong JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_create(
        JNIEnv* env,
        jobject /*thiz*/,
        jstring modelPath,
        jint sampleRate) {
    const char* cModelPath = env->GetStringUTFChars(modelPath, nullptr);
    if (!cModelPath) {
        return 0;
    }

    auto* holder = new (std::nothrow) WhisperContextHolder();
    if (!holder) {
        env->ReleaseStringUTFChars(modelPath, cModelPath);
        return 0;
    }
    holder->sampleRate = sampleRate;

    whisper_context_params cparams = whisper_context_default_params();
    holder->context = whisper_init_from_file_with_params(cModelPath, cparams);
    env->ReleaseStringUTFChars(modelPath, cModelPath);

    if (!holder->context) {
        delete holder;
        LOGE("Failed to initialize whisper context");
        return 0;
    }

    LOGI("Whisper context created");
    return toPtr(holder);
}

JNIEXPORT void JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_feedPcm(
        JNIEnv* env,
        jobject /*thiz*/,
        jlong ctxPtr,
        jshortArray pcm,
        jint length) {
    auto* holder = fromPtr(ctxPtr);
    if (!holder || !holder->context || length <= 0) return;

    jboolean isCopy = JNI_FALSE;
    jshort* data = env->GetShortArrayElements(pcm, &isCopy);
    if (!data) return;

    // Convert int16 PCM to float [-1,1]
    size_t len = static_cast<size_t>(length);
    holder->pcmBuffer.reserve(holder->pcmBuffer.size() + len);
    for (size_t i = 0; i < len; ++i) {
        holder->pcmBuffer.push_back(static_cast<float>(data[i]) / 32768.0f);
    }

    env->ReleaseShortArrayElements(pcm, data, JNI_ABORT);
}

JNIEXPORT jstring JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_finalizeStream(
        JNIEnv* env,
        jobject /*thiz*/,
        jlong ctxPtr) {
    auto* holder = fromPtr(ctxPtr);
    if (!holder || !holder->context) {
        return env->NewStringUTF("");
    }

    // Basic single-shot run using accumulated audio
    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.translate = false;
    wparams.no_context = true;

    int rc = whisper_full(holder->context, wparams, holder->pcmBuffer.data(), holder->pcmBuffer.size());
    if (rc != 0) {
        LOGE("whisper_full failed: %d", rc);
        holder->pcmBuffer.clear();
        return env->NewStringUTF("");
    }

    // Collect segments into a single string
    std::string result;
    const int n_segments = whisper_full_n_segments(holder->context);
    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(holder->context, i);
        if (text) {
            result += text;
            if (i + 1 < n_segments) result += " ";
        }
    }

    holder->pcmBuffer.clear();
    return env->NewStringUTF(result.c_str());
}

JNIEXPORT void JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_destroy(
        JNIEnv* /*env*/,
        jobject /*thiz*/,
        jlong ctxPtr) {
    auto* holder = fromPtr(ctxPtr);
    if (!holder) return;
    if (holder->context) {
        whisper_free(holder->context);
        holder->context = nullptr;
    }
    delete holder;
}

JNIEXPORT jstring JNICALL
Java_com_wristlingo_app_nativebridge_NativeWhisper_nativePartial(
        JNIEnv* env,
        jobject /*thiz*/,
        jlong ctxPtr,
        jint windowMs) {
    auto* holder = fromPtr(ctxPtr);
    if (!holder || !holder->context) return env->NewStringUTF("");
    // If whisper had a streaming partial API, we'd use it. As a placeholder,
    // run a greedy decode on the last N ms window (2-3s recommended).
    const int sr = holder->sampleRate;
    const int windowSamples = (windowMs * sr) / 1000;
    if (windowSamples <= 0) return env->NewStringUTF("");
    const int total = (int) holder->pcmBuffer.size();
    const int start = total > windowSamples ? total - windowSamples : 0;
    const float* data = holder->pcmBuffer.data() + start;

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = false;
    wparams.translate = false;
    wparams.no_context = true;

    int rc = whisper_full(holder->context, wparams, data, total - start);
    if (rc != 0) return env->NewStringUTF("");

    std::string result;
    const int n_segments = whisper_full_n_segments(holder->context);
    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(holder->context, i);
        if (text) {
            result += text;
            if (i + 1 < n_segments) result += " ";
        }
    }
    return env->NewStringUTF(result.c_str());
}


