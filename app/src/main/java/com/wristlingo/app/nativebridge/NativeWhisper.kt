package com.wristlingo.app.nativebridge

class NativeWhisper {
    init {
        System.loadLibrary("whisper_bridge")
    }

    private external fun create(modelPath: String, sampleRate: Int): Long
    private external fun feedPcm(ptr: Long, pcm: ShortArray, length: Int)
    private external fun finalizeStream(ptr: Long): String
    private external fun destroy(ptr: Long)
    private external fun nativePartial(ptr: Long, windowMs: Int): String

    private var ctxPtr: Long = 0

    fun start(modelPath: String, sampleRate: Int): Boolean {
        if (ctxPtr != 0L) return true
        ctxPtr = create(modelPath, sampleRate)
        return ctxPtr != 0L
    }

    fun feed(pcm: ShortArray) {
        if (ctxPtr == 0L || pcm.isEmpty()) return
        feedPcm(ctxPtr, pcm, pcm.size)
    }

    fun finish(): String {
        if (ctxPtr == 0L) return ""
        return finalizeStream(ctxPtr)
    }

    fun close() {
        if (ctxPtr != 0L) {
            destroy(ctxPtr)
            ctxPtr = 0
        }
    }

    fun partial(windowMs: Int): String {
        val p = ctxPtr
        if (p == 0L) return ""
        return nativePartial(p, windowMs)
    }
}


