package com.wristlingo.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wristlingo.app.R
import com.wristlingo.app.i18n.Languages
import com.wristlingo.app.i18n.RecentLanguagesStore

@Composable
fun LanguagePickerScreen(
	selectedCode: String,
	onSelect: (String) -> Unit,
	onBack: () -> Unit,
	modifier: Modifier = Modifier,
) {
	var query by remember { mutableStateOf("") }
	val context = androidx.compose.ui.platform.LocalContext.current
	val recentsStore = remember(context) { RecentLanguagesStore(context = context) }
	val recents = remember { recentsStore.get() }

	val filtered = remember(query) {
		val q = query.trim()
		if (q.isBlank()) Languages.all else Languages.all.filter { l ->
			l.name.contains(q, ignoreCase = true) || l.code.contains(q, ignoreCase = true)
		}
	}

	Scaffold(
		contentWindowInsets = WindowInsets.systemBars,
		topBar = {
			Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
					IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(id = R.string.action_back)) }
					Text(text = stringResource(id = R.string.target_language), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
				}
			}
		}
	) { padding ->
		Column(modifier = modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
			OutlinedTextField(
				value = query,
				onValueChange = { query = it },
				modifier = Modifier.fillMaxWidth(),
				placeholder = { Text(stringResource(id = R.string.search_languages)) }
			)
			Spacer(Modifier.height(8.dp))
			if (recents.isNotEmpty()) {
				Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
					recents.forEach { code ->
						val lang = Languages.byCode[code]
						if (lang != null) {
							AssistChip(
								onClick = { onSelect(code); recentsStore.add(code) },
								label = { Text((lang.flag ?: "") + " " + lang.name) }
							)
						}
					}
				}
				Spacer(Modifier.height(12.dp))
			}
			LazyColumn(modifier = Modifier.fillMaxSize()) {
				items(filtered) { lang ->
					val isSelected = lang.code == selectedCode
					val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent, label = "bg")
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(vertical = 10.dp)
							.semantics { contentDescription = "Select language ${lang.name} (${lang.code})" }
							.clickable { onSelect(lang.code); recentsStore.add(lang.code) }
					) {
						Text(text = (lang.flag ?: "") + "\t" + lang.name, modifier = Modifier.weight(1f))
						Text(text = lang.code.uppercase())
					}
				}
			}
		}
	}
}


