package com.passerelle.sms.data

import kotlinx.coroutines.flow.Flow

class SmsRepository(private val dao: SmsDao) {
    fun observeAll(): Flow<List<SmsJob>> = dao.observeAll()

    suspend fun listAll(): List<SmsJob> = dao.listAll()

    suspend fun getById(id: Long): SmsJob? = dao.getById(id)

    suspend fun nextPending(): SmsJob? = dao.nextPending()

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

    suspend fun markSending(id: Long) {
        dao.updateStatus(id, SmsStatus.SENDING.apiValue, null, System.currentTimeMillis())
    }

    suspend fun markSent(id: Long) {
        dao.updateStatus(id, SmsStatus.SENT.apiValue, null, System.currentTimeMillis())
    }

    suspend fun markFailed(id: Long, error: String) {
        dao.updateStatus(id, SmsStatus.FAILED.apiValue, error, System.currentTimeMillis())
    }

    suspend fun counts(): Map<String, Int> = mapOf(
        SmsStatus.PENDING.apiValue to dao.countByStatus(SmsStatus.PENDING.apiValue),
        SmsStatus.SENDING.apiValue to dao.countByStatus(SmsStatus.SENDING.apiValue),
        SmsStatus.SENT.apiValue to dao.countByStatus(SmsStatus.SENT.apiValue),
        SmsStatus.FAILED.apiValue to dao.countByStatus(SmsStatus.FAILED.apiValue)
    )
}
