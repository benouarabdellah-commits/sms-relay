package com.passerelle.sms.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_jobs")
data class SmsJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tel: String,
    val message: String,
    val status: String = SmsStatus.PENDING.apiValue,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val attempt: Int = 0,
    val nextAttemptAt: Long = 0
) {
    val statusEnum: SmsStatus
        get() = SmsStatus.fromApi(status) ?: SmsStatus.PENDING
}
