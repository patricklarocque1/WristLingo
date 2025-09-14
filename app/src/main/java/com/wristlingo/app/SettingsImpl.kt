package com.wristlingo.app

import android.content.Context
import android.content.SharedPreferences
import com.wristlingo.core.settings.Settings

class SettingsImpl(context: Context) : Settings {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wristlingo.settings", Context.MODE_PRIVATE)

    override var defaultTargetLanguage: String
        get() = prefs.getString(KEY_TARGET_LANG, DEFAULT_TARGET_LANG) ?: DEFAULT_TARGET_LANG
        set(value) { prefs.edit().putString(KEY_TARGET_LANG, value).apply() }

    override var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, false)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply() }

    override var useCloudTranslate: Boolean
        get() = prefs.getBoolean(KEY_USE_CLOUD_TRANSLATE, false)
        set(value) { prefs.edit().putBoolean(KEY_USE_CLOUD_TRANSLATE, value).apply() }

    override var useWhisperRemote: Boolean
        get() = prefs.getBoolean(KEY_USE_WHISPER_REMOTE, false)
        set(value) { prefs.edit().putBoolean(KEY_USE_WHISPER_REMOTE, value).apply() }

    override var whisperModelPath: String
        get() = prefs.getString(KEY_WHISPER_MODEL_PATH, "") ?: ""
        set(value) { prefs.edit().putString(KEY_WHISPER_MODEL_PATH, value).apply() }

    override var vadRmsThreshold: Int
        get() = prefs.getInt(KEY_VAD_RMS_THRESHOLD, 1000)
        set(value) { prefs.edit().putInt(KEY_VAD_RMS_THRESHOLD, value).apply() }

    override var vadSilenceMs: Int
        get() = prefs.getInt(KEY_VAD_SILENCE_MS, 800)
        set(value) { prefs.edit().putInt(KEY_VAD_SILENCE_MS, value).apply() }

    override var partialWindowMs: Int
        get() = prefs.getInt(KEY_PARTIAL_WINDOW_MS, 2500)
        set(value) { prefs.edit().putInt(KEY_PARTIAL_WINDOW_MS, value).apply() }

    override var partialThrottleMs: Int
        get() = prefs.getInt(KEY_PARTIAL_THROTTLE_MS, 500)
        set(value) { prefs.edit().putInt(KEY_PARTIAL_THROTTLE_MS, value).apply() }

    override var backlogCapMs: Int
        get() = prefs.getInt(KEY_BACKLOG_CAP_MS, 3000)
        set(value) { prefs.edit().putInt(KEY_BACKLOG_CAP_MS, value).apply() }

    override var logRms: Boolean
        get() = prefs.getBoolean(KEY_LOG_RMS, false)
        set(value) { prefs.edit().putBoolean(KEY_LOG_RMS, value).apply() }

    companion object {
        private const val KEY_TARGET_LANG = "target_lang"
        private const val DEFAULT_TARGET_LANG = "fr"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_USE_CLOUD_TRANSLATE = "use_cloud_translate"
        private const val KEY_USE_WHISPER_REMOTE = "use_whisper_remote"
        private const val KEY_WHISPER_MODEL_PATH = "whisper_model_path"
        private const val KEY_VAD_RMS_THRESHOLD = "vad_rms_threshold"
        private const val KEY_VAD_SILENCE_MS = "vad_silence_ms"
        private const val KEY_PARTIAL_WINDOW_MS = "partial_window_ms"
        private const val KEY_PARTIAL_THROTTLE_MS = "partial_throttle_ms"
        private const val KEY_BACKLOG_CAP_MS = "backlog_cap_ms"
        private const val KEY_LOG_RMS = "log_rms"
    }
}


