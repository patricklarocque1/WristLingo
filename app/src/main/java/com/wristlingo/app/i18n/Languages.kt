package com.wristlingo.app.i18n

data class Language(val code: String, val name: String, val flag: String?)

object Languages {
	val all: List<Language> = listOf(
		Language("en", "English", "🇬🇧"),
		Language("fr", "French", "🇫🇷"),
		Language("es", "Spanish", "🇪🇸"),
		Language("de", "German", "🇩🇪"),
		Language("it", "Italian", "🇮🇹"),
		Language("pt", "Portuguese", "🇵🇹"),
		Language("nl", "Dutch", "🇳🇱"),
		Language("sv", "Swedish", "🇸🇪")
	)

	val byCode: Map<String, Language> = all.associateBy { it.code }

	fun nameFor(code: String): String = byCode[code]?.name ?: code
}


