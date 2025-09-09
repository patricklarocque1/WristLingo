## CODEX-WEB — W1: Enable NDK/CMake and project layout (app only)

Create branch `feature/whisper-phone-asr`. Update the `:app` module to support externalNativeBuild with CMake.

Changes:
1) **Gradle (app/build.gradle.kts)**:
   - `android.externalNativeBuild.cmake.path = file("src/main/cpp/CMakeLists.txt")`
   - `ndk { abiFilters += listOf("arm64-v8a") }`
   - `packagingOptions { pickFirst("**/*.so") }` (only if needed)
   - Add `jniLibs` packaging if necessary.
2) **Create dirs** in `app/src/main/cpp/`:
   - `CMakeLists.txt` (see W2 for content)
   - `whisper_bridge.cpp` and `whisper_bridge.h` (JNI glue)
   - placeholder `README.md` for build notes
3) **Gradle (project)**: ensure JDK 21, AGP 8.x; no changes to :wear.

Do not add whisper.cpp yet; that comes in W2. Ensure the app still compiles after adding the empty CMake project (build a trivial native lib that returns a version string).
