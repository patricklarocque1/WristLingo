package com.wristlingo.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonlExportImportTest {
    @Test
    fun jsonl_roundtrip_meta_and_utterances() {
        val meta = JsonlExportImport.SessionMeta(1234L, "fr")
        val utts = listOf(
            UtteranceEntity(id = 1, sessionId = 9, timestampEpochMs = 100, srcText = "hi", dstText = "salut", srcLang = "en", dstLang = "fr"),
            UtteranceEntity(id = 2, sessionId = 9, timestampEpochMs = 200, srcText = "bye", dstText = "au revoir", srcLang = "en", dstLang = "fr")
        )
        val lines = JsonlExportImport.toJsonlLines(meta, utts)
        val (m2, back) = JsonlExportImport.parseJsonlLines(lines.asSequence())
        assertEquals(meta.startedAtEpochMs, m2.startedAtEpochMs)
        assertEquals(meta.targetLang, m2.targetLang)
        assertEquals(utts.size, back.size)
        assertEquals(utts[0].srcText, back[0].srcText)
        assertEquals(utts[1].dstText, back[1].dstText)
    }
}


