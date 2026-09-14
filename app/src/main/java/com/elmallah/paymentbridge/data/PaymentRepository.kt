package com.elmallah.paymentbridge.data

import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.SyncStatus
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

sealed class RecordResult {
    data class Success(val entity: PaymentEventEntity) : RecordResult()
    data class Duplicate(val existing: PaymentEventEntity) : RecordResult()
    data class Failure(val errorMessage: String) : RecordResult()
}

class PaymentRepository(
    private val dao: PaymentEventDao,
    private val keyManager: DeviceKeyManager
) {

    val allEventsFlow: Flow<List<PaymentEventEntity>> = dao.getAllEventsFlow()

    fun getTodayEventCount(): Flow<Int> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTodayEventCountFlow(cal.timeInMillis)
    }

    fun getTodayTotalAmountMinor(): Flow<Long?> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return dao.getTodayTotalAmountMinorFlow(cal.timeInMillis)
    }

    /**
     * Records parsed payment event into local Room database with strict duplicate prevention.
     * Enforces CAPTURE_ONLY mode when upload bridge is disabled.
     */
    suspend fun recordPaymentEvent(
        event: PaymentBridgeEvent,
        rawSnippet: String? = null
    ): RecordResult {
        // 1. Check duplicate by provider + transaction reference
        val existingByRef = dao.findByProviderAndReference(event.provider, event.transactionReference)
        if (existingByRef != null) {
            return RecordResult.Duplicate(existingByRef)
        }

        val existingByFingerprint = dao.findByFingerprint(event.fingerprint)
        if (existingByFingerprint != null) {
            return RecordResult.Duplicate(existingByFingerprint)
        }

        // 2. Raw snippet privacy: only store if explicitly enabled in diagnostics
        val snippetToStore = if (keyManager.rawDiagnosticsEnabled) rawSnippet else null

        // 3. Application mode: CAPTURE_ONLY by default in Phase 1
        val initialStatus = if (keyManager.bridgeUploadEnabled) {
            SyncStatus.PENDING_UPLOAD
        } else {
            SyncStatus.CAPTURE_ONLY
        }

        val entity = PaymentEventEntity.fromDomain(
            event = event,
            rawSnippet = snippetToStore,
            initialSyncStatus = initialStatus
        )

        val insertedId = dao.insert(entity)
        if (insertedId == -1L) {
            // Room conflict abort on unique constraint
            val existing = dao.findByProviderAndReference(event.provider, event.transactionReference)
                ?: dao.findByEventId(event.eventId)
            return if (existing != null) RecordResult.Duplicate(existing)
            else RecordResult.Failure("فشل إدراج العملية في قاعدة البيانات")
        }

        return RecordResult.Success(entity.copy(id = insertedId))
    }

    suspend fun clearAllRawSnippets(): Int {
        return dao.clearAllRawSnippets()
    }

    suspend fun purgeOldSnippets(olderThanDays: Int = 7): Int {
        val threshold = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000)
        return dao.purgeOldRawSnippets(threshold)
    }

    suspend fun getPendingUploads(): List<PaymentEventEntity> {
        return dao.getPendingUploads()
    }

    suspend fun updateSyncFailure(eventId: String, error: String?) {
        dao.updateSyncFailure(eventId, SyncStatus.FAILED_RETRYABLE.name, System.currentTimeMillis(), error)
    }

    suspend fun updateServerAck(
        eventId: String,
        matchStatus: String,
        orderId: String?,
        message: String?
    ) {
        dao.updateServerAck(eventId, matchStatus, orderId, message, System.currentTimeMillis())
        keyManager.lastSyncTimestamp = System.currentTimeMillis()
    }
}
