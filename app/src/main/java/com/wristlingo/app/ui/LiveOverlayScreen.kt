package com.wristlingo.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import com.wristlingo.app.R

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LiveOverlayScreen(
    currentText: String,
    languages: List<String>,
    selectedLang: String,
    onLangChange: (String) -> Unit,
    onPause: () -> Unit,
    onStar: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.systemBars),
        bottomBar = {
            val pauseDesc = stringResource(id = R.string.pause)
            val starDesc = stringResource(id = R.string.star)
            val endDesc = stringResource(id = R.string.end)
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onPause, modifier = Modifier.semantics { contentDescription = pauseDesc }) { Text(pauseDesc) }
                TextButton(onClick = onStar, modifier = Modifier.semantics { contentDescription = starDesc }) { Text(starDesc) }
                TextButton(onClick = onEnd, modifier = Modifier.semantics { contentDescription = endDesc }) { Text(endDesc) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = currentText,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = if (reduceMotion) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().animateContentSize()
            )
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = selectedLang.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(id = R.string.language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    languages.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.uppercase()) },
                            onClick = { onLangChange(lang); expanded = false }
                        )
                    }
                }
            }
        }
    }
}


