package com.passerelle.sms.data

import kotlinx.coroutines.flow.Flow

class SmsRepository(private val dao: SmsDao) {
    fun observeAll(): Flow<List<SmsJob>> = dao.observeAll()

    suspend fun listAll(): List<SmsJob> = dao.listAll()

    suspend fun getById(id: Long): SmsJob? = dao.getById(id)

    suspend fun nextPending(now: Long = System.currentTimeMillis()): SmsJob? =
        dao.nextPending(now)

    suspend fun enqueue(tel: String, message: String): SmsJob {
        val id = dao.insert(
            SmsJob(
                tel = tel,
                message = message,
                status = SmsStatus.PENDING.apiValue
            )
        )
        return dao.getById(id) ?: error("Impossible de relire le SMS #$id")
    }

    suspend fun markSending(job: SmsJob): SmsJob {
        val updated = job.copy(
            status = SmsStatus.SENDING.apiValue,
            error = null,
            updatedAt = System.currentTimeMillis(),
            attempt = job.attempt + 1,
            nextAttemptAt = 0
        )
        dao.updateProgress(
            id = updated.id,
            status = updated.status,
            error = null,
            updatedAt = updated.updatedAt,
            attempt = updated.attempt,
            nextAttemptAt = 0
        )
        return updated
    }

    suspend fun markSent(job: SmsJob) {
        dao.updateProgress(
            id = job.id,
            status = SmsStatus.SENT.apiValue,
            error = null,
            updatedAt = System.currentTimeMillis(),
            attempt = job.attempt,
            nextAttemptAt = 0
        )
    }

    suspend fun markRetry(job: SmsJob, error: String, nextAttemptAt: Long) {
        dao.updateProgress(
            id = job.id,
            status = SmsStatus.PENDING.apiValue,
            error = error,
            updatedAt = System.currentTimeMillis(),
            attempt = job.attempt,
            nextAttemptAt = nextAttemptAt
        )
    }

    suspend fun markFailed(job: SmsJob, error: String) {
        dao.updateProgress(
            id = job.id,
            status = SmsStatus.FAILED.apiValue,
            error = error,
            updatedAt = System.currentTimeMillis(),
            attempt = job.attempt.coerceAtLeast(1),
            nextAttemptAt = 0
        )
    }

    suspend fun retryNow(id: Long) {
        val job = dao.getById(id) ?: return
        dao.updateProgress(
            id = job.id,
            status = SmsStatus.PENDING.apiValue,
            error = null,
            updatedAt = System.currentTimeMillis(),
            attempt = 0,
            nextAttemptAt = 0
        )
    }

    suspend fun recoverStuckSending() {
        dao.recoverStuckSending(System.currentTimeMillis())
    }

    suspend fun counts(): Map<String, Int> = mapOf(
        SmsStatus.PENDING.apiValue to dao.countByStatus(SmsStatus.PENDING.apiValue),
        SmsStatus.SENDING.apiValue to dao.countByStatus(SmsStatus.SENDING.apiValue),
        SmsStatus.SENT.apiValue to dao.countByStatus(SmsStatus.SENT.apiValue),
        SmsStatus.FAILED.apiValue to dao.countByStatus(SmsStatus.FAILED.apiValue)
    )
}
