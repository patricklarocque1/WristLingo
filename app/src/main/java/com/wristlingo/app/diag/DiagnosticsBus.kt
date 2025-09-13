package com.wristlingo.app.diag

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DiagnosticsBus {
    data class DlMessage(val direction: Direction, val topic: String, val sizeBytes: Int)
    enum class Direction { In, Out }

    val messages = MutableSharedFlow<DlMessage>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val _asrActive = MutableStateFlow(false)
    val asrActive: StateFlow<Boolean> = _asrActive.asStateFlow()

    private val _vadRms = MutableStateFlow(0.0)
    val vadRms: StateFlow<Double> = _vadRms.asStateFlow()

    fun logIn(topic: String, size: Int) {
        messages.tryEmit(DlMessage(Direction.In, topic, size))
    }
    fun logOut(topic: String, size: Int) {
        messages.tryEmit(DlMessage(Direction.Out, topic, size))
    }
    fun setAsrActive(active: Boolean) { _asrActive.value = active }
    fun setVadRms(rms: Double) { _vadRms.value = rms }
}


