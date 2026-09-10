package com.passerelle.sms.sms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

class SmsSender(private val context: Context) {
    suspend fun send(jobId: Long, tel: String, message: String) {
        val smsManager = smsManager()
        val parts = smsManager.divideMessage(message)
        if (parts.isNullOrEmpty()) {
            throw SmsSendException("Message vide après découpage")
        }

        withTimeout(45_000) {
            suspendCancellableCoroutine { cont ->
                val action = "$SENT_ACTION.$jobId"
                val filter = IntentFilter(action)
                val receiver = object : BroadcastReceiver() {
                    private var remaining = parts.size
                    private var failed: String? = null

                    override fun onReceive(ctx: Context?, intent: Intent?) {
                        val error = resultToError()
                        if (error != null) failed = error
                        remaining -= 1
                        if (remaining <= 0) {
                            runCatching { context.unregisterReceiver(this) }
                            if (cont.isActive) {
                                val reason = failed
                                if (reason == null) cont.resume(Unit)
                                else cont.resumeWith(Result.failure(SmsSendException(reason)))
                            }
                        }
                    }

                    private fun resultToError(): String? = when (resultCode) {
                        android.app.Activity.RESULT_OK -> null
                        SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Échec générique de l'opérateur"
                        SmsManager.RESULT_ERROR_NO_SERVICE -> "Aucun service réseau"
                        SmsManager.RESULT_ERROR_NULL_PDU -> "PDU invalide"
                        SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio éteinte"
                        else -> "Erreur d'envoi ($resultCode)"
                    }
                }

                ContextCompat.registerReceiver(
                    context,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )

                cont.invokeOnCancellation {
                    runCatching { context.unregisterReceiver(receiver) }
                }

                try {
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    val sentIntents = ArrayList<PendingIntent>(parts.size)
                    repeat(parts.size) { index ->
                        val intent = Intent(action).setPackage(context.packageName)
                        sentIntents += PendingIntent.getBroadcast(
                            context,
                            (jobId * 10 + index).toInt(),
                            intent,
                            flags
                        )
                    }
                    if (parts.size == 1) {
                        smsManager.sendTextMessage(tel, null, parts[0], sentIntents[0], null)
                    } else {
                        smsManager.sendMultipartTextMessage(tel, null, parts, sentIntents, null)
                    }
                } catch (t: Throwable) {
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) {
                        cont.resumeWith(
                            Result.failure(
                                SmsSendException(t.message ?: "Impossible d'envoyer le SMS")
                            )
                        )
                    }
                }
            }
        }
    }

    private fun smsManager(): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    companion object {
        private const val SENT_ACTION = "com.passerelle.sms.SMS_SENT"
    }
}

class SmsSendException(message: String) : Exception(message)
