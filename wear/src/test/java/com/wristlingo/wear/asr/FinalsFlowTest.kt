package com.wristlingo.wear.asr

import android.app.Activity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FinalsFlowTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun finalsAreEmittedAndCollectable() = runTest {
        val controller = AsrController(FakeActivity())

        controller.emitFinalForTest("hello")
        controller.emitFinalForTest("world")

        val received = mutableListOf<String>()
        controller.finalText.take(2).toList(received)

        assertEquals(listOf("hello", "world"), received)
    }

    private class FakeActivity : Activity()
}


