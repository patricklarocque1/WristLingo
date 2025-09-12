package com.wristlingo.core.tts

/**
 * Pure contract for TTS to keep :core JVM-only. Platform-specific impls live in app/wear.
 */
interface TtsHelper {
    suspend fun speakPreferWatch(text: String, languageTag: String)
}
