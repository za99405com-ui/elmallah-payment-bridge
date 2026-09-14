package com.example.paymentbridge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentEventDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: PaymentEventEntity): Long

    @Update
    suspend fun update(event: PaymentEventEntity)

    @Query("SELECT * FROM payment_events ORDER BY receivedAt DESC")
    fun getAllEventsFlow(): Flow<List<PaymentEventEntity>>

    @Query("SELECT * FROM payment_events WHERE id = :id")
    suspend fun getById(id: Long): PaymentEventEntity?

    @Query("SELECT * FROM payment_events WHERE eventId = :eventId LIMIT 1")
    suspend fun getByEventId(eventId: String): PaymentEventEntity?

    @Query("SELECT * FROM payment_events WHERE transactionFingerprint = :fingerprint LIMIT 1")
    suspend fun getByFingerprint(fingerprint: String): PaymentEventEntity?

    @Query("SELECT * FROM payment_events WHERE transactionReference = :reference LIMIT 1")
    suspend fun getByTransactionReference(reference: String): PaymentEventEntity?

    @Query("SELECT * FROM payment_events WHERE syncStatus IN ('PENDING_UPLOAD', 'FAILED_RETRYABLE') ORDER BY receivedAt ASC")
    suspend fun getPendingUploads(): List<PaymentEventEntity>

    @Query("SELECT COUNT(*) FROM payment_events WHERE receivedAt >= :startOfDayMillis AND syncStatus != 'DUPLICATE'")
    fun getTodayCountFlow(startOfDayMillis: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM payment_events WHERE receivedAt >= :startOfDayMillis AND syncStatus != 'DUPLICATE'")
    fun getTodaySumMinorFlow(startOfDayMillis: Long): Flow<Long>

    @Query("SELECT MAX(lastAttemptAt) FROM payment_events WHERE syncStatus = 'UPLOADED'")
    fun getLastSuccessfulSyncTimeFlow(): Flow<Long?>

    @Query("UPDATE payment_events SET syncStatus = :newStatus, serverMatchStatus = :matchStatus, matchedOrderId = :orderId, lastAttemptAt = :timestamp, attemptCount = attemptCount + 1, lastErrorMessage = :error WHERE eventId = :eventId")
    suspend fun updateSyncResult(
        eventId: String,
        newStatus: String,
        matchStatus: String?,
        orderId: String?,
        timestamp: Long,
        error: String?
    )
}
