package com.wristlingo.app.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VadUtilsTest {
    @Test
    fun computeRms_silence_isZero() {
        val rms = VadUtils.computeRms(ShortArray(1600) { 0 })
        assertTrue(rms >= 0.0)
        assertTrue(rms < 1.0)
    }

    @Test
    fun isSpeech_aboveThreshold_true() {
        assertTrue(VadUtils.isSpeech(1200.0, 1000.0))
        assertFalse(VadUtils.isSpeech(800.0, 1000.0))
    }

    @Test
    fun shouldFinalize_afterSilence_true() {
        val now = 10_000L
        val last = 9_000L
        assertTrue(VadUtils.shouldFinalize(last, now, 800))
        assertFalse(VadUtils.shouldFinalize(last, now, 1200))
    }
}
