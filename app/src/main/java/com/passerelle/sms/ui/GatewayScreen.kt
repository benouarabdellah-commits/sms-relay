package com.passerelle.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passerelle.sms.data.SmsJob
import com.passerelle.sms.data.SmsStatus
import com.passerelle.sms.sms.SimOption
import com.passerelle.sms.ui.theme.Busy
import com.passerelle.sms.ui.theme.BusySoft
import com.passerelle.sms.ui.theme.Danger
import com.passerelle.sms.ui.theme.DangerSoft
import com.passerelle.sms.ui.theme.Foam
import com.passerelle.sms.ui.theme.Ink
import com.passerelle.sms.ui.theme.Muted
import com.passerelle.sms.ui.theme.Ok
import com.passerelle.sms.ui.theme.OkSoft
import com.passerelle.sms.ui.theme.Pine
import com.passerelle.sms.ui.theme.Wait
import com.passerelle.sms.ui.theme.WaitSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GatewayScreen(
    jobs: List<SmsJob>,
    running: Boolean,
    lastError: String?,
    listenUrl: String?,
    localIps: List<String>,
    apiKey: String,
    port: Int,
    hasSmsPermission: Boolean,
    simOptions: List<SimOption>,
    selectedSimId: Int,
    sendDelayMs: Int,
    autoRetry: Boolean,
    maxAttempts: Int,
    onToggle: (Boolean) -> Unit,
    onCopy: (String, String) -> Unit,
    onRegenerateKey: () -> Unit,
    onRequestPermission: () -> Unit,
    onSelectSim: (Int) -> Unit,
    onDelayChange: (Int) -> Unit,
    onAutoRetryChange: (Boolean) -> Unit,
    onMaxAttemptsChange: (Int) -> Unit,
    onRetryJob: (Long) -> Unit
) {
    var filter by remember { mutableStateOf<SmsStatus?>(null) }
    val visible = jobs.filter {
        when (filter) {
            null -> true
            SmsStatus.PENDING -> it.statusEnum == SmsStatus.PENDING || it.statusEnum == SmsStatus.SENDING
            else -> it.statusEnum == filter
        }
    }
    val pending = jobs.count { it.statusEnum == SmsStatus.PENDING || it.statusEnum == SmsStatus.SENDING }
    val sent = jobs.count { it.statusEnum == SmsStatus.SENT }
    val failed = jobs.count { it.statusEnum == SmsStatus.FAILED }
    val url = listenUrl ?: localIps.firstOrNull()?.let { "http://$it:$port" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Passerelle SMS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                Text(
                    "Le PC envoie un POST sur le Wi-Fi local. Le téléphone enfile les SMS et les expédie.",
                    color = Muted,
                    fontSize = 15.sp
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Ink),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Pine),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Wifi, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (running) "Écoute le réseau local" else "Arrêtée",
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (running) "Prête à recevoir les POST du PC" else "Démarrez pour accepter les SMS",
                                color = androidx.compose.ui.graphics.Color(0xFFB7D5CE),
                                fontSize = 13.sp
                            )
                        }
                        Switch(
                            checked = running,
                            onCheckedChange = onToggle,
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = TealSafe,
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
                    }
                    if (url != null) {
                        InfoLine("URL", url, onCopy)
                    }
                    InfoLine("Jeton", apiKey, onCopy)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { url?.let { onCopy(it, "URL") } }) {
                            Text("Copier l’URL", color = androidx.compose.ui.graphics.Color(0xFF9EE0D0))
                        }
                        TextButton(onClick = onRegenerateKey) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF9EE0D0), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Nouveau jeton", color = androidx.compose.ui.graphics.Color(0xFF9EE0D0))
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(
                simOptions = simOptions,
                selectedSimId = selectedSimId,
                sendDelayMs = sendDelayMs,
                autoRetry = autoRetry,
                maxAttempts = maxAttempts,
                onSelectSim = onSelectSim,
                onDelayChange = onDelayChange,
                onAutoRetryChange = onAutoRetryChange,
                onMaxAttemptsChange = onMaxAttemptsChange
            )
        }

        if (!hasSmsPermission) {
            item {
                Banner(
                    title = "Permission SMS requise",
                    body = "Sans cette autorisation, les messages restent en file et passent en échec.",
                    action = "Autoriser",
                    onAction = onRequestPermission,
                    danger = true
                )
            }
        }

        if (lastError != null) {
            item {
                Banner(title = "Impossible d’écouter", body = lastError, action = null, onAction = {}, danger = true)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatChip("En file", pending.toString(), WaitSoft, Wait, Modifier.weight(1f))
                StatChip("Envoyés", sent.toString(), OkSoft, Ok, Modifier.weight(1f))
                StatChip("Échecs", failed.toString(), DangerSoft, Danger, Modifier.weight(1f))
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusFilter("Tous", filter == null) { filter = null }
                StatusFilter("En attente", filter == SmsStatus.PENDING) { filter = SmsStatus.PENDING }
                StatusFilter("Envoyés", filter == SmsStatus.SENT) { filter = SmsStatus.SENT }
                StatusFilter("Échecs", filter == SmsStatus.FAILED) { filter = SmsStatus.FAILED }
            }
        }

        if (visible.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Foam)
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Aucun SMS pour l’instant", fontWeight = FontWeight.Medium, color = Ink)
                        Text(
                            "Depuis le PC, ouvrez l’URL ci-dessus ou envoyez un POST JSON avec le numéro et le message. Les SMS s’empilent ici : en attente, puis envoyé.",
                            color = Muted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(visible, key = { it.id }) { job ->
                JobCard(job = job, maxAttempts = maxAttempts, onRetry = onRetryJob)
            }
        }
    }
}

private val TealSafe = androidx.compose.ui.graphics.Color(0xFF1B8A74)

@Composable
private fun SettingsCard(
    simOptions: List<SimOption>,
    selectedSimId: Int,
    sendDelayMs: Int,
    autoRetry: Boolean,
    maxAttempts: Int,
    onSelectSim: (Int) -> Unit,
    onDelayChange: (Int) -> Unit,
    onAutoRetryChange: (Boolean) -> Unit,
    onMaxAttemptsChange: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Envoi", fontWeight = FontWeight.Medium, color = Ink)
            Text("Carte SIM", color = Muted, fontSize = 13.sp)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(simOptions, key = { it.subscriptionId }) { option ->
                    StatusFilter(option.label, selectedSimId == option.subscriptionId) {
                        onSelectSim(option.subscriptionId)
                    }
                }
            }
            Text("Délai entre deux SMS", color = Muted, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 5, 10).forEach { seconds ->
                    StatusFilter("${seconds}s", sendDelayMs == seconds * 1000) {
                        onDelayChange(seconds * 1000)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Relance automatique", fontWeight = FontWeight.Medium, color = Ink)
                    Text(
                        if (autoRetry) "Jusqu’à $maxAttempts essais en cas d’échec" else "Un seul essai, puis échec",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = autoRetry,
                    onCheckedChange = onAutoRetryChange,
                    colors = SwitchDefaults.colors(checkedTrackColor = Pine)
                )
            }
            if (autoRetry) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 5).forEach { attempts ->
                        StatusFilter("$attempts essais", maxAttempts == attempts) {
                            onMaxAttemptsChange(attempts)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String, onCopy: (String, String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label, color = androidx.compose.ui.graphics.Color(0xFF8FB5AC), fontSize = 12.sp)
            Text(
                value,
                color = androidx.compose.ui.graphics.Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = { onCopy(value, label) }) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copier $label", tint = androidx.compose.ui.graphics.Color.White)
        }
    }
}

@Composable
private fun Banner(
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit,
    danger: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (danger) DangerSoft else WaitSoft)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Medium, color = if (danger) Danger else Wait)
            Text(body, color = Ink, fontSize = 14.sp)
            if (action != null) {
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = if (danger) Danger else Pine)
                ) { Text(action) }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.padding(14.dp)) {
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = fg)
            Text(label, color = Ink, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatusFilter(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Pine,
            selectedLabelColor = androidx.compose.ui.graphics.Color.White
        )
    )
}

@Composable
private fun JobCard(job: SmsJob, maxAttempts: Int, onRetry: (Long) -> Unit) {
    val (bg, fg) = when (job.statusEnum) {
        SmsStatus.PENDING -> WaitSoft to Wait
        SmsStatus.SENDING -> BusySoft to Busy
        SmsStatus.SENT -> OkSoft to Ok
        SmsStatus.FAILED -> DangerSoft to Danger
    }
    val time = remember(job.updatedAt) {
        SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date(job.updatedAt))
    }
    val statusLabel = when {
        job.statusEnum == SmsStatus.PENDING && job.attempt > 0 ->
            "Relance ${job.attempt}/$maxAttempts"
        job.statusEnum == SmsStatus.SENDING ->
            "En cours · essai ${job.attempt.coerceAtLeast(1)}/$maxAttempts"
        else -> job.statusEnum.labelFr
    }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.tel, fontWeight = FontWeight.Medium, color = Ink, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(bg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusLabel, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Text(job.message, color = Ink, fontSize = 15.sp)
            Text(time, color = Muted, fontSize = 12.sp)
            if (!job.error.isNullOrBlank()) {
                Text(job.error, color = if (job.statusEnum == SmsStatus.FAILED) Danger else Wait, fontSize = 13.sp)
            }
            if (job.statusEnum == SmsStatus.FAILED) {
                TextButton(onClick = { onRetry(job.id) }) {
                    Text("Relancer")
                }
            }
        }
    }
}
