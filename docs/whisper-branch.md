# Whisper branch: local NDK + CMake setup

These steps install the exact NDK and CMake needed for the native build in `:app`.

## Prereqs
- Android Studio (latest)
- Android SDK already installed (see `local.properties` `sdk.dir`)

## Install NDK 27.0.12077973 and CMake
1. Open Android Studio → SDK Manager → SDK Tools tab
2. Check "Show Package Details"
3. Under NDK (Side by side), select: `27.0.12077973`
4. Under CMake, install the latest available version compatible with your SDK (e.g., 3.22.1+)
5. Apply/OK to download

## Project configuration
- The project declares versions centrally in `gradle.properties`:
  - `android.ndkVersion=27.0.12077973`
- The `:app` module reads this via:
  - `ndkVersion = project.property("android.ndkVersion") as String`
  - `externalNativeBuild.cmake.path = app/src/main/cpp/CMakeLists.txt`
  - `defaultConfig.ndk.abiFilters += ["arm64-v8a"]`

## Build
From Android Studio, run any `:app` variant (e.g., `offlineDebug`).

Alternatively via Gradle wrapper (if you add one locally):
```bash
./gradlew :app:assembleOfflineDebug
```

## Verify JNI linkage
The native library exposes:
- `NativeBridge.nativeVersion(): String` — returns a version string
- `NativeBridge.nativeNoop()` — no‑op for linkage verification

If the build succeeds, `libwristlingo_native.so` is packaged for `arm64-v8a`.

## Add whisper.cpp submodule

Initialize the upstream Whisper C++ engine as a Git submodule under the app's native sources:

```bash
git submodule add https://github.com/ggerganov/whisper.cpp app/src/main/cpp/whisper.cpp
git submodule update --init --recursive
```

Notes:
- Do NOT commit model files (e.g., `.bin` GGML/GGUF weights). Keep them out of the repo.
- Place models outside the repo or download at runtime to app storage. Configure your absolute path when calling the JNI bridge.
- The build integrates the submodule via CMake (`add_subdirectory(whisper.cpp)`), producing `libwhisper` which is linked into `libwhisper_bridge.so`.

## JNI bridge usage (preview)

Wrapper class: `com.wristlingo.app.nativebridge.NativeWhisper`

```kotlin
val whisper = NativeWhisper()
val ok = whisper.start(modelPath = "/absolute/path/to/model.gguf", sampleRate = 16000)
if (ok) {
    whisper.feed(pcmShortArray)
    val text = whisper.finish()
    whisper.close()
}
```

## Runtime model management

On phone, models are managed at `filesDir/models/whisper/` by `WhisperModelManager`.
- `hasModel()` — check presence
- `downloadModel(url, sha256)` — downloads to a temp file, verifies SHA‑256, then atomically moves into place
- `getModelPath()` — returns absolute path or null

Enable “Use Whisper on phone (watch sends audio)” and set the model path or use the manager to download one.

