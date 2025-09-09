package com.wristlingo.core.transport

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * Wear OS/Data Layer implementation of DlClient using MessageClient.
 * Works on both phone and wear by choosing the first available connected node.
 */
class WearMessageClientDl(
    private val context: Context,
    private val appScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DlClient, MessageClient.OnMessageReceivedListener {

    @Volatile
    private var listener: ((topic: String, payload: String) -> Unit)? = null

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val nodeClient: NodeClient = Wearable.getNodeClient(context)

    private var registeredJob: Job? = null

    init {
        // Register immediately; safe to call multiple times internally
        registeredJob = appScope.launch {
            messageClient.addListener(this@WearMessageClientDl)
        }
    }

    override suspend fun send(topic: String, payload: String) {
        val nodes = try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (t: Throwable) {
            emptyList()
        }
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        for (node in nodes) {
            try {
                Tasks.await(messageClient.sendMessage(node.id, "/$topic", bytes))
                break
            } catch (_: Throwable) {
                // Try next node
            }
        }
    }

    override fun setListener(listener: (topic: String, payload: String) -> Unit): () -> Unit {
        this.listener = listener
        return {
            this.listener = null
        }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val path = event.path.trimStart('/')
        val payload = String(event.data, StandardCharsets.UTF_8)
        listener?.invoke(path, payload)
    }

    /** Returns true if at least one counterpart node is connected. */
    suspend fun hasConnectedNode(): Boolean {
        return try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            nodes.isNotEmpty()
        } catch (_: Throwable) {
            false
        }
    }

    fun close() {
        appScope.launch {
            try { messageClient.removeListener(this@WearMessageClientDl) } catch (_: Throwable) {}
        }
        registeredJob?.cancel()
    }
}

