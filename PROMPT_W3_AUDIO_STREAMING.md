## CODEX-WEB — W3: WhisperAsrController + Watch audio streaming

Wire phone-side Whisper ASR and change the watch to stream audio instead of text when a new toggle is ON.

1) **AsrController (phone)**: implement `WhisperAsrController` that:
   - On `start(sr)`: create `NativeWhisper` with modelPath + sr.
   - On `feed(frame)`: append to native buffer.
   - On `stop/finalize`: call `finish()` to get final text; emit via `finals` Flow.
   - Optionally, every N frames (e.g., 1 sec), run a lightweight interim decode for partials.

2) **Model manager** (phone):
   - `WhisperModelManager`: check presence of `filesDir/models/whisper/ggml-base-q5_0.bin` (or user-selected).
   - If absent, show a dialog to download; verify checksum; store path in prefs.
   - Expose `getModelPathOrNull()` to controller.

3) **Watch audio capture**:
   - Use `AudioRecord` with 16 kHz mono, 16-bit PCM, buffer for 200–300 ms frames.
   - On PTT, stream `AudioFrame` to phone over Wear **MessageClient** under topic `audio/pcm`.
   - Frame payload = small JSON header `{sr,bits,seq,end}` + base64 PCM or raw shorts.

4) **Phone receiver**:
   - When `audio/pcm` arrives, feed into `WhisperAsrController`.
   - When `end=true`, call finalize; send `caption/update` back to watch, persist to Room, and translate via ML Kit as before.

5) **Settings**:
   - Add toggle: “Use Whisper on phone (watch sends audio)”. If ON, disable watch SpeechRecognizer UI and switch to PTT audio streaming.
