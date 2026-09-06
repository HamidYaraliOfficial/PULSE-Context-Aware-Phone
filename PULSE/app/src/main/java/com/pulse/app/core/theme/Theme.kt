package com.pulse.app.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Resolves the effective [ColorScheme] for a given (variant, darkModePreference,
 * dynamicColor) combination and applies it via [MaterialTheme]. This is the
 * single entry point every screen in PULSE composes under.
 */
@Composable
fun PulseTheme(
    themePreferences: ThemePreferences,
    content: @Composable () -> Unit,
) {
    val useDark = when (themePreferences.darkMode) {
        DarkModePreference.SYSTEM -> isSystemInDarkTheme()
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme: ColorScheme = when {
        themePreferences.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> when (themePreferences.variant) {
            ThemeVariant.WINDOWS_DEFAULT -> if (useDark) WindowsDarkColors else WindowsLightColors
            ThemeVariant.RED -> if (useDark) RedDarkColors else RedLightColors
            ThemeVariant.BLUE -> if (useDark) BlueDarkColors else BlueLightColors
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        shapes = PulseShapes,
        content = content,
    )
}
