package com.miqu.thinktwice

import com.miqu.thinktwice.ui.common.ProvideAppLanguage
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.miqu.thinktwice.data.prefs.Settings
import com.miqu.thinktwice.data.prefs.ThemeMode
import com.miqu.thinktwice.ui.navigation.AppNavHost
import com.miqu.thinktwice.ui.theme.ThinkTwiceTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val ready = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val container = appContainer

        lifecycleScope.launch {
            container.legacyImporter.importIfNeeded(container.content.content("en"))
            ready.value = true
        }
        splash.setKeepOnScreenCondition { !ready.value }

        // Roll the day over (streaks, daily challenge) while the app is open.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    container.clock.refresh()
                    delay(30_000)
                }
            }
        }

        setContent {
            val isReady by ready.collectAsStateWithLifecycle()
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null as Settings?)
            val current = settings
            val dark = when (current?.theme ?: ThemeMode.SYSTEM) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                    else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                )
            }
            ThinkTwiceTheme(mode = current?.theme ?: ThemeMode.SYSTEM) {
                if (isReady && current != null) {
                    // Decide the start destination once; later changes are handled by navigation.
                    val onboarded = remember { current.onboarded }
                    ProvideAppLanguage(current.language.resolve()) {
                        AppNavHost(onboarded = onboarded)
                    }
                }
            }
        }
    }
}
