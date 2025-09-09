package com.wristlingo.app.transport

import android.content.Context
import com.wristlingo.app.data.SessionRepository
import com.wristlingo.core.transport.DlClient
import com.wristlingo.core.transport.WearMessageClientDl
import com.wristlingo.core.tts.TtsHelper
import com.wristlingo.core.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

class TranslatorOrchestrator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dl: DlClient,
    private val translationProvider: TranslationProvider,
    private val repository: SessionRepository
) {
    private var unregister: (() -> Unit)? = null
    private var activeSessionId: Long? = null
    private var activeTargetLang: String? = null
    private val settings = Settings(context)
    private val tts = if (dl is WearMessageClientDl) TtsHelper(context, dl) else TtsHelper(context, null)

    fun start() {
        unregister = dl.setListener { topic, payload ->
            if (topic == "utterance/text") {
                scope.launch(Dispatchers.IO) {
                    handleUtterance(payload)
                }
            }
        }
    }

    fun stop() {
        unregister?.invoke()
        unregister = null
    }

    private suspend fun handleUtterance(payload: String) {
        val obj = JSONObject(payload)
        val seq = obj.optLong("seq")
        val text = obj.optString("text")
        val src = obj.optString("srcLang", null)
        val tgt = translationProvider.defaultTarget()

        // Ensure an active session exists for the current target language
        val sessionId = ensureSession(tgt)
        val translated = try {
            translationProvider.translate(text, src, tgt)
        } catch (_: Throwable) {
            text
        }
        // Persist utterance
        try {
            repository.insertUtterance(
                sessionId = sessionId,
                timestampEpochMs = System.currentTimeMillis(),
                srcText = text,
                dstText = translated,
                srcLang = src,
                dstLang = tgt
            )
        } catch (_: Throwable) {
        }
        val out = JSONObject()
            .put("seq", seq)
            .put("text", translated)
            .put("dstLang", tgt)
        dlSend("caption/update", out.toString())

        if (settings.autoSpeak) {
            scope.launch(Dispatchers.Main) {
                try { tts.speakPreferWatch(translated, tgt) } catch (_: Throwable) {}
            }
        }
    }

    private suspend fun ensureSession(targetLang: String): Long {
        val currentId = activeSessionId
        if (currentId != null && activeTargetLang == targetLang) return currentId
        val createdId = try {
            repository.createSession(targetLang)
        } catch (_: Throwable) {
            // If creation fails, reuse existing id if any or fallback to creating again next time
            currentId ?: 0L
        }
        if (createdId != 0L) {
            activeSessionId = createdId
            activeTargetLang = targetLang
        }
        return activeSessionId ?: createdId
    }

    private fun dlSend(topic: String, payload: String) {
        scope.launch(Dispatchers.IO) {
            try { dl.send(topic, payload) } catch (_: Throwable) {}
        }
    }
}

