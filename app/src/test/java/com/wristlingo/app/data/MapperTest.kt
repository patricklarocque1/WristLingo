package com.wristlingo.app.data

import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {
    @Test
    fun entityRoundtrip() {
        val session = SessionEntity(id = 42L, startedAtEpochMs = 1234L, targetLang = "fr")
        assertEquals(42L, session.id)
        assertEquals(1234L, session.startedAtEpochMs)
        assertEquals("fr", session.targetLang)

        val utter = UtteranceEntity(
            id = 7L,
            sessionId = 42L,
            timestampEpochMs = 5678L,
            srcText = "hello",
            dstText = "bonjour",
            srcLang = "en",
            dstLang = "fr"
        )
        assertEquals(7L, utter.id)
        assertEquals(42L, utter.sessionId)
        assertEquals(5678L, utter.timestampEpochMs)
        assertEquals("hello", utter.srcText)
        assertEquals("bonjour", utter.dstText)
        assertEquals("en", utter.srcLang)
        assertEquals("fr", utter.dstLang)
    }
}


