package com.wristlingo.core.settings

import android.content.Context
import android.content.SharedPreferences

class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("wristlingo.settings", Context.MODE_PRIVATE)

    var defaultTargetLanguage: String
        get() = prefs.getString(KEY_TARGET_LANG, DEFAULT_TARGET_LANG) ?: DEFAULT_TARGET_LANG
        set(value) { prefs.edit().putString(KEY_TARGET_LANG, value).apply() }

    companion object {
        private const val KEY_TARGET_LANG = "target_lang"
        private const val DEFAULT_TARGET_LANG = "fr"
    }
}

