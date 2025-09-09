package com.wristlingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.wristlingo.core.transport.WearMessageClientDl
import com.wristlingo.core.settings.Settings
import com.wristlingo.app.transport.TranslationProvider
import com.wristlingo.app.transport.TranslatorOrchestrator
import com.wristlingo.app.data.AppDatabase
import com.wristlingo.app.data.SessionRepository
import com.wristlingo.app.ui.SessionsScreen
import com.wristlingo.app.ui.SessionDetailScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    private val appScope = CoroutineScope(Dispatchers.Main)
    private lateinit var orchestrator: TranslatorOrchestrator
    private lateinit var repository: SessionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)
        val dl = WearMessageClientDl(this)
        val tp = TranslationProvider(this, settings)
        val db = AppDatabase.getInstance(this)
        repository = SessionRepository(db)
        orchestrator = TranslatorOrchestrator(this, appScope, dl, tp, repository)
        orchestrator.start()
        setContent {
            MaterialTheme {
                var selectedSessionId by remember { mutableStateOf<Long?>(null) }
                if (selectedSessionId == null) {
                    SessionsScreen(
                        repository = repository,
                        modifier = Modifier.fillMaxSize(),
                        onOpenSession = { selectedSessionId = it }
                    )
                } else {
                    SessionDetailScreen(
                        repository = repository,
                        sessionId = selectedSessionId!!,
                        modifier = Modifier.fillMaxSize(),
                        onBack = { selectedSessionId = null }
                    )
                }
            }
        }
    }
}

