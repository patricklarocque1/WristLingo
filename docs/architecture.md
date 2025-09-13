# Architecture

## Modules
- `:core` (JVM-only): contracts and utilities
  - Contracts: `DlClient`, `Settings`, `TtsHelper`
  - Utilities: `AudioHeader`, `AudioCodec` (headered PCM encode/decode)
- `:app` (Android): translation pipeline, persistence, diagnostics, Whisper JNI
- `:wear` (Wear OS): capture UI/UX, on-watch ASR (SpeechRecognizer), tiles

## End-to-end pipeline
1. User presses and holds the ring mic on Wear. `AsrController` (SpeechRecognizer) emits partials/finals, or `PcmStreamer` streams PCM frames (200–300 ms, 16 kHz/16‑bit) with header `{sr,bits,seq,end}` over `DlClient` topic `audio/pcm`.
2. Phone `TranslatorOrchestrator` receives:
   - `utterance/text`: translates via ML Kit and stores to Room; sends `caption/update` to watch; optional TTS on watch/phone.
   - `audio/pcm`: decodes header+Base64, feeds `WhisperAsrController` (JNI), applies RMS‑based VAD and partial smoothing. Finals get translated and stored. Partials and finals are broadcast to the Live overlay via `LiveCaptionBus`.
3. The Live overlay (phone) shows high‑contrast captions with fade transitions; Wear shows a caption ticker and connection banners.

## Storage
- Room database on phone: `sessions` and `utterances` with timestamps and languages.
- Export to `.jsonl` and share; developer import from `.jsonl` remaps IDs.

## Settings & feature flags
- Settings groups: Translation, Speech, Playback/Whisper, Data, Advanced.
- Flavors: `offline`, `hybrid`, `cloudstt`; cloud toggles disabled by default.
- Whisper model manager: download/resume/remove with SHA‑256 verification; path persisted.

## Diagnostics
- Hidden screen: Data Layer status, recent messages, ASR activity and VAD RMS.
- Developer helpers: `:app:printStatus`, `:wear:checkAbi`, and `:app:verify16k`.

## UI/UX
- Material 3 expressive styling across phone and Wear.
- Phone: Live overlay, Settings, Sessions home and timeline with day headers.
- Wear: Ring mic pulse, caption ticker, connection banner, Translate Tile with "LIVE" badge; QS tile on phone starts Live overlay.

## Whisper branch
- `:app` integrates `whisper.cpp` via CMake; JNI bridge in `whisper_bridge.cpp` and `NativeWhisper`.
- Audio path from Wear uses headered PCM; phone VAD/partials feed captions and translation.
- Arm64‑only native packaging; NDK/CMake pinned via properties.
