package com.wristlingo.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import com.wristlingo.core.transport.WearMessageClientDl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

class TtsHelper(
    private val context: Context,
    private val dl: WearMessageClientDl? = null
) {
    @Volatile
    private var tts: TextToSpeech? = null

    suspend fun speakPreferWatch(text: String, languageTag: String) {
        val useWatch = try {
            dl?.hasConnectedNode() == true
        } catch (_: Throwable) {
            false
        }
        if (useWatch && dl != null) {
            sendSpeakToWatch(text, languageTag)
        } else {
            speakLocal(text, languageTag)
        }
    }

    private suspend fun sendSpeakToWatch(text: String, languageTag: String) {
        val payload = JSONObject()
            .put("text", text)
            .put("lang", languageTag)
            .toString()
        try {
            withContext(Dispatchers.IO) {
                dl?.send("tts/speak", payload)
            }
        } catch (_: Throwable) {}
    }

    suspend fun speakLocal(text: String, languageTag: String) {
        withContext(Dispatchers.Main) {
            val engine = ensureTts()
            val locale = try {
                Locale.forLanguageTag(languageTag)
            } catch (_: Throwable) {
                Locale.getDefault()
            }
            try { engine.language = locale } catch (_: Throwable) {}
            try { engine.setPitch(1.0f) } catch (_: Throwable) {}
            try { engine.setSpeechRate(1.0f) } catch (_: Throwable) {}
            try { engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wristlingo-tts") } catch (_: Throwable) {}
        }
    }

    private fun ensureTts(): TextToSpeech {
        val existing = tts
        if (existing != null) return existing
        val created = TextToSpeech(context) { /* ignore init status; we'll try speak anyway */ }
        tts = created
        return created
    }

    fun release() {
        try { tts?.stop() } catch (_: Throwable) {}
        try { tts?.shutdown() } catch (_: Throwable) {}
        tts = null
    }
}

