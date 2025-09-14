package com.wristlingo.app.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Simple event bus for live captions from speech recognition
 */
object LiveCaptionBus {
    private val _partials = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val _finals = MutableSharedFlow<String>(extraBufferCapacity = 1)
    
    val partials: SharedFlow<String> = _partials.asSharedFlow()
    val finals: SharedFlow<String> = _finals.asSharedFlow()
    
    fun emitPartial(text: String) {
        _partials.tryEmit(text)
    }
    
    fun emitFinal(text: String) {
        _finals.tryEmit(text)
    }
}