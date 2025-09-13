package com.wristlingo.app.transport

import com.wristlingo.app.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowConfigurationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun rapidOrchestratorFlowOperationsDoNotThrow() = runTest {
        // Test that MutableSharedFlow configurations used in orchestrator-like patterns
        // handle rapid emissions without IllegalArgumentException
        
        // Simulate caption updates (partials pattern)
        val captionFlow = MutableSharedFlow<String>(
            replay = 1,  // Latest caption should be available to new subscribers
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        
        // Simulate utterance events (events pattern)  
        val utteranceFlow = MutableSharedFlow<String>(
            replay = 0,  // Events don't need replay
            extraBufferCapacity = 16,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        
        // Rapidly emit like a real orchestrator would
        repeat(1000) { idx ->
            captionFlow.tryEmit("Caption update $idx")
            if (idx % 10 == 0) {  // Less frequent final utterances
                utteranceFlow.tryEmit("Final utterance ${idx / 10}")
            }
        }
        
        // Test passes if no IllegalArgumentException was thrown
    }
    
    @Test 
    fun startStopOperationsDoNotThrow() = runTest {
        // Test that rapid start/stop cycles don't cause flow issues
        // This simulates the orchestrator start/stop pattern
        
        val statusFlow = MutableSharedFlow<String>(
            replay = 1,  // Current status should be available
            extraBufferCapacity = 8,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        
        // Simulate rapid start/stop cycles
        repeat(50) { idx ->
            statusFlow.tryEmit("started-$idx")
            statusFlow.tryEmit("stopped-$idx")
        }
        
        // Test passes if no IllegalArgumentException was thrown
    }
}
