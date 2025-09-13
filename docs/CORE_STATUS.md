# Core Module Status

## Module Type
- JVM library (Kotlin JVM, toolchain 21).

## Summary of Current State
- `:core` is JVM-only and hosts pure contracts:
  - `Settings`, `TtsHelper`, `DlClient`
  - Audio helpers: `AudioHeader`, `AudioCodec` for PCM header encoding/decoding (Base64 JSON)
- Platform implementations live in `:app` and `:wear`.

### Implementations in platform modules
- Phone:
  - `WearMessageClientDl` (Data Layer), `AppTtsHelper` (TTS), `SettingsImpl` (SharedPreferences)
  - `WhisperAsrController` JNI wrapper; `TranslationProvider` using ML Kit
- Wear:
  - `WearMessageClientDl`, `AsrController` (SpeechRecognizer)

## Remaining Android Imports in :core
- none

## Dependency Edges
- `:app` → `:core` ✓
- `:wear` → `:core` ✓
- `:core` → (no app/wear) ✓

## Centralized Versions
- `:app` and `:wear` read `compileSdk`, `targetSdk`, `minSdk`/`wearMinSdk`, `buildToolsVersion`, `ndkVersion` from `gradle.properties` per instructions.

## UI/UX Hooks in Phone App
- Settings screen groups: Translation, Speech, Playback/Whisper, Data, Advanced
- Manage Whisper model card; checksum verification; persisted model path

## Notes
- Contracts are stable; behavior remains backward compatible
- AudioCodec has unit tests; keep serialization stable across devices


