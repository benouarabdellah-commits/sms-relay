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
    }
}
