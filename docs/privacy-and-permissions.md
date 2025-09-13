# Privacy & Permissions

## Data handling
- Default operation is fully on‑device: watch captures, phone transcribes (Whisper/SpeechRecognizer), and translates (ML Kit).
- Cloud features (Translation, STT) are opt‑in and disabled by default.
- No PII beyond user speech is logged. Diagnostics panel shows message metadata only (direction/topic/size), not full contents.
- Sessions are stored locally in Room. Users can export sessions to `.jsonl` and share. Developers can import `.jsonl` for testing.

## Permissions
- Microphone (`RECORD_AUDIO`): requested on watch; rationale UI and link to system Settings if denied/permanently denied.
- Foreground service (phone): notification shown when active.
- Network (`INTERNET`): required only when enabling cloud features.
- Wake lock: used to keep capture/services reliable during active sessions.

## Deletions & control
- Users can remove the Whisper model from storage via the Manage Model card.
- Users can delete sessions from the app (and may clear app storage to remove all data).
- Cloud toggles can be turned off at any time; on‑device remains the default path.
