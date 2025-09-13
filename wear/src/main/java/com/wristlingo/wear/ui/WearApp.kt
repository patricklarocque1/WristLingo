package com.wristlingo.wear.ui

import android.app.Activity
import android.os.PowerManager
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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wristlingo.wear.ui.components.RingMicButton
import com.wristlingo.wear.ui.components.CaptionTicker
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color

@Composable
fun WearApp(
    activity: Activity,
    onPttStart: () -> Unit,
    onPttStop: () -> Unit,
    partialText: String?,
    captionText: String?,
    recording: Boolean,
    disconnected: Boolean,
    showMicRationale: Boolean = false,
    permanentlyDenied: Boolean = false,
    onRequestMic: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    dlError: String? = null,
    onRetrySend: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val pm = remember { ctx.getSystemService(PowerManager::class.java) }
    // Approximate ambient by power-save/idle to avoid extra dependencies
    val isAmbient = (pm?.isPowerSaveMode == true) || (android.os.Build.VERSION.SDK_INT >= 23 && pm?.isDeviceIdleMode == true)
    var showReconnectToast by remember { mutableStateOf(false) }
    LaunchedEffect(disconnected) {
        if (!disconnected) {
            showReconnectToast = true
            kotlinx.coroutines.delay(1000)
            showReconnectToast = false
        }
    }

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
                            label = { Text("Phone not reachable") },
                            colors = ChipDefaults.chipColors()
                        )
                    }
                    if (dlError != null) {
                        Chip(onClick = onRetrySend, label = { Text("Retry send") }, colors = ChipDefaults.chipColors())
                    }
                    CaptionTicker(text = captionText ?: "", ambient = isAmbient)
                    if (showReconnectToast) {
                        Chip(
                            onClick = {},
                            label = { Text("Reconnected") },
                            colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF2E7D32))
                        )
                    }
                }

                // Center: ring PTT and partial below
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showMicRationale) {
                        Chip(onClick = onRequestMic, label = { Text(if (permanentlyDenied) "Open Settings" else "Try again") }, colors = ChipDefaults.chipColors())
                    }
                    RingMicButton(
                        recording = recording,
                        ambient = isAmbient,
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        onPttStart()
                                        tryAwaitRelease()
                                        onPttStop()
                                    }
                                )
                            }
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    CaptionTicker(text = partialText ?: "", ambient = isAmbient)
                }
            }
        }
    }
}

