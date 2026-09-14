package com.elmallah.paymentbridge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PaymentEventEntity): Long

    @Update
    suspend fun update(entity: PaymentEventEntity)

    @Query("SELECT * FROM payment_events WHERE eventId = :eventId LIMIT 1")
    suspend fun findByEventId(eventId: String): PaymentEventEntity?

    @Query("SELECT * FROM payment_events WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findByFingerprint(fingerprint: String): PaymentEventEntity?

    @Query("SELECT * FROM payment_events WHERE provider = :provider AND transactionReference = :reference LIMIT 1")
    suspend fun findByProviderAndReference(provider: String, reference: String): PaymentEventEntity?

    @Query("SELECT * FROM payment_events ORDER BY capturedAt DESC")
    fun getAllEventsFlow(): Flow<List<PaymentEventEntity>>

    @Query("SELECT * FROM payment_events WHERE syncStatus = :status ORDER BY capturedAt DESC")
    fun getEventsBySyncStatus(status: String): Flow<List<PaymentEventEntity>>

    @Query("SELECT * FROM payment_events WHERE syncStatus IN ('PENDING_UPLOAD', 'FAILED_RETRYABLE') ORDER BY capturedAt ASC LIMIT :limit")
    suspend fun getPendingUploads(limit: Int = 20): List<PaymentEventEntity>

    @Query("UPDATE payment_events SET syncStatus = :status, lastSyncAttemptAt = :attemptAt, lastSyncError = :error WHERE eventId = :eventId")
    suspend fun updateSyncFailure(eventId: String, status: String, attemptAt: Long, error: String?)

    @Query("""
        UPDATE payment_events 
        SET syncStatus = 'UPLOADED', 
            serverMatchStatus = :matchStatus, 
            serverOrderId = :orderId, 
            serverMessage = :message, 
            acknowledgedAt = :ackAt 
        WHERE eventId = :eventId
    """)
    suspend fun updateServerAck(
        eventId: String,
        matchStatus: String,
        orderId: String?,
        message: String?,
        ackAt: Long
    )

    @Query("SELECT COUNT(*) FROM payment_events WHERE capturedAt >= :startOfDay")
    fun getTodayEventCountFlow(startOfDay: Long): Flow<Int>

    @Query("SELECT SUM(amountMinor) FROM payment_events WHERE capturedAt >= :startOfDay AND syncStatus != 'DUPLICATE'")
    fun getTodayTotalAmountMinorFlow(startOfDay: Long): Flow<Long?>

    /**
     * Privacy retention purge:
     * Clears raw diagnostic SMS snippets older than threshold timestamp.
     * Normalized transaction metadata and financial amounts remain preserved.
     */
    @Query("UPDATE payment_events SET rawSnippet = NULL WHERE rawSnippet IS NOT NULL AND capturedAt < :thresholdMillis")
    suspend fun purgeOldRawSnippets(thresholdMillis: Long): Int

    /**
     * Immediate privacy purge:
     * Clears ALL raw diagnostic snippets from the local database.
     */
    @Query("UPDATE payment_events SET rawSnippet = NULL WHERE rawSnippet IS NOT NULL")
    suspend fun clearAllRawSnippets(): Int
}
