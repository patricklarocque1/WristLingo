package com.wristlingo.wear.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text

@Composable
fun CaptionTicker(text: String, ambient: Boolean, modifier: Modifier = Modifier) {
    // Simple marquee: animate horizontal offset; when ambient, disable and just ellipsize
    if (ambient || text.length < 16) {
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.fillMaxWidth())
        return
    }
    var start by remember { mutableStateOf(true) }
    val anim by animateFloatAsState(
        targetValue = if (start) 0f else -60f,
        animationSpec = tween(durationMillis = 4000, easing = LinearEasing),
        finishedListener = { start = !start }
    )
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.offset(x = anim.dp)
        )
    }
}


