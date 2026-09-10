package com.passerelle.sms.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.passerelle.sms.MainActivity
import com.passerelle.sms.PasserelleApp
import com.passerelle.sms.R
import com.passerelle.sms.net.GatewayServer
import com.passerelle.sms.net.WifiInfo
import com.passerelle.sms.sms.RetryPolicy
import com.passerelle.sms.sms.SmsSendException
import com.passerelle.sms.sms.SmsSender
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GatewayService : LifecycleService() {
    private var server: GatewayServer? = null
    private var worker: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopGateway()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification("Démarrage…"),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        startGateway()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        stopGatewayInternal()
        super.onDestroy()
    }

    private fun startGateway() {
        val app = application as PasserelleApp
        val ips = WifiInfo.ipv4Addresses()
        GatewayState.setLocalIps(ips)
        if (ips.isEmpty()) {
            GatewayState.setError("Aucune adresse Wi-Fi. Connectez le téléphone au même réseau que le PC.")
            GatewayState.setRunning(false)
            stopSelf()
            return
        }

        val ip = ips.first()
        val port = app.settings.port
        val url = "http://$ip:$port"
        GatewayState.setListenUrl(url)
        GatewayState.setError(null)

        try {
            val http = GatewayServer(this, port, app.settings.apiKey, app.repository)
            http.start()
            server = http
        } catch (t: Throwable) {
            GatewayState.setError("Impossible de démarrer le serveur : ${t.message}")
            GatewayState.setRunning(false)
            stopSelf()
            return
        }

        acquireWakeLock()
        GatewayState.setRunning(true)
        app.settings.gatewayWanted = true
        updateNotification("Active — $url")
        startWorker()
    }

    private fun startWorker() {
        val app = application as PasserelleApp
        val sender = SmsSender(this)
        worker?.cancel()
        worker = lifecycleScope.launch(Dispatchers.IO) {
            app.repository.recoverStuckSending()
            while (isActive) {
                val next = app.repository.nextPending()
                if (next == null) {
                    delay(400)
                    continue
                }
                val sending = app.repository.markSending(next)
                val delayMs = app.settings.sendDelayMs
                val maxAttempts = app.settings.effectiveMaxAttempts()
                val subscriptionId = app.settings.subscriptionId
                try {
                    sender.send(sending.id, sending.tel, sending.message, subscriptionId)
                    app.repository.markSent(sending)
                } catch (t: CancellationException) {
                    app.repository.markFailed(sending, "Arrêté")
                    throw t
                } catch (t: Throwable) {
                    val reason = when {
                        t is SmsSendException -> t.message ?: "Échec d'envoi"
                        t.message?.contains("timeout", ignoreCase = true) == true -> "Délai dépassé"
                        else -> t.message ?: "Échec d'envoi"
                    }
                    if (RetryPolicy.shouldRetry(sending.attempt, maxAttempts)) {
                        val nextAt = RetryPolicy.nextAttemptAt(System.currentTimeMillis(), delayMs)
                        app.repository.markRetry(
                            sending,
                            RetryPolicy.retryNote(reason, sending.attempt, maxAttempts, delayMs),
                            nextAt
                        )
                    } else {
                        app.repository.markFailed(sending, reason)
                    }
                }
                delay(delayMs.toLong())
            }
        }
    }

    private fun stopGateway() {
        val app = application as PasserelleApp
        app.settings.gatewayWanted = false
        stopGatewayInternal()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopGatewayInternal() {
        worker?.cancel()
        worker = null
        runCatching { server?.stop() }
        server = null
        releaseWakeLock()
        GatewayState.setRunning(false)
        GatewayState.setListenUrl(null)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "passerelle:gateway").apply {
            setReferenceCounted(false)
            acquire(6 * 60 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Passerelle SMS",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Indique que le téléphone écoute le PC sur le Wi-Fi"
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, GatewayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_gateway)
            .setContentTitle("Passerelle SMS")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, "Arrêter", stop)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    companion object {
        const val ACTION_STOP = "com.passerelle.sms.STOP"
        private const val CHANNEL_ID = "gateway"
        private const val NOTIFICATION_ID = 21
    }
}
