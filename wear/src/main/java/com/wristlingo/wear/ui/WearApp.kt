package com.wristlingo.wear.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text

@Composable
fun WearApp(
    activity: Activity,
    onPttStart: () -> Unit,
    onPttStop: () -> Unit,
    partialText: String?,
    captionText: String?,
    recording: Boolean,
    disconnected: Boolean
) {
    Scaffold {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Top: status + current caption
                Column {
                    if (disconnected) {
                        Chip(
                            onClick = {},
                            label = { Text("Disconnected") },
                            colors = ChipDefaults.chipColors()
                        )
                    }
                    Text(text = captionText ?: "")
                }

                // Center: ring PTT and partial below
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier
                            .size(72.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onPttStart()
                                        tryAwaitRelease()
                                        onPttStop()
                                    }
                                )
                            },
                        shape = CircleShape
                    ) {
                        Text(if (recording) "REC" else "PTT")
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = partialText ?: "")
                }
            }
        }
    }
}

