package com.pulse.app.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android Keystore (via Jetpack Security's AES256-GCM MasterKey) for
 * any secret that must never sit in plain SharedPreferences — most notably
 * an optional Cloud AI provider API key, which the spec explicitly requires
 * to never be hardcoded in the APK. Nothing is stored here unless the user
 * explicitly configures a Cloud AI provider in Settings → AI; the on-device
 * inference path (ContextInferenceEngine) has no secret material at all.
 */
@Singleton
class KeystoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "pulse_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun putSecret(key: String, value: String) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    fun getSecret(key: String): String? = encryptedPrefs.getString(key, null)

    fun clearSecret(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }
}
