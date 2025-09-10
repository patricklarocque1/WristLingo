package com.wristlingo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.nl.translate.TranslateLanguage
import com.wristlingo.core.settings.Settings
import com.wristlingo.app.transport.TranslationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settings: Settings,
    translationProvider: TranslationProvider,
    isOfflineFlavor: Boolean,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    var target by remember { mutableStateOf(settings.defaultTargetLanguage) }
    var autoSpeak by remember { mutableStateOf(settings.autoSpeak) }
    var useCloudTranslate by remember { mutableStateOf(settings.useCloudTranslate) }
    var modelStatus by remember { mutableStateOf("Unknown") }
    var isChecking by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        isChecking = true
        val downloaded = try {
            translationProvider.isModelDownloaded(target)
        } catch (_: Throwable) { false }
        modelStatus = if (downloaded) "Downloaded" else "Not downloaded"
        isChecking = false
    }

    Column(modifier = modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (onBack != null) Button(onClick = onBack) { Text("Back") }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text(text = "Target language")
        OutlinedTextField(
            value = target,
            onValueChange = {
                target = it
                settings.defaultTargetLanguage = it
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. fr, es, de, en") }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = autoSpeak,
                onCheckedChange = { checked ->
                    autoSpeak = checked
                    settings.autoSpeak = checked
                }
            )
            Text("Auto speak translated text")
        }

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Checkbox(
                checked = useCloudTranslate,
                onCheckedChange = if (isOfflineFlavor) null else { checked ->
                    useCloudTranslate = checked
                    settings.useCloudTranslate = checked
                },
                enabled = !isOfflineFlavor
            )
            val cloudLabel = if (isOfflineFlavor) "Use cloud translate (disabled in offline)" else "Use cloud translate"
            Text(cloudLabel)
        }

        Divider()
        Spacer(modifier = Modifier.height(8.dp))
        Text("On-device model status: $modelStatus")
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                isDownloading = true
                // Kick model download
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    try { translationProvider.downloadTargetModelIfNeeded(target) } catch (_: Throwable) {}
                    val downloaded = try { translationProvider.isModelDownloaded(target) } catch (_: Throwable) { false }
                    withContext(Dispatchers.Main) {
                        modelStatus = if (downloaded) "Downloaded" else "Not downloaded"
                        isDownloading = false
                    }
                }
            },
            enabled = !isDownloading
        ) { Text(if (isDownloading) "Downloading..." else "Download on-device translation model") }
    }
}

