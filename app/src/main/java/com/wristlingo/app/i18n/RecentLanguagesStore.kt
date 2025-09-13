package com.wristlingo.app.i18n

import android.content.Context
import android.content.SharedPreferences

class RecentLanguagesStore(context: Context) {
	private val prefs: SharedPreferences = context.getSharedPreferences("recent_langs", Context.MODE_PRIVATE)

	fun get(): List<String> {
		val csv = prefs.getString(KEY, "") ?: ""
		return csv.split(',').mapNotNull { it.ifBlank { null } }
	}

	fun add(code: String, maxSize: Int = 5) {
		val current = get().toMutableList()
		current.remove(code)
		current.add(0, code)
		while (current.size > maxSize) current.removeLast()
		prefs.edit().putString(KEY, current.joinToString(",")).apply()
	}

	companion object {
		private const val KEY = "recent"
	}
}


