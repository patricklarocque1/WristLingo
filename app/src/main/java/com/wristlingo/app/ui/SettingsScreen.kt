package com.wristlingo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wristlingo.core.settings.Settings
import com.wristlingo.app.transport.TranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.res.stringResource
import com.wristlingo.app.R
import com.wristlingo.app.asr.WhisperModelManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wristlingo.app.ui.components.SectionHeader
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow

@Composable
fun SettingsScreen(
    settings: Settings,
    translationProvider: TranslationProvider,
    isOfflineFlavor: Boolean,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onOpenLanguagePicker: ((String) -> Unit)? = null,
    onLanguagePicked: ((String) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            settings = settings,
            translationProvider = translationProvider,
            isOfflineFlavor = isOfflineFlavor,
            whisperMgr = WhisperModelManager(ctx.applicationContext)
        )
    )
    val state = vm.state

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Row {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.action_back))
                        }
                    }
                    Text(
                        text = stringResource(id = R.string.title_settings),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding).padding(horizontal = 16.dp)) {
            // Translation section
            SectionHeader(text = stringResource(id = R.string.translation))
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.target_language), maxLines = 1) },
                supportingContent = { Text(state.value.target.uppercase(), maxLines = 1) },
                trailingContent = {
                    TextButton(onClick = { onOpenLanguagePicker?.invoke(state.value.target) }) {
                        Text(stringResource(id = R.string.change), maxLines = 1)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.value.langQuery,
                onValueChange = vm::setLangQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(id = R.string.search_languages), maxLines = 1) }
            )
            val filtered = state.value.allLangs.filter { it.contains(state.value.langQuery, ignoreCase = true) }
            LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                items(filtered) { code ->
                    ListItem(
                        headlineContent = { Text(code.uppercase(), maxLines = 1) },
                        trailingContent = {
                            TextButton(onClick = {
                                vm.setTarget(code)
                                onLanguagePicked?.invoke(code)
                            }) {
                                Text(if (state.value.target == code) stringResource(id = R.string.selected) else stringResource(id = R.string.select), maxLines = 1)
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Speech section
            SectionHeader(text = stringResource(id = R.string.speech))
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.speak_translations), maxLines = 1) },
                trailingContent = {
                    Checkbox(
                        checked = state.value.autoSpeak,
                        onCheckedChange = { vm.setAutoSpeak(it) }
                    )
                }
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Translation provider section
            SectionHeader(text = stringResource(id = R.string.translation))
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.use_cloud_translate), maxLines = 1) },
                supportingContent = { if (isOfflineFlavor) Text(stringResource(id = R.string.use_cloud_translate_offline_disabled), maxLines = 1) },
                trailingContent = {
                    Checkbox(
                        checked = state.value.useCloudTranslate,
                        onCheckedChange = if (isOfflineFlavor) null else { vm::setUseCloudTranslate },
                        enabled = !isOfflineFlavor
                    )
                }
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Playback / Whisper phone section
            SectionHeader(text = stringResource(id = R.string.whisper_phone_section))
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.whisper_phone_toggle), maxLines = 1) },
                trailingContent = {
                    Checkbox(
                        checked = state.value.useWhisperRemote,
                        onCheckedChange = vm::setUseWhisperRemote
                    )
                }
            )
            OutlinedTextField(
                value = state.value.whisperModelPath,
                onValueChange = vm::setWhisperModelPath,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("/absolute/path/to/model.gguf", maxLines = 1) },
                label = { Text(stringResource(id = R.string.whisper_model_path), maxLines = 1) },
                readOnly = true
            )
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.whisper_model_status, if (state.value.whisperHasModel) stringResource(id = R.string.downloaded) else stringResource(id = R.string.not_downloaded)), maxLines = 1) }
            )
            OutlinedTextField(
                value = state.value.whisperUrl,
                onValueChange = vm::setWhisperUrl,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.whisper_model_url), maxLines = 1) },
                placeholder = { Text("https://…/model.gguf", maxLines = 1) }
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Advanced tuning
            SectionHeader(text = "Whisper tuning")
            var vadThresh by remember { mutableStateOf(settings.vadRmsThreshold.toString()) }
            var silenceMs by remember { mutableStateOf(settings.vadSilenceMs.toString()) }
            var winMs by remember { mutableStateOf(settings.partialWindowMs.toString()) }
            var thrMs by remember { mutableStateOf(settings.partialThrottleMs.toString()) }
            var backlogMs by remember { mutableStateOf(settings.backlogCapMs.toString()) }
            var logRms by remember { mutableStateOf(settings.logRms) }
            OutlinedTextField(
                value = vadThresh,
                onValueChange = { vadThresh = it; it.toIntOrNull()?.let { v -> settings.vadRmsThreshold = v } },
                label = { Text("VAD RMS threshold", maxLines = 1) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = silenceMs,
                onValueChange = { silenceMs = it; it.toIntOrNull()?.let { v -> settings.vadSilenceMs = v } },
                label = { Text("VAD silence ms", maxLines = 1) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = winMs,
                onValueChange = { winMs = it; it.toIntOrNull()?.let { v -> settings.partialWindowMs = v } },
                label = { Text("Partial window ms", maxLines = 1) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = thrMs,
                onValueChange = { thrMs = it; it.toIntOrNull()?.let { v -> settings.partialThrottleMs = v } },
                label = { Text("Partial throttle ms", maxLines = 1) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = backlogMs,
                onValueChange = { backlogMs = it; it.toIntOrNull()?.let { v -> settings.backlogCapMs = v } },
                label = { Text("Backlog cap ms", maxLines = 1) },
                modifier = Modifier.fillMaxWidth()
            )
            ListItem(
                headlineContent = { Text("Log RMS to Logcat", maxLines = 1) },
                trailingContent = {
                    Checkbox(checked = logRms, onCheckedChange = { checked -> logRms = checked; settings.logRms = checked })
                }
            )
            OutlinedTextField(
                value = state.value.whisperSha256,
                onValueChange = vm::setWhisperSha256,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.whisper_model_sha256), maxLines = 1) },
                placeholder = { Text("64-hex SHA-256", maxLines = 1) }
            )
            Button(
                onClick = { vm.downloadWhisperModel() },
                enabled = !state.value.whisperDownloading && state.value.whisperUrl.isNotBlank() && state.value.whisperSha256.length >= 64
            ) {
                Text(
                    if (state.value.whisperDownloading)
                        stringResource(id = R.string.whisper_model_downloading, state.value.whisperProgress)
                    else stringResource(id = R.string.whisper_model_download)
                )
            }
            // Manage Whisper model card
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            SectionHeader(text = "Manage Whisper model")
            ListItem(
                headlineContent = { Text("Status: " + if (state.value.whisperHasModel) stringResource(id = R.string.downloaded) else stringResource(id = R.string.not_downloaded), maxLines = 1) },
                supportingContent = { Text("Size: ${state.value.whisperModelSizeBytes} bytes", maxLines = 1) },
                trailingContent = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = { vm.removeWhisperModel() }, enabled = state.value.whisperHasModel) { Icon(Icons.Default.Delete, contentDescription = "Remove model") }
                        // Pause/resume handled implicitly by download button state; keeping placeholders for UX parity
                    }
                }
            )
            if (!state.value.whisperHasModel && state.value.whisperUrl.isBlank()) {
                Text(text = "Model missing. Provide URL + SHA-256 above, then Download.")
            }
            ListItem(
                headlineContent = { Text(stringResource(id = R.string.on_device_model_status, state.value.modelStatus), maxLines = 1) },
                trailingContent = {
                    Button(
                        onClick = { vm.downloadTranslationModelForTarget() },
                        enabled = !state.value.isDownloading
                    ) { Text(if (state.value.isDownloading) stringResource(id = R.string.downloading) else stringResource(id = R.string.download_model), maxLines = 1) }
                }
            )
        }
    }
}

