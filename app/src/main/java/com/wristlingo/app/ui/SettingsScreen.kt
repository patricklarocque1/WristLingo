package com.wristlingo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    var modelStatus by remember { mutableStateOf("Unknown") }
    var isDownloading by remember { mutableStateOf(false) }

    var langQuery by remember { mutableStateOf("") }
    val allLangs = remember { listOf("en", "fr", "es", "de", "it", "pt", "nl", "sv") }
    val filtered = allLangs.filter { it.contains(langQuery, ignoreCase = true) }

    LaunchedEffect(target) {
        val isDownloaded = try { translationProvider.isModelDownloaded(target) } catch (_: Throwable) { false }
        modelStatus = if (isDownloaded) "Downloaded" else "Not downloaded"
    }

    Column(modifier = modifier.padding(16.dp)) {
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
                TextButton(onClick = { }) { Text(stringResource(id = R.string.change)) }
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
                    headlineContent = { Text(code.uppercase()) },
                    trailingContent = {
                        TextButton(onClick = {
                            target = code
                            settings.defaultTargetLanguage = code
                        }) { Text(if (target == code) stringResource(id = R.string.selected) else stringResource(id = R.string.select)) }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = stringResource(id = R.string.speech), style = MaterialTheme.typography.titleSmall)
        ListItem(
            headlineContent = { Text(stringResource(id = R.string.speak_translations)) },
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
            headlineContent = { Text(stringResource(id = R.string.use_cloud_translate)) },
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

