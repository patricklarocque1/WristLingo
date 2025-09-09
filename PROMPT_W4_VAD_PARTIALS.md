## CODEX-WEB — W4: Performance, VAD, and partials

Refine streaming behavior:
- Add a simple energy-based VAD on the phone to auto-segment if user forgets to release PTT.
- Emit partial captions every ~500 ms by running `whisper_full()` on the last 2–3 seconds (overlapping window).
- Throttle UI updates to avoid flooding.
- Ensure backpressure: if frames queue grows, coalesce them.
