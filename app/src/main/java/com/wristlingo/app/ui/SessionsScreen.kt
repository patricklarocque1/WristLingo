package com.wristlingo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    onOpenSession: (Long) -> Unit
) {
    val sessions by repository.observeRecentSessions().collectAsState(initial = emptyList())
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Sessions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sessions) { session ->
                SessionRow(session = session, onClick = { onOpenSession(session.id) })
                Divider()
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity, onClick: () -> Unit) {
    val dateText = rememberDate(session.startedAtEpochMs)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = dateText, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Target: ${session.targetLang}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun rememberDate(epochMs: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(epochMs))
}

