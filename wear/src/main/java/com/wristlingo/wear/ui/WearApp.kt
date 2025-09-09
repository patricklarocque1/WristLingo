package com.wristlingo.wear.ui

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Column

@Composable
fun WearApp(
    activity: Activity,
    onPttStart: () -> Unit,
    onPttStop: () -> Unit,
    partialText: String?,
    captionText: String?
) {
    Scaffold {
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = captionText ?: "")
                Text(text = partialText ?: "")
                Button(
                    onClick = {},
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                onPttStart()
                                tryAwaitRelease()
                                onPttStop()
                            }
                        )
                    }
                ) {
                    Text("PTT")
                }
            }
        }
    }
}

