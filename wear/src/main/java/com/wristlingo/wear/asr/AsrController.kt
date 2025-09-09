package com.wristlingo.wear.asr

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AsrController(private val activity: Activity) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _partial = MutableSharedFlow<String>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val partial = _partial.asSharedFlow()

    private val _finalText = MutableSharedFlow<String>(replay = 0, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val finalText = _finalText.asSharedFlow()

    private fun ensureRecognizer(): Boolean {
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) return false
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                    override fun onPartialResults(partialResults: Bundle?) {
                        val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = list?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            _partial.tryEmit(text)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = list?.firstOrNull()?.trim()
                        if (!text.isNullOrEmpty()) {
                            _finalText.tryEmit(text)
                        }
                    }
                })
            }
        }
        return true
    }

    fun start(localeTag: String?) {
        if (!ensureRecognizer()) {
            startFallback(localeTag)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            if (!localeTag.isNullOrEmpty()) putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stop() {
        speechRecognizer?.stopListening()
    }

    private fun startFallback(localeTag: String?) {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                if (!localeTag.isNullOrEmpty()) putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            }
            activity.startActivityForResult(intent, REQUEST_CODE)
        } catch (_: ActivityNotFoundException) {
            // No recognizer available
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE) return
        if (resultCode != Activity.RESULT_OK) return
        val list = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val text = list?.firstOrNull()?.trim()
        if (!text.isNullOrEmpty()) {
            _finalText.tryEmit(text)
        }
    }

    fun release() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    companion object {
        private const val REQUEST_CODE = 0xA51
    }
}

