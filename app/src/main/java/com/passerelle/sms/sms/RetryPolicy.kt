package com.passerelle.sms.sms

data class SimOption(
    val subscriptionId: Int,
    val label: String
)

object RetryPolicy {
    fun shouldRetry(attempt: Int, maxAttempts: Int): Boolean =
        attempt < maxAttempts.coerceAtLeast(1)

    fun nextAttemptAt(nowMs: Long, delayMs: Int): Long =
        nowMs + delayMs.coerceAtLeast(0)

    fun retryNote(error: String, attempt: Int, maxAttempts: Int, delayMs: Int): String {
        val seconds = (delayMs / 1000).coerceAtLeast(1)
        return "$error — nouvelle tentative ${attempt + 1}/$maxAttempts dans ${seconds}s"
    }
}
