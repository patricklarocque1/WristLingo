# Whisper Branch — Phone-based ASR via whisper.cpp (JNI)

This branch moves **ASR (speech-to-text)** from the watch to the **phone** using `whisper.cpp`. 
Your public interfaces stay the same; we add a **WhisperAsrController** on the phone and a new **watch audio→phone** transport.

## High-level plan
1. **Branch**: `feature/whisper-phone-asr`
2. **NDK & CMake**: enable externalNativeBuild in `:app` only.
3. **whisper.cpp**: add as a **git submodule** under `app/src/main/cpp/whisper.cpp` or use `FetchContent` in CMake.
4. **JNI bridge**: expose `start(sampleRate)`, `feed(short[] frame)`, `finalize()` to Kotlin.
5. **Audio transport**: switch watch to capture **PCM 16 kHz mono** and send 200–300 ms frames via Wear **MessageClient** to the phone (`topic: audio/pcm`), using the header `{sr, bits, seq, end}` + base64 payload (or raw shorts if you prefer).
6. **Provider switch**: add a **settings toggle** “Use Whisper on phone”. When on, ignore watch SpeechRecognizer and use watch audio streaming instead.
7. **Models**: runtime download into `app/files/models/whisper/`, e.g., `ggml-base-q5_0.bin`. Use **on-demand download** with checksum; do not commit the models.
8. **CI**: pin NDK + CMake; keep default workflow on **offline flavor**; add a separate workflow for the whisper build matrix (arm64-v8a).

## Architecturally unchanged pieces
- Translation remains **ML Kit** by default; cloud toggles unchanged.
- Data model (Room), TTS, and session logging unchanged.
