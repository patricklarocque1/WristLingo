package com.wristlingo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wristlingo.app.diag.DiagnosticsBus
import kotlinx.coroutines.flow.collect
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.wristlingo.app.data.JsonlExportImport
import com.wristlingo.app.data.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import android.content.Intent
import android.net.Uri

@Composable
fun DiagnosticsScreen(onBack: () -> Unit, modifier: Modifier = Modifier, repository: SessionRepository? = null) {
    val asr = DiagnosticsBus.asrActive.collectAsStateWithLifecycle()
    val rms = DiagnosticsBus.vadRms.collectAsStateWithLifecycle()
    val msgs = remember { mutableStateListOf<DiagnosticsBus.DlMessage>() }
    val ctx = LocalContext.current
    var importUriText by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        DiagnosticsBus.messages.collect { m ->
            msgs.add(m)
            if (msgs.size > 50) msgs.removeAt(0)
        }
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Diagnostics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = onBack) { Text("Back") }
        }
        Spacer(Modifier.height(12.dp))
        Text("ASR active: ${asr.value}")
        Text("VAD RMS: ${"%.1f".format(rms.value)}")
        // TODO: Add CPU/FPS simple estimates if needed (placeholder)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Text("Last messages")
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(msgs) { m ->
                val lastWords = "…" // PII guard; avoid dumping full text
                Text("${m.direction} ${m.topic} (${m.sizeBytes}B) ${lastWords}")
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Text("Developer import (JSONL)")
        OutlinedTextField(
            value = importUriText,
            onValueChange = { importUriText = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("content:// or file:// URI to .jsonl") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/*"
                }
                try { ctx.startActivity(intent) } catch (_: Throwable) {}
            }) { Text("Pick file") }
            Button(onClick = {
                val repo = repository ?: return@Button
                val txt = importUriText.trim()
                if (txt.isBlank()) return@Button
                try {
                    val uri = Uri.parse(txt)
                    val input = ctx.contentResolver.openInputStream(uri) ?: return@Button
                    val lines = mutableListOf<String>()
                    BufferedReader(InputStreamReader(input)).use { br ->
                        var line = br.readLine()
                        while (line != null) {
                            lines += line
                            line = br.readLine()
                        }
                    }
                    val (meta, utts) = JsonlExportImport.parseJsonlLines(lines.asSequence())
                    // Insert session and remap utterances
                    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                        val newId = repo.createSession(targetLang = meta.targetLang, startedAtEpochMs = meta.startedAtEpochMs)
                        utts.forEach { u ->
                            repo.insertUtterance(
                                sessionId = newId,
                                timestampEpochMs = u.timestampEpochMs,
                                srcText = u.srcText,
                                dstText = u.dstText,
                                srcLang = u.srcLang,
                                dstLang = u.dstLang
                            )
                        }
                        withContext(Dispatchers.Main) {
                            importStatus = "Imported ${utts.size} lines into session #${newId}"
                        }
                    }
                } catch (t: Throwable) {
                    importStatus = "Import failed: ${t.message}"
                }
            }) { Text("Import") }
        }
        if (importStatus.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(importStatus)
        }
    }
}


