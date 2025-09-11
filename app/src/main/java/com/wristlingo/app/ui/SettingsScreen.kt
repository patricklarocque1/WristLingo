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

@Composable
fun SettingsScreen(
    settings: Settings,
    translationProvider: TranslationProvider,
    isOfflineFlavor: Boolean,
    scope: CoroutineScope,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    var target by remember { mutableStateOf(settings.defaultTargetLanguage) }
    var autoSpeak by remember { mutableStateOf(settings.autoSpeak) }
    var useCloudTranslate by remember { mutableStateOf(settings.useCloudTranslate) }
    var useWhisperRemote by remember { mutableStateOf(settings.useWhisperRemote) }
    var whisperModelPath by remember { mutableStateOf(settings.whisperModelPath) }
    val ctx = LocalContext.current
    val whisperMgr = remember { WhisperModelManager(ctx.applicationContext) }
    var whisperHasModel by remember { mutableStateOf(whisperMgr.hasModel()) }
    var whisperUrl by remember { mutableStateOf("") }
    var whisperSha256 by remember { mutableStateOf("") }
    var whisperDownloading by remember { mutableStateOf(false) }
    var whisperProgress by remember { mutableStateOf(0) }
    var modelStatus by remember { mutableStateOf("Unknown") }
    var isDownloading by remember { mutableStateOf(false) }

    var langQuery by remember { mutableStateOf("") }
    val allLangs = remember { listOf("en", "fr", "es", "de", "it", "pt", "nl", "sv") }
    val filtered = allLangs.filter { it.contains(langQuery, ignoreCase = true) }

    LaunchedEffect(target) {
        val isDownloaded = try { translationProvider.isModelDownloaded(target) } catch (_: Throwable) { false }
        modelStatus = if (isDownloaded) "Downloaded" else "Not downloaded"
    }

    Column(modifier = modifier.padding(WindowInsets.systemBars.asPaddingValues()).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(id = R.string.title_settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (onBack != null) Button(onClick = onBack) { Text(stringResource(id = R.string.action_back)) }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = stringResource(id = R.string.language), style = MaterialTheme.typography.titleSmall)
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.target_language)) },
            supportingContent = { Text(target.uppercase()) },
            trailingContent = {
                TextButton(onClick = { }) { Text(stringResource(id = R.string.change), maxLines = 1) }
            }
        )
        OutlinedTextField(
            value = langQuery,
            onValueChange = { langQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(id = R.string.search_languages)) }
        )
        LazyColumn(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            items(filtered) { code ->
                ListItem(
                    headlineContent = { Text(code.uppercase(), maxLines = 1) },
                    trailingContent = {
                        TextButton(onClick = {
                            target = code
                            settings.defaultTargetLanguage = code
                        }) { Text(if (target == code) stringResource(id = R.string.selected) else stringResource(id = R.string.select), maxLines = 1) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = stringResource(id = R.string.speech), style = MaterialTheme.typography.titleSmall)
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.speak_translations), maxLines = 1) },
            trailingContent = {
                Checkbox(
                    checked = autoSpeak,
                    onCheckedChange = { checked ->
                        autoSpeak = checked
                        settings.autoSpeak = checked
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = stringResource(id = R.string.translation), style = MaterialTheme.typography.titleSmall)
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.use_cloud_translate), maxLines = 1) },
            supportingContent = { if (isOfflineFlavor) Text(stringResource(id = R.string.use_cloud_translate_offline_disabled)) },
            trailingContent = {
                Checkbox(
                    checked = useCloudTranslate,
                    onCheckedChange = if (isOfflineFlavor) null else { checked ->
                        useCloudTranslate = checked
                        settings.useCloudTranslate = checked
                    },
                    enabled = !isOfflineFlavor
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = stringResource(id = R.string.whisper_phone_section), style = MaterialTheme.typography.titleSmall)
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.whisper_phone_toggle), maxLines = 1) },
            trailingContent = {
                Checkbox(
                    checked = useWhisperRemote,
                    onCheckedChange = { checked ->
                        useWhisperRemote = checked
                        settings.useWhisperRemote = checked
                    }
                )
            }
        )
        OutlinedTextField(
            value = whisperModelPath,
            onValueChange = { value ->
                whisperModelPath = value
                settings.whisperModelPath = value
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("/absolute/path/to/model.gguf") },
            label = { Text(stringResource(id = R.string.whisper_model_path)) },
            readOnly = true
        )
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.whisper_model_status, if (whisperHasModel) stringResource(id = R.string.downloaded) else stringResource(id = R.string.not_downloaded))) }
        )
        OutlinedTextField(
            value = whisperUrl,
            onValueChange = { whisperUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.whisper_model_url)) },
            placeholder = { Text("https://…/model.gguf") }
        )
        // Tuning controls
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Whisper tuning", style = MaterialTheme.typography.titleSmall)
        var vadThresh by remember { mutableStateOf(settings.vadRmsThreshold.toString()) }
        var silenceMs by remember { mutableStateOf(settings.vadSilenceMs.toString()) }
        var winMs by remember { mutableStateOf(settings.partialWindowMs.toString()) }
        var thrMs by remember { mutableStateOf(settings.partialThrottleMs.toString()) }
        var backlogMs by remember { mutableStateOf(settings.backlogCapMs.toString()) }
        var logRms by remember { mutableStateOf(settings.logRms) }
        OutlinedTextField(
            value = vadThresh,
            onValueChange = { vadThresh = it; it.toIntOrNull()?.let { v -> settings.vadRmsThreshold = v } },
            label = { Text("VAD RMS threshold") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = silenceMs,
            onValueChange = { silenceMs = it; it.toIntOrNull()?.let { v -> settings.vadSilenceMs = v } },
            label = { Text("VAD silence ms") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = winMs,
            onValueChange = { winMs = it; it.toIntOrNull()?.let { v -> settings.partialWindowMs = v } },
            label = { Text("Partial window ms") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = thrMs,
            onValueChange = { thrMs = it; it.toIntOrNull()?.let { v -> settings.partialThrottleMs = v } },
            label = { Text("Partial throttle ms") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = backlogMs,
            onValueChange = { backlogMs = it; it.toIntOrNull()?.let { v -> settings.backlogCapMs = v } },
            label = { Text("Backlog cap ms") },
            modifier = Modifier.fillMaxWidth()
        )
        ListItem(
            headlineContent = { Text("Log RMS to Logcat") },
            trailingContent = {
                Checkbox(checked = logRms, onCheckedChange = { checked -> logRms = checked; settings.logRms = checked })
            }
        )
        OutlinedTextField(
            value = whisperSha256,
            onValueChange = { whisperSha256 = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(id = R.string.whisper_model_sha256)) },
            placeholder = { Text("64-hex SHA-256") }
        )
        Button(
            onClick = {
                whisperDownloading = true
                whisperProgress = 0
                scope.launch(Dispatchers.IO) {
                    try {
                        val path = whisperMgr.downloadModel(
                            url = whisperUrl.trim(),
                            sha256Hex = whisperSha256.trim()
                        ) { downloaded, total ->
                            if (total > 0) {
                                val pct = (downloaded * 100 / total).toInt()
                                launch(Dispatchers.Main) { whisperProgress = pct }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            whisperModelPath = path
                            settings.whisperModelPath = path
                            whisperHasModel = true
                            whisperDownloading = false
                        }
                    } catch (_: Throwable) {
                        withContext(Dispatchers.Main) {
                            whisperHasModel = whisperMgr.hasModel()
                            whisperDownloading = false
                        }
                    }
                }
            },
            enabled = !whisperDownloading && whisperUrl.isNotBlank() && whisperSha256.length >= 64
        ) {
            Text(
                if (whisperDownloading)
                    stringResource(id = R.string.whisper_model_downloading, whisperProgress)
                else stringResource(id = R.string.whisper_model_download)
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.on_device_model_status, modelStatus)) },
            trailingContent = {
                Button(
                    onClick = {
                        isDownloading = true
                        scope.launch(Dispatchers.IO) {
                            try { translationProvider.downloadTargetModelIfNeeded(target) } catch (_: Throwable) {}
                            val downloaded = try { translationProvider.isModelDownloaded(target) } catch (_: Throwable) { false }
                            withContext(Dispatchers.Main) {
                                modelStatus = if (downloaded) "Downloaded" else "Not downloaded"
                                isDownloading = false
                            }
                        }
                    },
                    enabled = !isDownloading
                ) { Text(if (isDownloading) stringResource(id = R.string.downloading) else stringResource(id = R.string.download_model)) }
            }
        )
    }
}

