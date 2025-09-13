package com.wristlingo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wristlingo.app.data.SessionEntity
import com.wristlingo.app.data.SessionRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SessionsScreen(
    repository: SessionRepository,
    modifier: Modifier = Modifier,
    onOpenSession: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit = {}
) {
    val sessions by repository.observeRecentSessionPreviews().collectAsStateWithLifecycle(initialValue = emptyList())
    Column(modifier = modifier.padding(16.dp)) {
        var taps = 0
        Row(modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    taps++
                    if (taps >= 5) { onOpenDiagnostics() ; taps = 0 }
                }
            )
        }) {
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onOpenSettings) { Text("Settings") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sessions) { session ->
                SessionRow(
                    session = session,
                    onClick = { onOpenSession(session.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionRepository.SessionPreview, onClick: () -> Unit) {
    val dateText = rememberDate(session.startedAtEpochMs)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(text = session.title ?: "(untitled)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = dateText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(text = "Target: ${session.targetLang}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun rememberDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

