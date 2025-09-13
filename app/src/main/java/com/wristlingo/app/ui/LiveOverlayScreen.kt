package com.wristlingo.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest

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
    val vm: LiveOverlayViewModel = viewModel()
    val ui = vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier.windowInsetsPadding(WindowInsets.systemBars),
        bottomBar = {
            val pauseDesc = stringResource(id = R.string.pause)
            val starDesc = stringResource(id = R.string.star)
            val endDesc = stringResource(id = R.string.end)
            val ctx = LocalContext.current
            fun vibrateShort() {
                val vib = ctx.getSystemService(Vibrator::class.java)
                if (vib != null) {
                    if (Build.VERSION.SDK_INT >= 26) vib.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") vib.vibrate(20)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { vm.setPaused(!ui.value.paused); onPause(); vibrateShort() }, modifier = Modifier.semantics { contentDescription = pauseDesc }) { Text(if (ui.value.paused) stringResource(id = R.string.resume) else pauseDesc) }
                TextButton(onClick = onStar, modifier = Modifier.semantics { contentDescription = starDesc }) { Text(starDesc) }
                TextButton(onClick = { onEnd(); vibrateShort() }, modifier = Modifier.semantics { contentDescription = endDesc }) { Text(endDesc) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Listen to incoming caption updates and update local VM state
            LaunchedEffect(Unit) {
                // Partials update headline quickly
                launch {
                    LiveCaptionBus.partials.collectLatest { part ->
                        vm.appendCaption(part)
                    }
                }
                // Finals also append and could trigger additional UI actions later
                launch {
                    LiveCaptionBus.finals.collectLatest { fin ->
                        vm.appendCaption(fin)
                    }
                }
            }
            // Text zone: show latest caption (or provided currentText), with fade transitions
            val headlineText = if (currentText.isNotEmpty()) currentText else (ui.value.captions.lastOrNull() ?: "")
            AnimatedContent(targetState = headlineText, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "caption") { txt ->
                Text(
                    text = txt,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().height(120.dp)) {
                items(ui.value.captions) { cap ->
                    Text(text = cap, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
            }
            // Auto-scroll to bottom on update
            if (ui.value.captions.isNotEmpty()) {
                androidx.compose.runtime.LaunchedEffect(ui.value.captions.size) {
                    listState.animateScrollToItem(ui.value.captions.lastIndex)
                }
            }
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


