@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.wristlingo.wear.asr

import android.app.Activity
import com.wristlingo.wear.testing.MainDispatcherRule
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FinalsFlowTest {

    @get:Rule 
    val main = MainDispatcherRule()

    @Test
    fun finalsAreEmittedAndCollectable() = runTest {
        // Pass the test-owned scope so child jobs are controlled by runTest
        val sut = AsrController(FakeActivity(), scope = backgroundScope)

        // 1) Start collector first
        val pending = async { withTimeout(1_000) { sut.finalText.first() } }

        // 2) Ensure subscription is active
        runCurrent()

        // 3) Emit synchronously via tryEmit hook
        sut.emitFinalForTest("hello")

        // 4) Flush emission
        runCurrent()

        assertEquals("hello", pending.await())

        // 5) Cleanly cancel all jobs before test ends
        sut.close()
        runCurrent()
    }

    private class FakeActivity : Activity()
}


