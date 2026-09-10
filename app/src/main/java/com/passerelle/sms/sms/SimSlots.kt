package com.passerelle.sms.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

object SimSlots {
    const val DEFAULT_ID = -1

    fun options(context: Context): List<SimOption> {
        val fallback = listOf(SimOption(DEFAULT_ID, "SIM par défaut"))
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return fallback
        }
        val manager = context.getSystemService(SubscriptionManager::class.java) ?: return fallback
        val infos = runCatching { manager.activeSubscriptionInfoList }.getOrNull().orEmpty()
        if (infos.isEmpty()) return fallback
        val sims = infos.sortedBy { it.simSlotIndex }.map { info ->
            val slot = info.simSlotIndex + 1
            val name = info.displayName?.toString()?.trim().orEmpty()
            val carrier = info.carrierName?.toString()?.trim().orEmpty()
            val label = when {
                name.isNotBlank() -> "SIM $slot · $name"
                carrier.isNotBlank() -> "SIM $slot · $carrier"
                else -> "SIM $slot"
            }
            SimOption(info.subscriptionId, label)
        }
        return if (sims.size == 1) {
            listOf(SimOption(sims.first().subscriptionId, "SIM unique"))
        } else {
            listOf(SimOption(DEFAULT_ID, "SIM par défaut")) + sims
        }
    }

    fun labelFor(options: List<SimOption>, subscriptionId: Int): String {
        return options.firstOrNull { it.subscriptionId == subscriptionId }?.label
            ?: options.firstOrNull { it.subscriptionId == DEFAULT_ID }?.label
            ?: "SIM par défaut"
    }
}
