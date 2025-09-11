package com.wristlingo.core.audio

import java.util.Base64

data class AudioHeader(
    val sr: Int,
    val bits: Int,
    val seq: Long,
    val end: Boolean = false
)

object AudioCodec {
    fun encodePcmMessage(header: AudioHeader, pcmBytes: ByteArray): String {
        val b64 = Base64.getEncoder().encodeToString(pcmBytes)
        val headerJson = "{" +
            "\"sr\":${'$'}{header.sr}," +
            "\"bits\":${'$'}{header.bits}," +
            "\"seq\":${'$'}{header.seq}," +
            "\"end\":${'$'}{header.end}" +
            "}"
        return "{" +
            "\"header\":" + headerJson + "," +
            "\"pcm\":\"" + b64 + "\"" +
            "}"
    }

    data class Decoded(val header: AudioHeader, val pcm: ByteArray)

    fun tryDecodePcmMessage(jsonString: String): Result<Decoded> {
        return runCatching {
            val obj = parseObject(jsonString)
            val headerObj = obj["header"] as? Map<*, *> ?: error("missing header")
            val sr = (headerObj["sr"] as Number).toInt()
            val bits = (headerObj["bits"] as Number).toInt()
            val seq = (headerObj["seq"] as Number).toLong()
            val end = (headerObj["end"] as Boolean)
            val header = AudioHeader(sr, bits, seq, end)
            val pcmB64 = obj["pcm"] as? String ?: ""
            val bytes = if (pcmB64.isNotEmpty()) Base64.getDecoder().decode(pcmB64) else ByteArray(0)
            Decoded(header, bytes)
        }
    }

    // Minimal JSON object parser for the limited structure we produce
    private fun parseObject(json: String): Map<String, Any?> {
        val trimmed = json.trim()
        require(trimmed.startsWith("{") && trimmed.endsWith("}"))
        val map = mutableMapOf<String, Any?>()
        var i = 1
        while (i < trimmed.length - 1) {
            while (i < trimmed.length && trimmed[i].isWhitespace()) i++
            if (i >= trimmed.length - 1) break
            if (trimmed[i] == ',') { i++; continue }
            require(trimmed[i] == '"')
            val keyStart = ++i
            while (trimmed[i] != '"') i++
            val key = trimmed.substring(keyStart, i)
            i++
            while (trimmed[i] != ':') i++
            i++
            while (i < trimmed.length && trimmed[i].isWhitespace()) i++
            val value: Any? = when (trimmed[i]) {
                '"' -> {
                    val vStart = ++i
                    while (trimmed[i] != '"') i++
                    val str = trimmed.substring(vStart, i)
                    i++
                    str
                }
                '{' -> {
                    val subStart = i
                    var depth = 0
                    while (i < trimmed.length) {
                        if (trimmed[i] == '{') depth++
                        if (trimmed[i] == '}') { depth--; if (depth == 0) { i++; break } }
                        i++
                    }
                    parseObject(trimmed.substring(subStart, i))
                }
                't','f' -> {
                    val endIdx = sequenceOf(trimmed.indexOf(',', i), trimmed.indexOf('}', i)).filter { it >= 0 }.min()
                    val token = trimmed.substring(i, endIdx)
                    i = endIdx
                    token == "true"
                }
                else -> {
                    // number
                    val endIdx = sequenceOf(trimmed.indexOf(',', i), trimmed.indexOf('}', i)).filter { it >= 0 }.min()
                    val numStr = trimmed.substring(i, endIdx)
                    i = endIdx
                    if (numStr.contains('.')) numStr.toDouble() else numStr.toLong()
                }
            }
            map[key] = value
        }
        return map
    }
}


