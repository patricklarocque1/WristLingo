package com.wristlingo.wear.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RingMicButton(
    recording: Boolean,
    ambient: Boolean,
    modifier: Modifier = Modifier,
    label: String = if (recording) "REC" else "PTT",
    innerColor: Color = if (recording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ringColor: Color = if (recording) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    content: (@Composable () -> Unit)? = null
) {
    val pulse by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val scaled = if (recording && !ambient) pulse else 1.0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scaled)
            .size(72.dp)
            .background(color = innerColor, shape = CircleShape)
            .border(width = 4.dp, color = ringColor, shape = CircleShape)
    ) {
        if (content != null) content() else Text(label, color = MaterialTheme.colorScheme.onPrimary)
    }
}


