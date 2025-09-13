# CI & Build Guidance

## Variants
- Offline build (default): `:app:assembleOfflineDebug` and `:wear:assembleOfflineDebug`
- Whisper build: `:app:externalNativeBuildOfflineDebug` to compile JNI and generate native libraries

## Toolchain pinning
- API levels and Build Tools are centralized via `gradle.properties`.
- NDK pinned via `android.ndkVersion` property and read by `:app`.

## Suggested CI steps
```bash
./gradlew clean \
  -Pandroid.compileSdk=36 \
  -Pandroid.targetSdk=36 \
  -Pandroid.buildToolsVersion=36.0.0 \
  :app:externalNativeBuildOfflineDebug \
  :app:assembleOfflineDebug \
  :wear:assembleOfflineDebug \
  :app:testOfflineDebug --stacktrace --no-daemon --warning-mode all
```

## Helper tasks
- `:app:printStatus` — quick flavor/toggle overview
- `:wear:checkAbi` — ABI split sanity on Wear
- `:app:verify16k` — checks 16 KB page alignment for JNI libs and APK zipalign

## Notes
- Cloud features are disabled by default in CI; do not require API keys.
- Keep artifacts arm64‑only for native; Wear module ships without NDK.
