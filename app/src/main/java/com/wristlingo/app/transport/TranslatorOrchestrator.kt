package com.wristlingo.app.transport

import android.content.Context
import com.wristlingo.app.data.SessionRepository
import com.wristlingo.core.transport.DlClient
import com.wristlingo.app.transport.WearMessageClientDl
import com.wristlingo.core.tts.TtsHelper
import com.wristlingo.core.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import android.util.Base64
import com.wristlingo.app.asr.WhisperAsrController
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TranslatorOrchestrator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dl: DlClient,
    private val translationProvider: TranslationProvider,
    private val repository: SessionRepository
) {
    private var whisper: WhisperAsrController? = null
    private var pcmSeqExpected: Long = 0L
    private var whisperSr: Int = 16000
    private var energyAvg: Double = 0.0
    private var lastVoiceMs: Long = 0L
    private var lastPartialMs: Long = 0L
    private var queuedFramesBytes: Int = 0
    private val maxQueuedBytes: Int = 16000 * 2 * 3 // ~3 seconds at 16kHz 16-bit
    private var unregister: (() -> Unit)? = null
    private var activeSessionId: Long? = null
    private var activeTargetLang: String? = null
    private val settings: Settings = com.wristlingo.app.SettingsImpl(context)
    private val tts = AppTtsHelper(context, dl as? WearMessageClientDl)

    fun start() {
        unregister = dl.addListener { topic, payload ->
            if (topic == "utterance/text") {
                scope.launch(Dispatchers.IO) {
                    handleUtterance(payload)
                }
            } else if (topic == "audio/pcm") {
                scope.launch(Dispatchers.IO) {
                    handlePcm(payload)
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
        val src = if (obj.has("srcLang")) obj.optString("srcLang") else null
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

    private fun handlePcm(payload: String) {
        if (!settings.useWhisperRemote) return
        try {
            val obj = JSONObject(payload)
            val header = obj.getJSONObject("header")
            val sr = header.optInt("sr", 16000)
            val bits = header.optInt("bits", 16)
            val seq = header.optLong("seq", 0L)
            val isEnd = header.optBoolean("end", false)
            val b64 = obj.getString("pcm")
            if (bits != 16) return

            if (whisper == null || whisperSr != sr) {
                whisper?.close()
                val mgr = com.wristlingo.app.asr.WhisperModelManager(context)
                whisper = WhisperAsrController { mgr.getModelPath() }
                if (!whisper!!.start(sr)) {
                    whisper = null
                    return
                }
                whisperSr = sr
                pcmSeqExpected = 0L
            }

            // simple sequence guard
            if (seq != pcmSeqExpected) {
                pcmSeqExpected = seq
            }
            pcmSeqExpected++

            val data = Base64.decode(b64, Base64.DEFAULT)
            // Coalesce if backlog grows: cap to last N bytes
            queuedFramesBytes += data.size
            if (queuedFramesBytes > maxQueuedBytes) {
                queuedFramesBytes = maxQueuedBytes
            }
            val samples = ShortArray(data.size / 2)
            var idx = 0
            var i = 0
            var energySum = 0.0
            while (i + 1 < data.size) {
                val lo = (data[i].toInt() and 0xFF)
                val hi = data[i + 1].toInt()
                val s = ((hi shl 8) or lo).toShort()
                samples[idx++] = s
                energySum += (s.toInt() * s.toInt()).toDouble()
                i += 2
            }
            whisper?.feed(samples)

            // Simple energy-based VAD over this frame
            val frameSamples = max(1, samples.size)
            val rms = kotlin.math.sqrt(energySum / frameSamples)
            val now = System.currentTimeMillis()
            val threshold = settings.vadRmsThreshold.toDouble()
            if (settings.logRms) {
                android.util.Log.d("WhisperVAD", "rms=${'$'}rms threshold=${'$'}threshold")
            }
            if (rms > threshold) {
                lastVoiceMs = now
            }
            // Throttle partial captions using configured window
            val throttleMs = max(100, settings.partialThrottleMs)
            val windowMs = max(500, settings.partialWindowMs)
            if (!isEnd && now - lastPartialMs >= throttleMs) {
                lastPartialMs = now
                val part = try { whisper?.partial(windowMs) } catch (_: Throwable) { null }
                if (!part.isNullOrBlank()) {
                    val out = JSONObject()
                        .put("seq", seq)
                        .put("text", part)
                    dlSend("caption/update", out.toString())
                }
            }

            if (isEnd) {
                val text = whisper?.finalizeStream().orEmpty()
                if (text.isNotEmpty()) {
                    scope.launch(Dispatchers.IO) {
                        handleUtterance(
                            JSONObject().put("seq", seq).put("text", text).toString()
                        )
                    }
                }
                whisper?.close()
                whisper = null
                energyAvg = 0.0
                queuedFramesBytes = 0
            } else {
                // Auto-finalize if silent for 800ms after voice
                val silenceMs = max(200, settings.vadSilenceMs)
                if (lastVoiceMs > 0L && now - lastVoiceMs >= silenceMs) {
                    val text = whisper?.finalizeStream().orEmpty()
                    if (text.isNotEmpty()) {
                        scope.launch(Dispatchers.IO) {
                            handleUtterance(
                                JSONObject().put("seq", seq).put("text", text).toString()
                            )
                        }
                    }
                    whisper?.close()
                    whisper = null
                    energyAvg = 0.0
                    queuedFramesBytes = 0
                }
            }
        } catch (_: Throwable) { }
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

