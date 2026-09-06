package com.pulse.app.core.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.themeDataStore by preferencesDataStore(name = "pulse_theme_prefs")

data class AppLanguage(val tag: String, val displayNameKey: String, val isRtl: Boolean) {
    companion object {
        val ENGLISH = AppLanguage("en", "lang_english", isRtl = false)
        val PERSIAN = AppLanguage("fa", "lang_persian", isRtl = true)
        val CHINESE = AppLanguage("zh", "lang_chinese", isRtl = false)
        val ALL = listOf(ENGLISH, PERSIAN, CHINESE)
        fun fromTag(tag: String): AppLanguage = ALL.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

data class ThemePreferences(
    val variant: ThemeVariant = ThemeVariant.WINDOWS_DEFAULT,
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val dynamicColor: Boolean = false,
    val languageTag: String = "en",
)

/**
 * Persists appearance + language choices across launches. Language switching
 * is applied via [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales]
 * from the UI layer (see LanguageSettingsScreen) — this class is the single
 * source of truth that both the Activity recreation path and Compose theming
 * read from.
 */
@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val VARIANT = stringPreferencesKey("theme_variant")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LANGUAGE = stringPreferencesKey("language_tag")
    }

    val preferences: Flow<ThemePreferences> = context.themeDataStore.data.map { prefs ->
        ThemePreferences(
            variant = prefs[Keys.VARIANT]?.let { runCatching { ThemeVariant.valueOf(it) }.getOrNull() }
                ?: ThemeVariant.WINDOWS_DEFAULT,
            darkMode = prefs[Keys.DARK_MODE]?.let { runCatching { DarkModePreference.valueOf(it) }.getOrNull() }
                ?: DarkModePreference.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            languageTag = prefs[Keys.LANGUAGE] ?: "en",
        )
    }

    suspend fun setVariant(variant: ThemeVariant) {
        context.themeDataStore.edit { it[Keys.VARIANT] = variant.name }
    }

    suspend fun setDarkMode(mode: DarkModePreference) {
        context.themeDataStore.edit { it[Keys.DARK_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.themeDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setLanguage(tag: String) {
        context.themeDataStore.edit { it[Keys.LANGUAGE] = tag }
    }
}
