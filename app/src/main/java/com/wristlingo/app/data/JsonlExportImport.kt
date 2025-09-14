package com.wristlingo.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import org.json.JSONObject

object JsonlExportImport {
    data class SessionMeta(
        val startedAtEpochMs: Long,
        val targetLang: String
    )

    fun toJsonlLines(meta: SessionMeta, utterances: List<UtteranceEntity>): List<String> {
        val lines = mutableListOf<String>()
        val metaObj = JSONObject()
            .put("type", "session")
            .put("version", 1)
            .put("startedAtEpochMs", meta.startedAtEpochMs)
            .put("targetLang", meta.targetLang)
        lines += metaObj.toString()
        utterances.forEach { u ->
            val o = JSONObject()
                .put("type", "utterance")
                .put("ts", u.timestampEpochMs)
                .put("src", u.srcText)
                .put("dst", u.dstText)
                .put("srcLang", u.srcLang)
                .put("dstLang", u.dstLang)
            lines += o.toString()
        }
        return lines
    }

    fun parseJsonlLines(lines: Sequence<String>): Pair<SessionMeta, List<UtteranceEntity>> {
        var meta: SessionMeta? = null
        val utts = mutableListOf<UtteranceEntity>()
        lines.forEach { line ->
            if (line.isBlank()) return@forEach
            val o = JSONObject(line)
            when (o.optString("type")) {
                "session" -> {
                    meta = SessionMeta(
                        startedAtEpochMs = o.optLong("startedAtEpochMs"),
                        targetLang = o.optString("targetLang", "en")
                    )
                }
                "utterance" -> {
                    utts += UtteranceEntity(
                        id = 0L,
                        sessionId = 0L,
                        timestampEpochMs = o.optLong("ts"),
                        srcText = o.optString("src"),
                        dstText = o.optString("dst"),
                        srcLang = o.optString("srcLang", null),
                        dstLang = o.optString("dstLang")
                    )
                }
            }
        }
        val m = meta ?: SessionMeta(System.currentTimeMillis(), "en")
        return m to utts
    }

    fun writeJsonlToCache(context: Context, fileName: String, lines: List<String>): File {
        val out = File(context.cacheDir, fileName)
        out.writeText(lines.joinToString("\n"))
        return out
    }

    fun contentUriFor(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    }
}


