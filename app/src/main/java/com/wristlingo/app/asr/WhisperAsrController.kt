package com.wristlingo.app.asr

import com.wristlingo.app.nativebridge.NativeWhisper

class WhisperAsrController(
    private val modelPathProvider: () -> String?
) {
    private var native: NativeWhisper? = null
    private var sampleRate: Int = 16000

    fun start(sr: Int): Boolean {
        val path = modelPathProvider() ?: ""
        if (path.isEmpty()) return false
        sampleRate = sr
        if (native == null) native = NativeWhisper()
        return native?.start(path, sampleRate) == true
    }

    fun feed(frame: ShortArray) {
        val n = native ?: return
        if (frame.isEmpty()) return
        n.feed(frame)
    }

    fun finalizeStream(): String {
        val n = native ?: return ""
        return n.finish()
    }

    fun partial(windowMs: Int): String {
        val n = native ?: return ""
        return n.partial(windowMs)
    }

    fun close() {
        native?.close()
        native = null
    }
}


