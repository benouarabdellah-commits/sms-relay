package com.passerelle.sms.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsDao {
    @Query("SELECT * FROM sms_jobs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SmsJob>>

    @Query("SELECT * FROM sms_jobs ORDER BY createdAt DESC")
    suspend fun listAll(): List<SmsJob>

    @Query("SELECT * FROM sms_jobs WHERE id = :id")
    suspend fun getById(id: Long): SmsJob?

    @Query(
        """
        SELECT * FROM sms_jobs
        WHERE status = 'pending'
        ORDER BY createdAt ASC
        LIMIT 1
        """
    )
    suspend fun nextPending(): SmsJob?

    @Insert
    suspend fun insert(job: SmsJob): Long

    @Query(
        """
        UPDATE sms_jobs
        SET status = :status, error = :error, updatedAt = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateStatus(id: Long, status: String, error: String?, updatedAt: Long)

    @Query("SELECT COUNT(*) FROM sms_jobs WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}
