# WristLingo (Android + Wear OS)

Hands‑free, on‑the‑go translation. The Wear OS app captures nearby speech and streams it to the phone. The phone performs on‑device ASR (speech‑to‑text) and on‑device translation by default, then streams live captions + short TTS back to the watch. Sessions are saved for review even if the apps aren’t foregrounded.

## Core goals

- **Watch‑first capture**: push‑to‑talk and quick phrases on Wear OS; stream small PCM frames to phone via the Android Data Layer.
- **Phone processing**: foreground service runs **on‑device** ASR (Whisper via `whisper.cpp` JNI or Android SpeechRecognizer) + **on‑device** translation (ML Kit). Optional cloud fallbacks (Google Cloud Translation; Cloud STT v2) are opt‑in.
- **Review later**: timestamped logs in Room DB with search/export; optional audio snippets.
- **Privacy**: explicit mic indicators; on‑device by default; cloud usage is transparent + disabled by default.

## Modules

- `:core` — Pure Kotlin contracts and utilities (no Android deps): `DlClient`, `Settings`, `TtsHelper`, audio header/codec helpers
- `:app` — Android phone app (Compose): translation, session storage (Room), Whisper JNI bridge, diagnostics, model manager
- `:wear` — Wear OS app (Compose for Wear): capture UI, SpeechRecognizer ASR, PCM streaming, tiles

## Min targets

- Phone: Android 8.0 (API 26+)
- Watch: Wear OS 4+ (target current)

## Build flavors (cost‑aware)

- `offline`  — Whisper + ML Kit only
- `hybrid`   — System SpeechRecognizer/Whisper + Cloud Translation (toggle)
- `cloudstt` — Enables Cloud STT v2 option (explicit opt‑in)

## Providers & contracts

- **Data layer**: `DlClient` contract in `:core` with platform impls `WearMessageClientDl` in `:app`/`:wear`
- **ASR**:
  - Watch: `AsrController` (SpeechRecognizer‑based) emits partial/final text
  - Phone: `WhisperAsrController` (JNI to `whisper.cpp`) consumes PCM frames and emits partial/final captions
  - Cloud STT (flag only) is opt‑in and not enabled by default
- **Translation**: `TranslationProvider` uses ML Kit by default; cloud translation is opt‑in and disabled by default
- **Settings**: `Settings` contract persisted via SharedPreferences on device
- **TTS**: `TtsHelper` contract with phone/watch routing (speak on watch when connected)

## IDE Setup

For detailed setup instructions across different IDEs (VS Code, IntelliJ IDEA, Android Studio, Eclipse), see **[WORKSPACE_SETUP.md](./WORKSPACE_SETUP.md)**.

The project includes pre-configured workspace files for:

- **VS Code**: `WristLingo.code-workspace` with build tasks and debugging
- **IntelliJ IDEA/Android Studio**: `.idea/` configuration files  
- **Eclipse**: `.project` and `.classpath` files

## Quickstart

```bash
# Build with Android Studio (Gradle wrapper not included in repo)
# Or add a Gradle wrapper locally and run:
# ./gradlew :app:assembleOfflineDebug :wear:assembleOfflineDebug
```

## Local build notes (API 36)

Android Gradle Plugin targets API 36 here. Ensure you have the following installed via Android SDK Manager:

- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0

In Android Studio:

- Open Settings/Preferences → Appearance & Behavior → System Settings → Android SDK
- SDK Platforms tab → check “Android 14 (UpsideDownCake) Extension Level 7 / 36” (or latest 36)
- SDK Tools tab → check “Android SDK Build-Tools 36.0.0”

If building from command line, set ANDROID_HOME or use the SDK bundled with Android Studio. Example wrapper build:

```bash
./gradlew :app:assembleOfflineDebug :wear:assembleOfflineDebug
```

## Optional Cloud (off by default)

- **Translation**: Google Cloud Translation (free tier generous; toggle in Settings).
- **Speech**: Cloud STT v2 ($/min; toggle in Settings).
Add keys as Gradle properties or environment variables only when testing cloud flows.

## Features (current)

- **Live caption overlay** on phone with high‑contrast text and fade transitions
- **Wear UI polish**: ring mic button with gentle pulse, caption ticker, connection banner, Translate Tile with LIVE badge
- **VAD & partial smoothing**: energy‑based RMS VAD on phone; partial caption throttling/windowing
- **Whisper model manager**: download/resume/remove with SHA‑256 verification; model path persisted in Settings
- **Session timeline**: chat‑like view with sticky day headers; export to `.jsonl`; developer import from `.jsonl`
- **Diagnostics panel (hidden)**: Data Layer status, recent messages, ASR/VAD metrics
- **Quick Settings tile (phone)**: jump directly to the Live overlay
- **Flow stability**: buffers sized for partials/events; lifecycle‑aware collectors; `close()` on controllers for tests

## Permissions & foreground service

- RECORD_AUDIO, FOREGROUND_SERVICE_MICROPHONE, WAKE_LOCK, INTERNET (only for cloud), POST_NOTIFICATIONS (Android 13+).
- ForegroundService shows a persistent notification while capturing/processing.

## Build flavors & helper tasks

- Flavors: `offline`, `hybrid`, `cloudstt` (cloud toggles are disabled by default)
- Helper tasks:
  - `./gradlew :app:printStatus` — list flavors and default toggle values
  - `./gradlew :wear:checkAbi` — print Wear ABI split status
  - `./gradlew :app:verify16k` — check JNI 16KB page alignment and APK zipalign

## License

MIT (change as you like).
