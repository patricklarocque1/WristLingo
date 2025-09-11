package com.wristlingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.wristlingo.app.ui.HomeScreen
import com.wristlingo.app.ui.LiveOverlayScreen
import com.wristlingo.app.ui.SessionDetailScreen
import com.wristlingo.app.ui.SettingsScreen
import com.wristlingo.app.ui.theme.WristLingoTheme
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
            WristLingoTheme {
                var route by remember { mutableStateOf<Screen>(Screen.Home) }
                when (val r = route) {
                    is Screen.Home -> HomeScreen(
                        repository = repo,
                        onOpenSession = { id -> route = Screen.SessionDetail(id) },
                        onOpenSettings = { route = Screen.Settings },
                        onStartLive = { route = Screen.Live }
                    )
                    is Screen.SessionDetail -> SessionDetailScreen(
                        repository = repo,
                        sessionId = r.id,
                        onBack = { route = Screen.Home }
                    )
                    is Screen.Settings -> SettingsScreen(
                        settings = settings,
                        translationProvider = translator,
                        isOfflineFlavor = !BuildConfig.USE_CLOUD_TRANSLATE,
                        scope = scope,
                        onBack = { route = Screen.Home }
                    )
                    is Screen.Live -> LiveOverlayScreen(
                        currentText = "",
                        languages = listOf("en","fr","es","de"),
                        selectedLang = settings.defaultTargetLanguage,
                        onLangChange = { settings.defaultTargetLanguage = it },
                        onPause = { },
                        onStar = { },
                        onEnd = { route = Screen.Home }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        orchestrator.stop()
        scope.cancel()
    }
}

private sealed interface Screen {
    data object Home : Screen
    data class SessionDetail(val id: Long) : Screen
    data object Settings : Screen
    data object Live : Screen
}

