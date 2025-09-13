package com.wristlingo.wear.asr

import android.app.Activity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowBurstTest {
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rapidPartialEmissionsDoNotThrow() = runTest {
        val controller = AsrController(FakeActivity())
        
        // Rapidly emit many partials; should not crash with IAE
        repeat(1000) { idx ->
            controller.emitPartialForTest("partial-$idx")
        }
        
        // Verify we can collect the latest partial (replay=1 ensures latest is available)
        val latest = controller.partial.first()
        assertEquals("partial-999", latest)
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rapidFinalEmissionsDoNotThrow() = runTest {
        val controller = AsrController(FakeActivity())
        
        // Rapidly emit many finals; should not crash with IAE
        // Since finals have replay=0, we just verify no exception is thrown
        repeat(1000) { idx ->
            controller.emitFinalForTest("final-$idx")
        }
        
        // For finals with replay=0, we can't collect past emissions
        // The test success is that no IllegalArgumentException was thrown above
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mixedRapidEmissionsDoNotThrow() = runTest {
        val controller = AsrController(FakeActivity())
        
        // Rapidly emit mixed partials and finals
        repeat(500) { idx ->
            controller.emitPartialForTest("partial-$idx")
            controller.emitFinalForTest("final-$idx")
        }
        
        // Verify we can collect partials (replay=1) but not finals (replay=0)
        val latestPartial = controller.partial.first()
        assertEquals("partial-499", latestPartial)
        
        // For finals with replay=0, we can't collect past emissions
        // The test success is that no IllegalArgumentException was thrown above
    }
    
    private class FakeActivity : Activity()
}
