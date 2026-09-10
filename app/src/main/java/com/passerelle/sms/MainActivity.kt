package com.passerelle.sms

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.passerelle.sms.service.GatewayService
import com.passerelle.sms.service.GatewayState
import com.passerelle.sms.sms.SimSlots
import com.passerelle.sms.ui.GatewayScreen
import com.passerelle.sms.ui.theme.PasserelleTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PasserelleApp

        setContent {
            PasserelleTheme {
                val jobs by app.repository.observeAll().collectAsStateWithLifecycle(emptyList())
                val running by GatewayState.running.collectAsStateWithLifecycle()
                val error by GatewayState.lastError.collectAsStateWithLifecycle()
                val url by GatewayState.listenUrl.collectAsStateWithLifecycle()
                val ips by GatewayState.localIps.collectAsStateWithLifecycle()
                var apiKey by remember { mutableStateOf(app.settings.apiKey) }
                var hasSmsPermission by remember { mutableStateOf(hasSms()) }
                var smsAsked by remember { mutableStateOf(false) }
                var smsPermanentlyDenied by remember { mutableStateOf(false) }
                var extrasAsked by remember { mutableStateOf(false) }
                var simOptions by remember { mutableStateOf(SimSlots.options(this)) }
                var selectedSimId by remember {
                    val options = SimSlots.options(this)
                    val saved = app.settings.subscriptionId
                    val resolved = if (options.any { it.subscriptionId == saved }) {
                        saved
                    } else {
                        options.first().subscriptionId
                    }
                    app.settings.subscriptionId = resolved
                    mutableIntStateOf(resolved)
                }
                var sendDelayMs by remember { mutableIntStateOf(app.settings.sendDelayMs) }
                var autoRetry by remember { mutableStateOf(app.settings.autoRetry) }
                var maxAttempts by remember { mutableIntStateOf(app.settings.effectiveMaxAttempts()) }

                val extraPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) {
                    simOptions = SimSlots.options(this)
                    if (simOptions.none { it.subscriptionId == selectedSimId }) {
                        selectedSimId = simOptions.first().subscriptionId
                        app.settings.subscriptionId = selectedSimId
                    }
                }

                val smsLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasSmsPermission = granted || hasSms()
                    smsAsked = true
                    smsPermanentlyDenied = !hasSmsPermission &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.SEND_SMS
                        )
                    if (hasSmsPermission) {
                        extrasAsked = true
                        val extras = extraPermissions()
                        if (extras.isNotEmpty()) extraPermissionLauncher.launch(extras)
                        if (app.settings.gatewayWanted && !running) startGateway()
                    }
                }

                fun askSmsNow(openSettingsIfBlocked: Boolean = false) {
                    if (hasSms()) {
                        hasSmsPermission = true
                        smsPermanentlyDenied = false
                        return
                    }
                    val canShowDialog = !smsAsked ||
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.SEND_SMS
                        )
                    if (canShowDialog) {
                        smsLauncher.launch(Manifest.permission.SEND_SMS)
                        return
                    }
                    smsPermanentlyDenied = true
                    if (openSettingsIfBlocked) openAppSettings()
                }

                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    hasSmsPermission = hasSms()
                    if (hasSmsPermission) {
                        smsPermanentlyDenied = false
                        val extras = extraPermissions()
                        if (!extrasAsked && extras.isNotEmpty()) {
                            extrasAsked = true
                            extraPermissionLauncher.launch(extras)
                        }
                    } else if (!smsAsked) {
                        askSmsNow()
                    } else {
                        smsPermanentlyDenied = !ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.SEND_SMS
                        )
                    }
                }

                GatewayScreen(
                    jobs = jobs,
                    running = running,
                    lastError = error,
                    listenUrl = url,
                    localIps = ips,
                    apiKey = apiKey,
                    port = app.settings.port,
                    hasSmsPermission = hasSmsPermission,
                    simOptions = simOptions,
                    selectedSimId = selectedSimId,
                    sendDelayMs = sendDelayMs,
                    autoRetry = autoRetry,
                    maxAttempts = maxAttempts,
                    onToggle = { enabled ->
                        if (enabled) {
                            if (!hasSms()) {
                                askSmsNow(openSettingsIfBlocked = true)
                            } else {
                                startGateway()
                            }
                        } else {
                            stopGateway()
                        }
                    },
                    onCopy = { value, label -> copy(value, label) },
                    onRegenerateKey = {
                        apiKey = app.settings.regenerateApiKey()
                        if (running) {
                            stopGateway()
                            startGateway()
                        }
                        Toast.makeText(this, "Nouveau jeton généré", Toast.LENGTH_SHORT).show()
                    },
                    onRequestPermission = { askSmsNow(openSettingsIfBlocked = true) },
                    onSelectSim = { id ->
                        app.settings.subscriptionId = id
                        selectedSimId = id
                    },
                    onDelayChange = { ms ->
                        app.settings.sendDelayMs = ms
                        sendDelayMs = ms
                    },
                    onAutoRetryChange = { enabled ->
                        app.settings.autoRetry = enabled
                        autoRetry = enabled
                        maxAttempts = app.settings.effectiveMaxAttempts()
                    },
                    onMaxAttemptsChange = { attempts ->
                        app.settings.autoRetry = true
                        app.settings.maxAttempts = attempts
                        autoRetry = true
                        maxAttempts = attempts
                    },
                    onRetryJob = { id ->
                        lifecycleScope.launch { app.repository.retryNow(id) }
                    },
                    smsPermanentlyDenied = smsPermanentlyDenied,
                    onOpenSettings = { openAppSettings() }
                )
            }
        }
    }

    private fun startGateway() {
        val intent = Intent(this, GatewayService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopGateway() {
        val intent = Intent(this, GatewayService::class.java).setAction(GatewayService.ACTION_STOP)
        startService(intent)
    }

    private fun copy(value: String, label: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "$label copié", Toast.LENGTH_SHORT).show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun hasSms(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun extraPermissions(): Array<String> {
        val list = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            list += Manifest.permission.READ_PHONE_STATE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            list += Manifest.permission.POST_NOTIFICATIONS
        }
        return list.toTypedArray()
    }
}
