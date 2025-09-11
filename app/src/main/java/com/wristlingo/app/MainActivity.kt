package com.wristlingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.wristlingo.app.data.AppDatabase
import com.wristlingo.app.data.SessionRepository
import com.wristlingo.app.transport.TranslationProvider
import com.wristlingo.app.transport.TranslatorOrchestrator
import com.wristlingo.core.settings.Settings
import com.wristlingo.core.transport.WearMessageClientDl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var orchestrator: TranslatorOrchestrator
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = Settings(this)
        val db = AppDatabase.getInstance(this)
        val repo = SessionRepository(db)
        val dl = WearMessageClientDl(applicationContext)
        val translator = TranslationProvider(applicationContext, settings)
        orchestrator = TranslatorOrchestrator(
            context = applicationContext,
            scope = scope,
            dl = dl,
            translationProvider = translator,
            repository = repo
        )
        orchestrator.start()
        setContent {
            MaterialTheme {
                Text(text = "Hello WristLingo")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orchestrator.stop()
        scope.cancel()
    }
}

