package com.pulse.app.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * PULSE ships four visual themes, each in Light and Dark, inspired by the
 * Windows 11 Fluent design language (Mica surfaces, rounded 8dp corners,
 * subtle acrylic-like elevation, restrained accent usage):
 *
 *  - Windows Default → Windows 11's stock accent blue
 *  - Red             → high-contrast red accent
 *  - Blue            → deeper "ocean" blue accent
 *
 * "Dark" / "Light" is an independent toggle (or "Follow system") layered on
 * top of whichever accent variant is selected — see [ThemeManager].
 */
enum class ThemeVariant { WINDOWS_DEFAULT, RED, BLUE }

enum class DarkModePreference { SYSTEM, LIGHT, DARK }

// ---------------------------------------------------------------------
// Windows Default (Fluent accent blue #0067C0)
// ---------------------------------------------------------------------
private val WinAccent = Color(0xFF0067C0)
private val WinAccentLight = Color(0xFF60CDFF)
private val WinOnAccent = Color(0xFFFFFFFF)

val WindowsLightColors = lightColorScheme(
    primary = WinAccent,
    onPrimary = WinOnAccent,
    primaryContainer = Color(0xFFD3E4FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF565F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFBFBFB),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFE7E0EC),
    outline = Color(0xFF767680),
    error = Color(0xFFC42B1C),
)

val WindowsDarkColors = darkColorScheme(
    primary = WinAccentLight,
    onPrimary = Color(0xFF00325A),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759),
    background = Color(0xFF202020),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF282828),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF90909A),
    error = Color(0xFFFFB4A9),
)

// ---------------------------------------------------------------------
// Red accent (#C42B1C — Windows 11 "Red" personalization accent)
// ---------------------------------------------------------------------
val RedLightColors = lightColorScheme(
    primary = Color(0xFFC42B1C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD3),
    onPrimaryContainer = Color(0xFF410100),
    secondary = Color(0xFF77574E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCF),
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFBFBFB),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFF5DED8),
    outline = Color(0xFF857370),
    error = Color(0xFFBA1A1A),
)

val RedDarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A6),
    onPrimary = Color(0xFF690100),
    primaryContainer = Color(0xFF930900),
    onPrimaryContainer = Color(0xFFFFDAD3),
    secondary = Color(0xFFE7BDB1),
    onSecondary = Color(0xFF442A21),
    secondaryContainer = Color(0xFF5D4036),
    background = Color(0xFF202020),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF282828),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF534341),
    outline = Color(0xFFA08C88),
    error = Color(0xFFFFB4AB),
)

// ---------------------------------------------------------------------
// Blue accent (#0063B1 — deeper "ocean" blue)
// ---------------------------------------------------------------------
val BlueLightColors = lightColorScheme(
    primary = Color(0xFF0063B1),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF50606F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD4E4F6),
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFFBFBFB),
    onSurface = Color(0xFF1B1B1B),
    surfaceVariant = Color(0xFFDDE3EA),
    outline = Color(0xFF73777F),
    error = Color(0xFFBA1A1A),
)

val BlueDarkColors = darkColorScheme(
    primary = Color(0xFF9FCAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFB8CAD9),
    onSecondary = Color(0xFF22323F),
    secondaryContainer = Color(0xFF384956),
    background = Color(0xFF202020),
    onBackground = Color(0xFFE3E3E3),
    surface = Color(0xFF282828),
    onSurface = Color(0xFFE3E3E3),
    surfaceVariant = Color(0xFF41474D),
    outline = Color(0xFF8B9198),
    error = Color(0xFFFFB4AB),
)

/** Confidence indicator + context-card accent colors, theme-independent. */
object PulseSemanticColors {
    val ConfidenceHigh = Color(0xFF2E7D32)
    val ConfidenceMedium = Color(0xFFE8A100)
    val ConfidenceLow = Color(0xFFC42B1C)
    val FocusAccent = Color(0xFF6750A4)
    val DrivingAccent = Color(0xFFE8A100)
    val SleepAccent = Color(0xFF4A4FB1)
}
