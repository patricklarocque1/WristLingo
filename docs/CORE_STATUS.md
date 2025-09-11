# Core Module Status

## Module Type
- JVM library (Kotlin JVM, toolchain 21).

## Changes Applied
- Converted `:core` from Android library to JVM-only.
- Replaced Android-specific classes with interfaces in `:core`:
  - `com.wristlingo.core.settings.Settings` is now an interface.
  - `com.wristlingo.core.tts.TtsHelper` is now an interface.
  - `com.wristlingo.core.transport.WearMessageClientDl` removed (left as deprecated stub), platform impls added in `:app` and `:wear`.
- Moved Android-specific implementations:
  - Phone `WearMessageClientDl` → `app/src/main/java/com/wristlingo/app/transport/WearMessageClientDl.kt`
  - Wear `WearMessageClientDl` → `wear/src/main/java/com/wristlingo/wear/transport/WearMessageClientDl.kt`
  - Text-to-speech impl → `app/src/main/java/com/wristlingo/app/transport/AppTtsHelper.kt`
  - SharedPreferences-backed settings → `app/src/main/java/com/wristlingo/app/SettingsImpl.kt`
- Added pure utilities to `:core`:
  - `com.wristlingo.core.audio.AudioUtils.kt` with `AudioHeader` and `AudioCodec`.
  - JVM tests: `core/src/test/java/com/wristlingo/core/audio/AudioCodecTest.kt`.

## Remaining Android Imports in :core
- none

## Dependency Edges
- `:app` → `:core` ✓
- `:wear` → `:core` ✓
- `:core` → (no app/wear) ✓

## Centralized Versions
- `:app` and `:wear` read `compileSdk`, `targetSdk`, `minSdk`/`wearMinSdk`, `buildToolsVersion`, `ndkVersion` from `gradle.properties` per instructions.

## Settings UI Adjustments
- Applied system bars insets and long-label clipping guards in `app/src/main/java/com/wristlingo/app/ui/SettingsScreen.kt`.

## TODO / Notes
- Public API behavior preserved; any behavior change risk: minimal. If further platform-specific settings are needed on wear, create a `SettingsImpl` in `:wear` mirroring `:app`.
- If serialization version for `AudioCodec` needs to be stable across devices, consider explicit `Json` config and schema tests.


