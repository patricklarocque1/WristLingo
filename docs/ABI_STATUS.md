# ABI Status

- Final policy:
  - `:app`: arm64-v8a only. NDK/JNI configured here if needed.
  - `:wear`: no NDK/JNI; ABI splits disabled (works on arm64-only devices).

- Centralized versions respected via `gradle.properties`: compileSdk/targetSdk=36; buildTools=36.0.0; minSdk wear=30, app=26; ndkVersion=27.x used only by `:app`.

## Changes

- wear/build.gradle.kts
  - Disabled ABI splits (`splits { abi { isEnable = false } }`).
  - Added packaging config for JNI libs; no NDK declared.
  - Reads compile/target/min/buildTools from properties.

- app/build.gradle.kts
  - Clarified ABI policy comment: arm64-v8a only.
  - Keeps `ndk { abiFilters += ["arm64-v8a"] }` in `defaultConfig`.

- whisper.cpp example projects (vendored examples only; not part of modules):
  - app/src/main/cpp/whisper.cpp/examples/whisper.android/lib/build.gradle
    - Restricted `ndk.abiFilters` to `arm64-v8a`.
    - Removed explicit `ndkVersion` line (keeps example self-contained).
  - app/src/main/cpp/whisper.cpp/examples/whisper.android.java/app/build.gradle
    - Restricted `ndk.abiFilters` to `arm64-v8a`.
    - Removed explicit `ndkVersion` line.
  - app/src/main/cpp/whisper.cpp/examples/.../whisper/CMakeLists.txt (two locations)
    - Removed `armeabi-v7a` branches; keep arm64-specific branch only.
  - Example Kotlin/Java detection helpers now ignore v7a paths.

## Historical references (purged)

- Prior references to `armeabi` or `armeabi-v7a` were limited to vendored whisper.cpp example build files and CMake scripts. These are now normalized to `arm64-v8a` only to avoid toolchain noise.

## Verification

- Build Wear only:
  - `./gradlew clean :wear:assembleOfflineDebug`
- Full build (defaults to offline flavor):
  - `./gradlew clean :app:externalNativeBuildOfflineDebug :app:assembleOfflineDebug :wear:assembleOfflineDebug`

