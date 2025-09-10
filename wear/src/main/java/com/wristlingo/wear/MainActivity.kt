package com.wristlingo.wear

import android.app.Activity
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val composeView = ComposeView(this)
        composeView.setContent {
            MaterialTheme {
                Text(text = "Hello WristLingo Wear")
            }
        }
        setContentView(composeView)
    }
}

