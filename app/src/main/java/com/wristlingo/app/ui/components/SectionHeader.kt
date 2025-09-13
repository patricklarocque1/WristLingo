package com.wristlingo.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues(vertical = 8.dp)) {
	Text(
		text = text,
		style = MaterialTheme.typography.titleSmall,
		fontWeight = FontWeight.Bold,
		modifier = modifier.fillMaxWidth().padding(contentPadding)
	)
}


