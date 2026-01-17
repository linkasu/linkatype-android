package ru.ibakaidov.distypepro.shared.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

actual typealias PlatformContext = Context

actual class SecureTokenStorage actual constructor(context: PlatformContext) {
    private val preferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "linka_secure_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    actual fun get(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    actual fun set(value: String?) {
        if (value == null) {
            preferences.edit().remove(KEY_REFRESH_TOKEN).apply()
        } else {
            preferences.edit().putString(KEY_REFRESH_TOKEN, value).apply()
        }
    }

    actual fun clear() {
        preferences.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    private companion object {
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
