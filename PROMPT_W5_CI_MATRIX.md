## CODEX-WEB — W5: CI + Build matrix for whisper branch

- Update Android CI to install NDK and CMake (e.g., `ndk;26.1.10909125`, `cmake;3.22.1`).
- For branch `feature/whisper-phone-asr`, add a job that runs:
  `./gradlew :app:assembleOfflineDebug :app:externalNativeBuildOfflineDebug`
- Keep main workflow unchanged (no NDK on main). The whisper job only runs on this branch or when `ENABLE_WHISPER_CI=1`.
- Add a doc note about model download being **runtime-only** (no large assets in repo).
