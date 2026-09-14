package com.example.paymentbridge.data

import android.content.Context
import com.example.paymentbridge.domain.PaymentBridgeEvent
import com.example.paymentbridge.domain.SyncStatus
import com.example.paymentbridge.network.ApiClientProvider
import com.example.paymentbridge.network.HealthCheckResponse
import com.example.paymentbridge.network.PaymentEventUploadRequest
import com.example.paymentbridge.network.PaymentEventUploadResponse
import com.example.paymentbridge.sync.SyncScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.Calendar

data class TodayMetrics(
    val count: Int,
    val totalAmountMinor: Long
) {
    val totalAmountMajor: Double
        get() = totalAmountMinor / 100.0
}

sealed class PaymentProcessOutcome {
    data class NewPaymentEnqueued(val event: PaymentBridgeEvent) : PaymentProcessOutcome()
    data class DuplicateDetected(val existingReference: String) : PaymentProcessOutcome()
    data class SaveFailed(val error: String) : PaymentProcessOutcome()
}

class PaymentRepository(
    private val context: Context,
    private val dao: PaymentEventDao = PaymentDatabase.getInstance(context).paymentEventDao(),
    private val apiProvider: ApiClientProvider = ApiClientProvider(context)
) {

    val allEventsFlow: Flow<List<PaymentEventEntity>> = dao.getAllEventsFlow()

    val lastSyncTimeFlow: Flow<Long?> = dao.getLastSuccessfulSyncTimeFlow()

    fun getTodayMetricsFlow(): Flow<TodayMetrics> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        return combine(
            dao.getTodayCountFlow(startOfDay),
            dao.getTodaySumMinorFlow(startOfDay)
        ) { count, sumMinor ->
            TodayMetrics(count = count, totalAmountMinor = sumMinor)
        }
    }

    /**
     * Ingests a parsed payment event:
     * 1. Checks local duplicate fingerprint & transaction reference.
     * 2. If duplicate -> records as DUPLICATE, does NOT upload.
     * 3. If new -> saves as PENDING_UPLOAD and triggers upload.
     */
    suspend fun ingestPaymentEvent(
        event: PaymentBridgeEvent,
        rawSnippet: String? = null
    ): PaymentProcessOutcome = withContext(Dispatchers.IO) {
        // 1. Check duplicate by fingerprint
        val existingByFingerprint = dao.getByFingerprint(event.fingerprint)
        if (existingByFingerprint != null) {
            return@withContext PaymentProcessOutcome.DuplicateDetected(event.transactionReference)
        }

        // 2. Check duplicate by transaction reference (primary transaction identity)
        val existingByRef = dao.getByTransactionReference(event.transactionReference)
        if (existingByRef != null) {
            return@withContext PaymentProcessOutcome.DuplicateDetected(event.transactionReference)
        }

        val snippet = if (apiProvider.keyManager.isRawDiagnosticsEnabled) rawSnippet else null
        val entity = PaymentEventEntity.fromDomain(
            event = event,
            syncStatus = SyncStatus.PENDING_UPLOAD.name,
            rawMessageSnippet = snippet
        )

        try {
            dao.insert(entity)
            // Trigger background upload immediately
            uploadEvent(entity)
            PaymentProcessOutcome.NewPaymentEnqueued(event)
        } catch (e: Exception) {
            PaymentProcessOutcome.SaveFailed(e.message ?: "Unknown database error")
        }
    }

    /**
     * Executes single event upload to elmallah-admin3.
     */
    suspend fun uploadEvent(entity: PaymentEventEntity): Result<PaymentEventUploadResponse> = withContext(Dispatchers.IO) {
        val api = apiProvider.getApi()
        val request = PaymentEventUploadRequest(
            eventId = entity.eventId,
            provider = entity.provider,
            paymentChannel = entity.paymentChannel,
            amountMinor = entity.amountMinor,
            currency = entity.currency,
            payerPhone = entity.payerPhone,
            walletPhone = entity.walletPhone,
            transactionReference = entity.transactionReference,
            accountLast4 = entity.accountLast4,
            notificationPostedAt = entity.notificationPostedAt,
            capturedAt = entity.capturedAt,
            parserVersion = "1.0",
            confidence = "high",
            rawMessageHash = entity.rawMessageHash,
            deviceId = apiProvider.keyManager.deviceId,
            rawSnippet = entity.rawMessageSnippet
        )

        try {
            val response = api.uploadPaymentEvent(request)
            val now = System.currentTimeMillis()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    dao.updateSyncResult(
                        eventId = entity.eventId,
                        newStatus = SyncStatus.UPLOADED.name,
                        matchStatus = body.matchStatus,
                        orderId = body.matchedOrderId,
                        timestamp = now,
                        error = null
                    )
                    return@withContext Result.success(body)
                } else {
                    val matchStatus = body?.matchStatus ?: "REJECTED"
                    dao.updateSyncResult(
                        eventId = entity.eventId,
                        newStatus = SyncStatus.REJECTED_BY_SERVER.name,
                        matchStatus = matchStatus,
                        orderId = body?.matchedOrderId,
                        timestamp = now,
                        error = body?.message ?: "Server rejected event"
                    )
                    return@withContext Result.failure(Exception(body?.message ?: "Server rejected"))
                }
            } else {
                val code = response.code()
                val errorMsg = "HTTP $code: ${response.errorBody()?.string() ?: response.message()}"
                val targetStatus = if (code in 400..403) {
                    SyncStatus.REJECTED_BY_SERVER.name
                } else if (code == 409) {
                    SyncStatus.DUPLICATE.name
                } else {
                    SyncStatus.FAILED_RETRYABLE.name
                }

                dao.updateSyncResult(
                    eventId = entity.eventId,
                    newStatus = targetStatus,
                    matchStatus = null,
                    orderId = null,
                    timestamp = now,
                    error = errorMsg
                )

                if (targetStatus == SyncStatus.FAILED_RETRYABLE.name) {
                    SyncScheduler.scheduleRetry(context)
                }
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            dao.updateSyncResult(
                eventId = entity.eventId,
                newStatus = SyncStatus.FAILED_RETRYABLE.name,
                matchStatus = null,
                orderId = null,
                timestamp = now,
                error = e.message ?: "Network failure"
            )
            SyncScheduler.scheduleRetry(context)
            return@withContext Result.failure(e)
        }
    }

    /**
     * Safely resends an event. Never generates a new eventId.
     */
    suspend fun retryEventUpload(eventId: String): Result<PaymentEventUploadResponse> = withContext(Dispatchers.IO) {
        val entity = dao.getByEventId(eventId)
            ?: return@withContext Result.failure(Exception("العملية غير موجودة"))

        // Resend using the exact same eventId & transactionReference
        uploadEvent(entity)
    }

    /**
     * Performs a health check ping to test server connectivity and secret authentication.
     */
    suspend fun testServerConnection(): Result<HealthCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val api = apiProvider.getApi()
            val res = api.checkHealth()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("HTTP ${res.code()}: ${res.errorBody()?.string() ?: res.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
