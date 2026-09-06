package com.pulse.app.core.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Thin wrapper around [AppCompatDelegate.setApplicationLocales] — the
 * modern (API 33+ backed, backward-compatible via AppCompat on older
 * devices) way to change the app's language independent of the system
 * locale. Persian is automatically laid out right-to-left because its
 * resources live in values-fa and the `fa` locale is RTL in ICU; English
 * and Chinese resources are values/ and values-zh (both LTR).
 *
 * No Activity recreation dance is needed by callers — AppCompatDelegate
 * handles recreating the Activity itself.
 */
object LocaleManager {

    fun applyLanguage(tag: String) {
        val localeList = LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) "en" else locales[0]?.language ?: "en"
    }

    fun isRtl(tag: String): Boolean = tag == "fa"
}
