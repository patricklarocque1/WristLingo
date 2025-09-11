package com.wristlingo.wear.asr

import android.app.Activity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AsrControllerFlowConfigTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun constructingControllerAndEmittingPartialsDoesNotThrow() = runTest {
        val controller = AsrController(FakeActivity())

        // Rapidly emit many partials; should not crash with IAE, and last value retained
        repeat(200) { idx -> controller.emitPartialForTest("partial-$idx") }

        // New collector should see the latest partial
        val latest = controller.partial.first()
        assertEquals("partial-199", latest)
    }

    private class FakeActivity : Activity()
}


