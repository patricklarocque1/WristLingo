package com.wristlingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.wristlingo.core.transport.WearMessageClientDl
import com.wristlingo.core.settings.Settings
import com.wristlingo.app.transport.TranslationProvider
import com.wristlingo.app.transport.TranslatorOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    private val appScope = CoroutineScope(Dispatchers.Main)
    private lateinit var orchestrator: TranslatorOrchestrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)
        val dl = WearMessageClientDl(this)
        val tp = TranslationProvider(this, settings)
        orchestrator = TranslatorOrchestrator(this, appScope, dl, tp)
        orchestrator.start()
        setContent {
            MaterialTheme {
                Text(text = "Hello WristLingo")
            }
        }
    }
}

