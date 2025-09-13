package com.wristlingo.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FlowBurstCoreTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun burstEmissionsDoNotThrow() = runTest {
        // Partials profile: keep latest for new collectors; drop oldest on overflow to avoid IAE
        val partials = MutableSharedFlow<String>(
            replay = 1,
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        // Events/finals profile: no replay, buffered to avoid IAE on non-default overflow
        val events = MutableSharedFlow<String>(
            replay = 0,
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        repeat(500) { i ->
            partials.tryEmit("p-$i")
            events.tryEmit("e-$i")
        }
        // Test passes if no IllegalArgumentException was thrown
    }
}


