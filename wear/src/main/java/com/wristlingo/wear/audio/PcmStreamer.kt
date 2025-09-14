package com.wristlingo.wear.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import com.wristlingo.wear.transport.WearMessageClientDl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

class PcmStreamer(
    private val dl: WearMessageClientDl,
    private val scope: CoroutineScope
) {
    private var job: Job? = null
    private val seq = AtomicLong(0L)
    @Volatile var isRecording: Boolean = false

    fun start(sr: Int = 16000) {
        stop()
        job = scope.launch(Dispatchers.IO) {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBuf = AudioRecord.getMinBufferSize(sr, channelConfig, audioFormat)
            val frameMs = 240 // ~200-300ms
            val frameSamples = (sr * frameMs) / 1000
            val frameBytes = frameSamples * 2
            val bufferSize = maxOf(minBuf, frameBytes)
            var recorder: AudioRecord? = null
            val buf = ByteArray(frameBytes)
            try {
                recorder = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sr,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                recorder.startRecording()
                isRecording = true
                while (true) {
                    val read = recorder.read(buf, 0, buf.size)
                    if (read <= 0) continue
                    val header = JSONObject()
                        .put("sr", sr)
                        .put("bits", 16)
                        .put("seq", seq.getAndIncrement())
                        .put("end", false)
                    val payload = JSONObject()
                        .put("header", header)
                        .put("pcm", Base64.encodeToString(buf, 0, read, Base64.NO_WRAP))
                        .toString()
                    try { dl.send("audio/pcm", payload) } catch (_: Throwable) {}
                }
            } catch (_: SecurityException) {
                isRecording = false
                return@launch
            } finally {
                isRecording = false
                try { recorder?.stop() } catch (_: Throwable) {}
                try { recorder?.release() } catch (_: Throwable) {}
            }
        }
    }

    fun stop() {
        val running = job
        if (running != null) {
            job = null
            running.cancel()
            scope.launch(Dispatchers.IO) {
                val header = JSONObject()
                    .put("sr", 16000)
                    .put("bits", 16)
                    .put("seq", seq.getAndIncrement())
                    .put("end", true)
                val payload = JSONObject().put("header", header).put("pcm", "").toString()
                try { dl.send("audio/pcm", payload) } catch (_: Throwable) {}
            }
        }
    }
}


