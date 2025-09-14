package com.wristlingo.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveOverlayState(
	val captions: List<String> = emptyList(),
	val paused: Boolean = false
)

class LiveOverlayViewModel(private val handle: SavedStateHandle) : ViewModel() {
	private val _state = MutableStateFlow(
		LiveOverlayState(
			captions = handle.get<List<String>>(KEY_CAPTIONS) ?: emptyList(),
			paused = handle.get<Boolean>(KEY_PAUSED) ?: false
		)
	)
	val state: StateFlow<LiveOverlayState> = _state.asStateFlow()

	fun appendCaption(text: String) {
		val updated = (_state.value.captions + text).takeLast(10)
		_state.value = _state.value.copy(captions = updated)
		handle[KEY_CAPTIONS] = updated
	}

	fun setPaused(value: Boolean) {
		_state.value = _state.value.copy(paused = value)
		handle[KEY_PAUSED] = value
	}

	companion object {
		private const val KEY_CAPTIONS = "captions"
		private const val KEY_PAUSED = "paused"
	}
}


