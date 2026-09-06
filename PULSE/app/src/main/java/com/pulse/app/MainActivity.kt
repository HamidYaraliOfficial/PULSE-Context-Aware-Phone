package com.pulse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pulse.app.core.theme.PulseTheme
import com.pulse.app.core.theme.ThemeManager
import com.pulse.app.core.theme.ThemePreferences
import com.pulse.app.ui.PulseApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var themeManager: ThemeManager

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by themeManager.preferences.collectAsState(initial = ThemePreferences())
            PulseTheme(themePreferences = prefs) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PulseApp()
                }
            }
        }
    }
}
