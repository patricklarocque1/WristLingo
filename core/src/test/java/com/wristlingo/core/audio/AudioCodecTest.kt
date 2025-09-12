package com.wristlingo.core.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioCodecTest {
    @Test
    fun roundtrip() {
        val header = AudioHeader(sr = 16000, bits = 16, seq = 1L, end = false)
        val pcm = ByteArray(16) { i -> i.toByte() }
        val json = AudioCodec.encodePcmMessage(header, pcm)
        val decoded = AudioCodec.tryDecodePcmMessage(json).getOrThrow()
        assertEquals(header, decoded.header)
        assertTrue(pcm.contentEquals(decoded.pcm))
    }

    @Test
    fun corruptPayload() {
        val bad = "{\"header\":{\"sr\":16000,\"bits\":16,\"seq\":1,\"end\":false},\"pcm\":\"@@@notbase64@@@\"}"
        val result = AudioCodec.tryDecodePcmMessage(bad)
        assertTrue(result.isFailure)
    }
}


