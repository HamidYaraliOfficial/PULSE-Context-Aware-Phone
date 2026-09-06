package com.pulse.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pulse.app.R
import com.pulse.app.core.theme.AppLanguage
import com.pulse.app.core.theme.DarkModePreference
import com.pulse.app.core.theme.ThemeManager
import com.pulse.app.core.theme.ThemePreferences
import com.pulse.app.core.theme.ThemeVariant
import com.pulse.app.core.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceSettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
) : ViewModel() {
    val preferences: StateFlow<ThemePreferences> =
        themeManager.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemePreferences())

    fun setVariant(variant: ThemeVariant) = viewModelScope.launch { themeManager.setVariant(variant) }
    fun setDarkMode(mode: DarkModePreference) = viewModelScope.launch { themeManager.setDarkMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { themeManager.setDynamicColor(enabled) }
    fun setLanguage(tag: String) = viewModelScope.launch {
        themeManager.setLanguage(tag)
        LocaleManager.applyLanguage(tag)
    }
}

@Composable
fun AppearanceSettingsScreen(viewModel: AppearanceSettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val themeOptions = listOf(
            ThemeVariant.WINDOWS_DEFAULT to R.string.theme_windows_default,
            ThemeVariant.RED to R.string.theme_red,
            ThemeVariant.BLUE to R.string.theme_blue,
        )
        themeOptions.forEach { (variant, labelRes) ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = prefs.variant == variant, onClick = { viewModel.setVariant(variant) })
                Text(stringResource(labelRes))
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_dark_mode), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        val darkOptions = listOf(
            DarkModePreference.SYSTEM to R.string.theme_system,
            DarkModePreference.LIGHT to R.string.theme_light,
            DarkModePreference.DARK to R.string.theme_dark,
        )
        darkOptions.forEach { (mode, labelRes) ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = prefs.darkMode == mode, onClick = { viewModel.setDarkMode(mode) })
                Text(stringResource(labelRes))
            }
        }

        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.settings_dynamic_color))
            Switch(checked = prefs.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        AppLanguage.ALL.forEach { lang ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = prefs.languageTag == lang.tag, onClick = { viewModel.setLanguage(lang.tag) })
                Text(stringResource(id = lang.displayNameResId()))
            }
        }
    }
}

private fun AppLanguage.displayNameResId(): Int = when (this.tag) {
    "fa" -> R.string.lang_persian
    "zh" -> R.string.lang_chinese
    else -> R.string.lang_english
}
