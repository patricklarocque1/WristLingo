package com.wristlingo.wear

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.platform.ComposeView
import androidx.wear.compose.material.MaterialTheme
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wristlingo.core.transport.WearMessageClientDl
import com.wristlingo.core.tts.TtsHelper
import com.wristlingo.wear.asr.AsrController
import com.wristlingo.wear.ui.WearApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var asr: AsrController
    private lateinit var dl: WearMessageClientDl
    private val scope = CoroutineScope(Dispatchers.Main)
    private var seq: Long = 1L

    private var captionText: String? = null
    private var partialText: String? = null
    private lateinit var tts: TtsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureRecordAudioPermission()
        asr = AsrController(this)
        dl = WearMessageClientDl(this, CoroutineScope(Dispatchers.IO))
        tts = TtsHelper(this, null)

        // Listen for captions and TTS requests from phone
        dl.setListener { topic, payload ->
            if (topic == "caption/update") {
                val obj = JSONObject(payload)
                val text = obj.optString("text")
                captionText = text
                runOnUiThread { composeView.setContent(content) }
            } else if (topic == "tts/speak") {
                val obj = JSONObject(payload)
                val text = obj.optString("text")
                val lang = obj.optString("lang")
                scope.launch { tts.speakLocal(text, lang) }
            }
        }

        composeView.setContent(content)
        setContentView(composeView)

        scope.launch {
            asr.partial.collectLatest {
                partialText = it
                runOnUiThread { composeView.setContent(content) }
            }
        }
        scope.launch {
            asr.finalText.collectLatest { text ->
                val payload = JSONObject()
                    .put("seq", seq++)
                    .put("text", text)
                scope.launch(Dispatchers.IO) {
                    try { dl.send("utterance/text", payload.toString()) } catch (_: Throwable) {}
                }
                partialText = null
                runOnUiThread { composeView.setContent(content) }
            }
        }
    }

    private val composeView by lazy { ComposeView(this) }

    private val content: @androidx.compose.runtime.Composable () -> Unit = {
        MaterialTheme {
            WearApp(
                activity = this,
                onPttStart = { asr.start(null) },
                onPttStop = { asr.stop() },
                partialText = partialText,
                captionText = captionText
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        asr.release()
        tts.release()
    }

    private fun ensureRecordAudioPermission() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        asr.onActivityResult(requestCode, resultCode, data)
    }
}

