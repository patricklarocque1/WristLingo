package com.wristlingo.app.ui

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-local bus for live caption updates to drive the phone overlay UI.
 * Partials use replay=1 to show latest quickly; finals are event-like.
 */
object LiveCaptionBus {
    private val _partials = MutableSharedFlow<String>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val partials: SharedFlow<String> = _partials.asSharedFlow()

    private val _finals = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val finals: SharedFlow<String> = _finals.asSharedFlow()

    fun emitPartial(text: String) {
        _partials.tryEmit(text)
    }

    fun emitFinal(text: String) {
        _finals.tryEmit(text)
    }
}


