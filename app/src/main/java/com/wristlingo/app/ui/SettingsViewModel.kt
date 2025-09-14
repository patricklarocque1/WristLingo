package com.wristlingo.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wristlingo.app.asr.WhisperModelManager
import com.wristlingo.app.transport.TranslationProvider
import com.wristlingo.core.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
	val target: String = "en",
	val autoSpeak: Boolean = false,
	val useCloudTranslate: Boolean = true,
	val useWhisperRemote: Boolean = false,
	val whisperModelPath: String = "",
	val whisperModelSizeBytes: Long = 0L,
	val whisperHasModel: Boolean = false,
	val whisperUrl: String = "",
	val whisperSha256: String = "",
	val whisperDownloading: Boolean = false,
	val whisperProgress: Int = 0,
	val modelStatus: String = "Unknown",
	val isDownloading: Boolean = false,
	val langQuery: String = "",
	val allLangs: List<String> = listOf("eng","fr","es","de","it","pt","nl","sv").map { it.take(2) }
)

class SettingsViewModel(
	private val settings: Settings,
	private val translationProvider: TranslationProvider,
	private val isOfflineFlavor: Boolean,
	private val whisperMgr: WhisperModelManager,
	private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

	private val _state = MutableStateFlow(
		SettingsUiState(
			target = settings.defaultTargetLanguage,
			autoSpeak = settings.autoSpeak,
			useCloudTranslate = settings.useCloudTranslate,
			useWhisperRemote = settings.useWhisperRemote,
			whisperModelPath = settings.whisperModelPath,
			whisperModelSizeBytes = whisperMgr.getModelSizeBytes(),
			whisperHasModel = whisperMgr.hasModel(),
			allLangs = listOf("en","fr","es","de","it","pt","nl","sv")
		)
	)
	val state: StateFlow<SettingsUiState> = _state.asStateFlow()

	init {
		refreshModelStatus()
	}

	fun setTarget(code: String) {
		_state.value = _state.value.copy(target = code)
		settings.defaultTargetLanguage = code
		refreshModelStatus()
	}

	fun setAutoSpeak(value: Boolean) {
		_state.value = _state.value.copy(autoSpeak = value)
		settings.autoSpeak = value
	}

	fun setUseCloudTranslate(value: Boolean) {
		if (isOfflineFlavor) return
		_state.value = _state.value.copy(useCloudTranslate = value)
		settings.useCloudTranslate = value
	}

	fun setUseWhisperRemote(value: Boolean) {
		_state.value = _state.value.copy(useWhisperRemote = value)
		settings.useWhisperRemote = value
	}

	fun setWhisperModelPath(path: String) {
		_state.value = _state.value.copy(whisperModelPath = path, whisperModelSizeBytes = whisperMgr.getModelSizeBytes())
		settings.whisperModelPath = path
	}

	fun setLangQuery(query: String) {
		_state.value = _state.value.copy(langQuery = query)
	}

	fun setWhisperUrl(url: String) {
		_state.value = _state.value.copy(whisperUrl = url)
	}

	fun setWhisperSha256(sha: String) {
		_state.value = _state.value.copy(whisperSha256 = sha)
	}

	fun downloadWhisperModel() {
		val current = _state.value
		if (current.whisperDownloading) return
		_state.value = current.copy(whisperDownloading = true, whisperProgress = 0)
		viewModelScope.launch(ioDispatcher) {
			try {
				val path = whisperMgr.downloadModel(
					url = current.whisperUrl.trim(),
					sha256Hex = current.whisperSha256.trim()
				) { downloaded, total ->
					if (total > 0) {
						val pct = (downloaded * 100 / total).toInt()
						_state.value = _state.value.copy(whisperProgress = pct)
					}
				}
				_state.value = _state.value.copy(
					whisperModelPath = path,
					whisperModelSizeBytes = whisperMgr.getModelSizeBytes(),
					whisperHasModel = true,
					whisperDownloading = false
				)
				settings.whisperModelPath = path
			} catch (t: Throwable) {
				_state.value = _state.value.copy(
					whisperHasModel = whisperMgr.hasModel(),
					whisperDownloading = false
				)
			}
		}
	}

	fun removeWhisperModel() {
		viewModelScope.launch(ioDispatcher) {
			val removed = whisperMgr.removeModel()
			_state.value = _state.value.copy(
				whisperHasModel = whisperMgr.hasModel(),
				whisperModelPath = settings.whisperModelPath.takeIf { whisperMgr.hasModel() } ?: "",
				whisperModelSizeBytes = whisperMgr.getModelSizeBytes()
			)
			if (removed && !whisperMgr.hasModel()) settings.whisperModelPath = ""
		}
	}

	fun downloadTranslationModelForTarget() {
		val code = _state.value.target
		if (_state.value.isDownloading) return
		_state.value = _state.value.copy(isDownloading = true)
		viewModelScope.launch(ioDispatcher) {
			try { translationProvider.downloadTargetModelIfNeeded(code) } catch (_: Throwable) {}
			val downloaded = try { translationProvider.isModelDownloaded(code) } catch (_: Throwable) { false }
			_state.value = _state.value.copy(
				modelStatus = if (downloaded) "Downloaded" else "Not downloaded",
				isDownloading = false
			)
		}
	}

	fun refreshModelStatus() {
		val code = _state.value.target
		viewModelScope.launch(ioDispatcher) {
			val isDownloaded = try { translationProvider.isModelDownloaded(code) } catch (_: Throwable) { false }
			_state.value = _state.value.copy(modelStatus = if (isDownloaded) "Downloaded" else "Not downloaded")
		}
	}

	class Factory(
		private val settings: Settings,
		private val translationProvider: TranslationProvider,
		private val isOfflineFlavor: Boolean,
		private val whisperMgr: WhisperModelManager
	) : ViewModelProvider.Factory {
		@Suppress("UNCHECKED_CAST")
		override fun <T : ViewModel> create(modelClass: Class<T>): T {
			return SettingsViewModel(settings, translationProvider, isOfflineFlavor, whisperMgr) as T
		}
	}
}


