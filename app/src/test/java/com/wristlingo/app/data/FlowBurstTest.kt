package com.wristlingo.app.data

import com.wristlingo.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowBurstTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun rapidSharedFlowEmissionsDoNotThrow() = runTest {
        // Test MutableSharedFlow with proper configuration to avoid IllegalArgumentException
        // Partials stream: replay=1, extraBufferCapacity=64, DROP_OLDEST
        val partialsFlow = MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        
        // Rapidly emit many partials; should not crash with IAE
        repeat(1000) { idx ->
            partialsFlow.tryEmit("partial-$idx")
        }
        
        // Verify we can collect the latest partial (replay=1 ensures latest is available)
        val latest = partialsFlow.asSharedFlow().first()
        assertEquals("partial-999", latest)
    }

    @Test
    fun rapidEventFlowEmissionsDoNotThrow() = runTest {
        // Test MutableSharedFlow for events: replay=0, extraBufferCapacity=16, DROP_OLDEST
        val eventsFlow = MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        
        // Rapidly emit many events; should not crash with IAE
        repeat(1000) { idx ->
            eventsFlow.tryEmit("event-$idx")
        }
        
        // For events with replay=0, we can't collect past emissions
        // The test success is that no IllegalArgumentException was thrown above
    }

    @Test
    fun mixedRapidFlowEmissionsDoNotThrow() = runTest {
        // Test both types of flows together
        val partialsFlow = MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val eventsFlow = MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        
        // Rapidly emit mixed partials and events
        repeat(500) { idx ->
            partialsFlow.tryEmit("partial-$idx")
            eventsFlow.tryEmit("event-$idx")
        }
        
        // Verify we can collect partials (replay=1) 
        val latestPartial = partialsFlow.asSharedFlow().first()
        assertEquals("partial-499", latestPartial)
        
        // For events with replay=0, we can't collect past emissions
        // The test success is that no IllegalArgumentException was thrown above
    }
}
