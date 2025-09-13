# Acceptance Criteria

## N-series
- N1 Flows: Partials use `replay=1` and `extraBufferCapacity=64`; finals/events use `replay=0` and `extraBufferCapacity=16`. Lifecycle collectors in Compose.
- N2–N6 UI: Settings grouped; searchable language picker with recents/flags; Live overlay uses high‑contrast typography with fade transitions; Sessions list uses expressive cards; Session detail timeline with day headers; Wear ring mic pulses; caption ticker; connection banners; Wear Translate tile with LIVE badge.
- N7 VAD & partials: Energy‑based RMS VAD reduces latency; partials throttled and smoothed; silence auto‑finalizes.
- N8 Whisper manager: Download/resume/remove model with SHA‑256 verification; persistent model path; helpful toasts when missing.
- N9 Export/Import: Sessions export as `.jsonl` and share; developer import inserts with remapped IDs.
- N10 Diagnostics: Hidden panel shows DL status, recent messages, ASR activity, VAD RMS.
- N11 UX: Mic permission rationale with Settings link; error toasts for missing model; compact error row on DL send failures.
- N12 Dev helpers: `:app:printStatus`, `:wear:checkAbi` available and non-invasive.

## W-series
- W1–W2 NDK toggle and JNI bridge included only in `:app`, arm64‑v8a; build respects centralized properties.
- W3 Audio streaming: Wear sends headered PCM or text (not both); phone decodes and feeds Whisper.
- W4 VAD & partials on Whisper path: partials/auto‑finalization implemented; phone overlay reflects updates.
- W5 Model manager: runtime download with resume and checksum; path persisted.
- W6 CI (optional): offline CI build succeeds; whisper CI build can be enabled; `verify16k` checks JNI alignment.
