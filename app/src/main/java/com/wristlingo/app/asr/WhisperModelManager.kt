package com.wristlingo.app.asr

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class WhisperModelManager(private val context: Context) {
    private val modelsDir: File by lazy { File(context.filesDir, "models/whisper") }
    private val modelFile: File by lazy { File(modelsDir, DEFAULT_MODEL_NAME) }

    fun hasModel(): Boolean = modelFile.exists() && modelFile.length() > 0

    fun getModelPath(): String? = if (hasModel()) modelFile.absolutePath else null

    suspend fun downloadModel(
        url: String,
        sha256Hex: String,
        onProgress: ((downloaded: Long, total: Long) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        require(url.isNotBlank()) { "url is blank" }
        require(sha256Hex.isNotBlank()) { "sha256 is blank" }
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val tmp = File(modelsDir, "download.tmp")
        if (tmp.exists()) tmp.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        connection.connect()
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("HTTP ${connection.responseCode}")
        }
        val total = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
        val digest = MessageDigest.getInstance("SHA-256")
        var downloaded = 0L
        connection.inputStream.use { input ->
            FileOutputStream(tmp).use { out ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    out.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    downloaded += read
                    onProgress?.invoke(downloaded, total)
                }
                out.flush()
            }
        }
        connection.disconnect()

        val actualHex = digest.digest().joinToString(separator = "") { b ->
            val v = b.toInt() and 0xFF
            val h = Integer.toHexString(v)
            if (h.length == 1) "0$h" else h
        }
        if (!actualHex.equals(sha256Hex.trim(), ignoreCase = true)) {
            tmp.delete()
            throw IllegalStateException("SHA-256 mismatch: expected ${sha256Hex.lowercase()} got $actualHex")
        }

        // Replace existing model atomically
        if (modelFile.exists()) modelFile.delete()
        if (!tmp.renameTo(modelFile)) {
            tmp.delete()
            throw IllegalStateException("Failed to move model into place")
        }
        modelFile.absolutePath
    }

    companion object {
        private const val DEFAULT_MODEL_NAME = "whisper-model.gguf"
    }
}


