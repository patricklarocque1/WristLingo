package com.wristlingo.core.transport

/**
 * Simple duplex link client API for exchanging small JSON strings over topics.
 */
interface DlClient {
    /** Send a small JSON payload on a topic to the counterpart device. */
    suspend fun send(topic: String, payload: String)

    /** Register a listener for inbound messages. Return a function to unregister. */
    fun setListener(listener: (topic: String, payload: String) -> Unit): () -> Unit
}

