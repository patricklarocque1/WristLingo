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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wristlingo.app.data.SessionRepository
import com.wristlingo.app.ui.components.CaptionBadge
import com.wristlingo.app.ui.components.ElevatedCardL
import com.wristlingo.app.ui.components.PillChip
import com.wristlingo.app.R

@Composable
fun HomeScreen(
    repository: SessionRepository,
    onOpenSession: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onStartLive: () -> Unit,
    onOpenDiagnostics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val previews by repository.observeRecentSessionPreviews().collectAsStateWithLifecycle(initialValue = emptyList())
    var query by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf<String?>(null) }

    val langs = previews.map { it.targetLang }.distinct()
    val filtered = previews.filter {
        (query.isBlank() || (it.title?.contains(query, ignoreCase = true) == true)) &&
            (selectedLang == null || it.targetLang == selectedLang)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onStartLive) { Text(stringResource(id = R.string.live)) }
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = stringResource(id = R.string.home_title), style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onOpenDiagnostics != null) {
                        TextButton(onClick = onOpenDiagnostics) { Text("Diag") }
                    }
                    TextButton(onClick = onOpenSettings) { Text(stringResource(id = R.string.title_settings)) }
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(id = R.string.search_sessions)) }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillChip(label = stringResource(id = R.string.all), selected = selectedLang == null, onClick = { selectedLang = null })
                langs.forEach { lang ->
                    PillChip(label = lang.uppercase(), selected = selectedLang == lang, onClick = { selectedLang = lang })
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered) { s ->
                    ElevatedCardL(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), onClick = { onOpenSession(s.id) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(text = s.title ?: stringResource(id = R.string.untitled), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            CaptionBadge(src = "EN", dst = s.targetLang)
                        }
                    }
                }
            }
        }
    }
}


