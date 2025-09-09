package com.wristlingo.wear

import android.os.Bundle
import android.app.Activity
import androidx.compose.ui.platform.ComposeView
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    Text(text = "Hello WristLingo Wear")
                }
            }
        }
        setContentView(composeView)
    }
}

