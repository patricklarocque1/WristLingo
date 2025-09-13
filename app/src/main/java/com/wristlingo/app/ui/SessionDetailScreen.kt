package com.wristlingo.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
// stickyHeader is available in newer foundation; inline headers used for broad compatibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wristlingo.app.data.SessionRepository
import com.wristlingo.app.data.UtteranceEntity
import com.wristlingo.app.data.JsonlExportImport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun SessionDetailScreen(
    repository: SessionRepository,
    sessionId: Long,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val utterances by repository.observeUtterances(sessionId).collectAsStateWithLifecycle(initialValue = emptyList())
    Column(modifier = modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Session #$sessionId",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = {
                    // Export current session to cache and share
                    val meta = JsonlExportImport.SessionMeta(startedAtEpochMs = utterances.firstOrNull()?.timestampEpochMs ?: System.currentTimeMillis(), targetLang = utterances.firstOrNull()?.dstLang ?: "")
                    val lines = JsonlExportImport.toJsonlLines(meta, utterances)
                    val file = JsonlExportImport.writeJsonlToCache(context, "session-${sessionId}.jsonl", lines)
                    val uri = JsonlExportImport.contentUriFor(context, file)
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Share session"))
                }) { Icon(Icons.Default.Share, contentDescription = "Share session") }
                Button(onClick = onBack) { Text("Back") }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val grouped = utterances.groupBy { toLocalDate(it.timestampEpochMs) }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (day, list) ->
                item { DayHeader(day) }
                items(list) { utt ->
                    UtteranceBubbles(utt)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun UtteranceBubbles(utt: UtteranceEntity) {
    val timeText = rememberTime(utt.timestampEpochMs)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = timeText, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Bubble(text = utt.srcText, label = utt.srcLang ?: "?", isPrimary = false)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Bubble(text = utt.dstText, label = utt.dstLang, isPrimary = true)
            }
        }
    }
}

@Composable
private fun rememberTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

@Composable
private fun DayHeader(day: LocalDate) {
    Text(
        text = day.toString(),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    )
}

private fun toLocalDate(epochMs: Long): LocalDate {
    return Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
}

@Composable
private fun Bubble(text: String, label: String, isPrimary: Boolean) {
    val bg = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val onBg = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .padding(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = onBg)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = onBg.copy(alpha = 0.7f))
        }
    }
}

