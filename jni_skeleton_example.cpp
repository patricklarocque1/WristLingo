// whisper_bridge.cpp (skeleton)
#include "whisper_bridge.h"
#include "whisper.cpp/whisper.h"
#include <android/log.h>
#include <vector>
#include <string>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "WhisperBridge", __VA_ARGS__)

struct WhisperCtx {
    whisper_context* ctx = nullptr;
    std::vector<float> buffer;
    int sampleRate = 16000;
};

extern "C" jlong Java_com_wristlingo_asr_whisper_NativeWhisper_create(
        JNIEnv* env, jobject thiz, jstring jModelPath, jint jSampleRate) {
    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    whisper_context_params cparams = whisper_context_default_params();
    auto* wctx = new WhisperCtx();
    wctx->sampleRate = (int) jSampleRate;
    wctx->ctx = whisper_init_from_file_with_params(modelPath, cparams);
    env->ReleaseStringUTFChars(jModelPath, modelPath);
    return (jlong) wctx;
}

extern "C" void Java_com_wristlingo_asr_whisper_NativeWhisper_destroy(
        JNIEnv*, jobject, jlong handle) {
    auto* wctx = (WhisperCtx*) handle;
    if (!wctx) return;
    if (wctx->ctx) whisper_free(wctx->ctx);
    delete wctx;
}

extern "C" void Java_com_wristlingo_asr_whisper_NativeWhisper_feedPcm(
        JNIEnv* env, jobject, jlong handle, jshortArray jpcm, jint length) {
    auto* wctx = (WhisperCtx*) handle;
    if (!wctx) return;
    jshort* pcm = env->GetShortArrayElements(jpcm, nullptr);
    for (int i = 0; i < length; ++i) {
        wctx->buffer.push_back((float) pcm[i] / 32768.0f);
    }
    env->ReleaseShortArrayElements(jpcm, pcm, JNI_ABORT);
}

extern "C" jstring Java_com_wristlingo_asr_whisper_NativeWhisper_finalizeStream(
        JNIEnv* env, jobject, jlong handle) {
    auto* wctx = (WhisperCtx*) handle;
    if (!wctx || !wctx->ctx) return env->NewStringUTF("");
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.translate = false;
    params.no_timestamps = true;

    int ret = whisper_full(wctx->ctx, params, wctx->buffer.data(), wctx->buffer.size());
    std::string out;
    if (ret == 0) {
        const int n = whisper_full_n_segments(wctx->ctx);
        for (int i = 0; i < n; ++i) {
            out += whisper_full_get_segment_text(wctx->ctx, i);
        }
    }
    wctx->buffer.clear();
    return env->NewStringUTF(out.c_str());
}
