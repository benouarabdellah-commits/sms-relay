package com.passerelle.sms.data

enum class SmsStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED;

    val apiValue: String
        get() = when (this) {
            PENDING -> "pending"
            SENDING -> "sending"
            SENT -> "sent"
            FAILED -> "failed"
        }

    val labelFr: String
        get() = when (this) {
            PENDING -> "En attente"
            SENDING -> "En cours"
            SENT -> "Envoyé"
            FAILED -> "Échec"
        }

    companion object {
        fun fromApi(value: String?): SmsStatus? {
            if (value.isNullOrBlank()) return null
            return entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
        }
    }
}
