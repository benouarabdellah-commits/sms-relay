package com.passerelle.sms.data

import android.content.Context
import java.security.SecureRandom

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("passerelle_settings", Context.MODE_PRIVATE)

    val port: Int = 8765

    val apiKey: String
        get() {
            val existing = prefs.getString(KEY_API, null)
            if (!existing.isNullOrBlank()) return existing
            val generated = generateKey()
            prefs.edit().putString(KEY_API, generated).apply()
            return generated
        }

    var gatewayWanted: Boolean
        get() = prefs.getBoolean(KEY_WANTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_WANTED, value).apply()
        }

    var sendDelayMs: Int
        get() = prefs.getInt(KEY_DELAY, 2_000).coerceIn(1_000, 30_000)
        set(value) {
            prefs.edit().putInt(KEY_DELAY, value.coerceIn(1_000, 30_000)).apply()
        }

    var maxAttempts: Int
        get() = prefs.getInt(KEY_ATTEMPTS, 3).coerceIn(1, 5)
        set(value) {
            prefs.edit().putInt(KEY_ATTEMPTS, value.coerceIn(1, 5)).apply()
        }

    var autoRetry: Boolean
        get() = prefs.getBoolean(KEY_RETRY, true)
        set(value) {
            prefs.edit().putBoolean(KEY_RETRY, value).apply()
            if (value && maxAttempts < 2) maxAttempts = 3
            if (!value) maxAttempts = 1
        }

    var subscriptionId: Int
        get() = prefs.getInt(KEY_SIM, -1)
        set(value) {
            prefs.edit().putInt(KEY_SIM, value).apply()
        }

    fun effectiveMaxAttempts(): Int = if (autoRetry) maxAttempts.coerceAtLeast(2) else 1

    fun regenerateApiKey(): String {
        val generated = generateKey()
        prefs.edit().putString(KEY_API, generated).apply()
        return generated
    }

    private fun generateKey(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
        return buildString(20) {
            repeat(4) { block ->
                if (block > 0) append('-')
                repeat(4) { append(alphabet[random.nextInt(alphabet.length)]) }
            }
        }
    }

    companion object {
        private const val KEY_API = "api_key"
        private const val KEY_WANTED = "gateway_wanted"
        private const val KEY_DELAY = "send_delay_ms"
        private const val KEY_ATTEMPTS = "max_attempts"
        private const val KEY_RETRY = "auto_retry"
        private const val KEY_SIM = "subscription_id"
    }
}
