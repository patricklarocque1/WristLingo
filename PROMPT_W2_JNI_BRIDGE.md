## CODEX-WEB — W2: Add whisper.cpp as submodule and JNI bridge

Add `whisper.cpp` as a **submodule** under `app/src/main/cpp/whisper.cpp` and wire CMake.

1) **Git submodule** (document in README):
   - `git submodule add https://github.com/ggerganov/whisper.cpp app/src/main/cpp/whisper.cpp`
   - `git submodule update --init --recursive`
   - Do NOT vendor model files.

2) **CMakeLists.txt** (in app/src/main/cpp):
```cmake
cmake_minimum_required(VERSION 3.22)
project(wristlingo_whisper)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimize for mobile (no OpenMP by default)
add_definitions(-DGGML_USE_ACCELERATE=0 -DGGML_USE_OPENMP=0)

# whisper.cpp core
add_subdirectory(whisper.cpp)

# Our JNI bridge
add_library(whisper_bridge SHARED whisper_bridge.cpp)
target_include_directories(whisper_bridge PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/whisper.cpp)
target_link_libraries(whisper_bridge PRIVATE whisper)

# Android log
find_library(log-lib log)
target_link_libraries(whisper_bridge PRIVATE ${log-lib})
```

3) **JNI header (whisper_bridge.h)** define a minimal C API:
```cpp
#pragma once
#include <jni.h>
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong JNICALL Java_com_wristlingo_asr_whisper_NativeWhisper_create(
  JNIEnv*, jobject, jstring modelPath, jint sampleRate);

JNIEXPORT void JNICALL Java_com_wristlingo_asr_whisper_NativeWhisper_destroy(
  JNIEnv*, jobject, jlong ctxPtr);

JNIEXPORT void JNICALL Java_com_wristlingo_asr_whisper_NativeWhisper_feedPcm(
  JNIEnv*, jobject, jlong ctxPtr, jshortArray pcm, jint length);

JNIEXPORT jstring JNICALL Java_com_wristlingo_asr_whisper_NativeWhisper_finalizeStream(
  JNIEnv*, jobject, jlong ctxPtr);

#ifdef __cplusplus
}
#endif
```

4) **JNI impl (whisper_bridge.cpp)**: 
- Hold a `struct WhisperCtx { struct whisper_context* ctx; std::vector<float> buffer; int sampleRate; };`
- `create()` loads model with `whisper_init_from_file_with_params`.
- `feedPcm()` append PCM -> float buffer `pcm/32768.f`.
- `finalizeStream()` runs `whisper_full()` with reasonable params (e.g., `translate=false`, `duration_ms` computed from buffer size), collects text via `whisper_full_n_segments()` and returns a UTF-8 string; then clears buffer.

5) **Kotlin wrapper** `NativeWhisper.kt` in :app:
```kotlin
class NativeWhisper(private val modelPath: String, private val sampleRate: Int) {
    companion object { init { System.loadLibrary("whisper_bridge") } }
    private external fun create(modelPath: String, sampleRate: Int): Long
    private external fun destroy(ptr: Long)
    private external fun feedPcm(ptr: Long, pcm: ShortArray, length: Int)
    private external fun finalizeStream(ptr: Long): String
    private var ptr: Long = 0L
    fun start() { ptr = create(modelPath, sampleRate) }
    fun feed(frame: ShortArray) { feedPcm(ptr, frame, frame.size) }
    fun finish(): String = finalizeStream(ptr)
    fun close() { if (ptr != 0L) { destroy(ptr); ptr = 0L } }
}
```

Ensure the app builds for `arm64-v8a`.
