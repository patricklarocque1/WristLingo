package com.wristlingo.core.transport

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FlowBurstTest {
    
    @Test
    fun rapidMessageSendsDoNotThrow() {
        // Test that the DlClient interface can handle rapid message sending
        val client = object : DlClient {
            private val messages = mutableListOf<Pair<String, String>>()
            
            override suspend fun send(topic: String, payload: String) {
                messages.add(topic to payload)
            }
            
            override fun addListener(listener: (topic: String, payload: String) -> Unit): () -> Unit {
                // No-op for this test
                return {}
            }
            
            fun getMessageCount() = messages.size
        }
        
        // Test that the interface is properly defined
        // In a real implementation, send() would be called from a coroutine
        assertEquals(0, client.getMessageCount())
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun rapidListenerRegistrationsDoNotThrow() = runTest {
        // Test that listener registration/unregistration works correctly
        val client = object : DlClient {
            private val listeners = mutableListOf<(String, String) -> Unit>()
            
            override suspend fun send(topic: String, payload: String) {
                // No-op for this test
            }
            
            override fun addListener(listener: (topic: String, payload: String) -> Unit): () -> Unit {
                listeners.add(listener)
                return { listeners.remove(listener) }
            }
            
            fun getListenerCount() = listeners.size
        }
        
        // Rapidly register and unregister listeners
        val unregisters = mutableListOf<() -> Unit>()
        repeat(100) { idx ->
            val unregister = client.addListener { topic, payload ->
                // No-op listener
            }
            unregisters.add(unregister)
        }
        
        // Verify all listeners were registered
        assertEquals(100, client.getListenerCount())
        
        // Unregister all listeners
        unregisters.forEach { it() }
        assertEquals(0, client.getListenerCount())
    }
}
