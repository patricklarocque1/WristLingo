package com.wristlingo.wear

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.wear.compose.material.MaterialTheme
import com.wristlingo.core.transport.WearMessageClientDl
import com.wristlingo.wear.asr.AsrController
import com.wristlingo.wear.ui.WearApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class MainActivity : Activity() {
    private lateinit var composeView: ComposeView
    private lateinit var asr: AsrController
    private lateinit var dl: WearMessageClientDl
    private var unregisterDl: (() -> Unit)? = null
    private var tts: TextToSpeech? = null

    private val scope = CoroutineScope(Dispatchers.Main)
    private val seqGen = AtomicLong(1L)

    // UI state
    private var partialText: String? by mutableStateOf(null)
    private var captionText: String? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asr = AsrController(this)
        dl = WearMessageClientDl(applicationContext)

        composeView = ComposeView(this)
        composeView.setContent {
            MaterialTheme {
                WearApp(
                    activity = this,
                    onPttStart = { startAsr() },
                    onPttStop = { stopAsr() },
                    partialText = partialText,
                    captionText = captionText
                )
            }
        }
        setContentView(composeView)

        // Collect ASR partials/finals
        scope.launch {
            asr.partial.collectLatest { text ->
                partialText = text
            }
        }
        scope.launch(Dispatchers.IO) {
            asr.finalText.collectLatest { text ->
                sendUtterance(text)
                // Clear local partial after sending final
                launch(Dispatchers.Main) { partialText = null }
            }
        }

        // Listen for caption updates from phone
        unregisterDl = dl.addListener { topic, payload ->
            if (topic == "caption/update") {
                try {
                    val obj = JSONObject(payload)
                    val text = obj.optString("text")
                    scope.launch(Dispatchers.Main) {
                        captionText = text
                    }
                } catch (_: Throwable) {}
            } else if (topic == "tts/speak") {
                try {
                    val obj = JSONObject(payload)
                    val text = obj.optString("text")
                    val lang = obj.optString("lang")
                    scope.launch(Dispatchers.Main) { speakLocal(text, lang) }
                } catch (_: Throwable) {}
            }
        }
    }

    private fun startAsr() {
        val localeTag = try { Locale.getDefault().toLanguageTag() } catch (_: Throwable) { null }
        asr.start(localeTag)
    }

    private fun stopAsr() {
        asr.stop()
    }

    private fun sendUtterance(text: String) {
        val obj = JSONObject()
            .put("seq", seqGen.getAndIncrement())
            .put("text", text)
        val srcLang = try { Locale.getDefault().toLanguageTag() } catch (_: Throwable) { null }
        if (!srcLang.isNullOrEmpty()) obj.put("srcLang", srcLang)
        val json = obj.toString()
        scope.launch(Dispatchers.IO) {
            try { dl.send("utterance/text", json) } catch (_: Throwable) {}
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        asr.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterDl?.invoke() } catch (_: Throwable) {}
        asr.release()
        try { tts?.stop() } catch (_: Throwable) {}
        try { tts?.shutdown() } catch (_: Throwable) {}
        scope.cancel()
    }

    private fun ensureTts(): TextToSpeech {
        val existing = tts
        if (existing != null) return existing
        val created = TextToSpeech(this) { /* ignore status */ }
        tts = created
        return created
    }

    private fun speakLocal(text: String, languageTag: String?) {
        val engine = ensureTts()
        val locale = try {
            if (!languageTag.isNullOrEmpty()) Locale.forLanguageTag(languageTag) else Locale.getDefault()
        } catch (_: Throwable) { Locale.getDefault() }
        try { engine.language = locale } catch (_: Throwable) {}
        try { engine.setPitch(1.0f) } catch (_: Throwable) {}
        try { engine.setSpeechRate(1.0f) } catch (_: Throwable) {}
        try { engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wristlingo-tts-watch") } catch (_: Throwable) {}
    }
}

