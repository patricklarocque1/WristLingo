# Testing

## Unit & JVM tests
- **Flow buffers**: ensure partial/event `MutableSharedFlow` configurations do not throw on bursts:
  - Partials: `replay=1`, `extraBufferCapacity=64`, `DROP_OLDEST`
  - Events: `replay=0`, `extraBufferCapacity=16`, `DROP_OLDEST`
- **Audio codec**: `AudioCodecTest` validates headered PCM encode/decode roundtrips and failure on corrupt payloads.
- **VAD**: `VadUtilsTest` checks RMS computation, threshold, and silence finalization.
- **Session export/import**: `JsonlExportImportTest` roundtrips meta and utterances.

## Android tests (component)
- **Message roundtrip (watch→phone)**: validate `DlClient` topic handling for `utterance/text` and `audio/pcm`.
- **Whisper JNI smoke**: verify `NativeWhisper` loads and `start/partial/finish` behave under small PCM input.

## Test infrastructure
- `MainDispatcherRule` for coroutine tests, with proper cleanup.
- Controllers expose `close()` to cancel scopes/jobs deterministically in tests.

## Guidelines
- Use `collectAsStateWithLifecycle` and `repeatOnLifecycle` to avoid duplicate collectors.
- Prefer `tryEmit/trySend` in tests to avoid suspensions/flakiness.
