package com.passerelle.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.passerelle.sms.ui.theme.Foam
import com.passerelle.sms.ui.theme.Ink
import com.passerelle.sms.ui.theme.Muted
import com.passerelle.sms.ui.theme.Pine

@Composable
fun SmsPermissionGate(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Foam)
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Pine),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Sms,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            "Autoriser les SMS",
            modifier = Modifier.padding(top = 24.dp),
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            textAlign = TextAlign.Center
        )
        Text(
            if (permanentlyDenied) {
                "Android a bloqué la fenêtre. Ouvrez les paramètres de l’appli, puis activez SMS / Messages."
            } else {
                "Passerelle SMS a besoin d’envoyer des messages. Touchez Autoriser sur la fenêtre Android qui s’affiche."
            },
            modifier = Modifier.padding(top = 10.dp, bottom = 28.dp),
            color = Muted,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = if (permanentlyDenied) onOpenSettings else onAllow,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Pine)
        ) {
            Text(if (permanentlyDenied) "Ouvrir les paramètres" else "Autoriser les SMS")
        }
        if (!permanentlyDenied) {
            TextButton(onClick = onOpenSettings) {
                Text("Ouvrir les paramètres de l’appli")
            }
        }
    }
}
