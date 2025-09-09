package com.wristlingo.core.settings

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wristlingo.settings", Context.MODE_PRIVATE)

    var defaultTargetLanguage: String
        get() = prefs.getString(KEY_TARGET_LANG, DEFAULT_TARGET_LANG) ?: DEFAULT_TARGET_LANG
        set(value) { prefs.edit().putString(KEY_TARGET_LANG, value).apply() }

    var autoSpeak: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SPEAK, false)
        set(value) { prefs.edit().putBoolean(KEY_AUTO_SPEAK, value).apply() }

    var useCloudTranslate: Boolean
        get() = prefs.getBoolean(KEY_USE_CLOUD_TRANSLATE, false)
        set(value) { prefs.edit().putBoolean(KEY_USE_CLOUD_TRANSLATE, value).apply() }

    companion object {
        private const val KEY_TARGET_LANG = "target_lang"
        private const val DEFAULT_TARGET_LANG = "fr"
        private const val KEY_AUTO_SPEAK = "auto_speak"
        private const val KEY_USE_CLOUD_TRANSLATE = "use_cloud_translate"
    }
}

