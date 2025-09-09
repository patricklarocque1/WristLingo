package com.wristlingo.app.transport

import android.content.Context
import com.wristlingo.core.transport.DlClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

class TranslatorOrchestrator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dl: DlClient,
    private val translationProvider: TranslationProvider
) {
    private var unregister: (() -> Unit)? = null

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
        val translated = try {
            translationProvider.translate(text, src, tgt)
        } catch (_: Throwable) {
            text
        }
        val out = JSONObject()
            .put("seq", seq)
            .put("text", translated)
            .put("dstLang", tgt)
        dlSend("caption/update", out.toString())
    }

    private fun dlSend(topic: String, payload: String) {
        scope.launch(Dispatchers.IO) {
            try { dl.send(topic, payload) } catch (_: Throwable) {}
        }
    }
}

