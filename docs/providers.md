# Providers & Contracts

## Contracts (in `:core`)
- `DlClient`: duplex message client for small JSON messages over topics
- `Settings`: runtime preferences (target language, Whisper, VAD tuning)
- `TtsHelper`: abstraction for TTS playback (phone vs. watch)

## Implementations

### Data Layer
- Phone: `com.wristlingo.app.transport.WearMessageClientDl`
- Wear:  `com.wristlingo.wear.transport.WearMessageClientDl`

### ASR
- Wear: `com.wristlingo.wear.asr.AsrController` (Android `SpeechRecognizer`) emits partial and final text.
- Phone: `com.wristlingo.app.asr.WhisperAsrController` (JNI to `whisper.cpp`) consumes PCM frames and provides partial/final text.
- Cloud STT: off by default; guarded by flavor/toggles when present.

### Translation
- Phone: `com.wristlingo.app.transport.TranslationProvider` (ML Kit) downloads models per language as needed; optional cloud translation disabled by default.

### Settings
- Phone: `com.wristlingo.app.SettingsImpl` (SharedPreferences)
- Wear: (if needed) mirror with a local `SettingsImpl` in `:wear`.

### TTS
- Phone: `com.wristlingo.app.transport.AppTtsHelper` routes to watch when connected via Data Layer, otherwise uses local `TextToSpeech`.

## Feature Flags & Flavors
- Flavors: `offline`, `hybrid`, `cloudstt`.
- BuildConfig toggles: `USE_CLOUD_TRANSLATE`, `USE_CLOUD_STT` (defined per flavor).
- Settings toggles: `useCloudTranslate`, `useWhisperRemote`, `autoSpeak`, plus VAD tuning.

## Message Topics
- `utterance/text`: watch → phone text (JSON: `{seq,text,srcLang?}`)
- `caption/update`: phone → watch caption updates (JSON: `{seq?,text,dstLang?}`)
- `audio/pcm`: watch → phone PCM frames with header `{sr,bits,seq,end}` and Base64 `pcm` payload
- `tts/speak`: phone → watch TTS text (JSON: `{text,lang}`)
