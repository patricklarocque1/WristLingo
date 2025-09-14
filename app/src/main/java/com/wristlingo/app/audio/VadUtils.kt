package com.wristlingo.app.audio

import kotlin.math.sqrt

object VadUtils {
	fun computeRms(samples: ShortArray): Double {
		if (samples.isEmpty()) return 0.0
		var sum = 0.0
		for (s in samples) {
			sum += (s.toInt() * s.toInt()).toDouble()
		}
		return sqrt(sum / samples.size)
	}

	fun isSpeech(rms: Double, threshold: Double): Boolean = rms > threshold

	fun shouldFinalize(lastVoiceMs: Long, nowMs: Long, silenceMs: Int): Boolean {
		if (lastVoiceMs <= 0L) return false
		return (nowMs - lastVoiceMs) >= silenceMs
	}
}


