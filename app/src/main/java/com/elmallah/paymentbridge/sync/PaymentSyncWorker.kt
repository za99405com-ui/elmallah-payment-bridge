package com.elmallah.paymentbridge.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.elmallah.paymentbridge.AlMallahBridgeApp
import com.elmallah.paymentbridge.network.PaymentEventUploadRequest

class PaymentSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "PaymentSyncWorker"
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? AlMallahBridgeApp ?: return Result.failure()
        val keyManager = app.apiProvider.keyManager
        val repository = app.repository
        val api = app.apiProvider.getApi()

        // PHASE 1 MANDATE: If upload bridge is disabled, do not attempt network uploads
        if (!keyManager.bridgeUploadEnabled) {
            Log.d(TAG, "Bridge upload is disabled (CAPTURE_ONLY mode). Worker skipping sync.")
            return Result.success()
        }

        val pending = repository.getPendingUploads()
        if (pending.isEmpty()) {
            return Result.success()
        }

        var anyFailed = false

        for (entity in pending) {
            val event = entity.toDomain()
            val request = PaymentEventUploadRequest(
                eventId = event.eventId,
                provider = event.provider,
                paymentChannel = event.paymentChannel,
                amountMinor = event.amountMinor,
                currency = event.currency,
                payerPhone = event.payerPhone,
                walletPhone = event.walletPhone,
                transactionReference = event.transactionReference,
                accountLast4 = event.accountLast4,
                sourceSender = event.sourceSender,
                sourcePackage = event.sourcePackage,
                notificationPostedAt = event.notificationPostedAt,
                capturedAt = event.capturedAt,
                parserVersion = event.parserVersion,
                parseConfidence = event.parseConfidence,
                rawMessageHash = event.rawMessageHash,
                deviceId = event.deviceId,
                paymentSourceId = event.paymentSourceId
            )

            try {
                val response = api.uploadPaymentEvent(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    repository.updateServerAck(
                        eventId = entity.eventId,
                        matchStatus = body.matchStatus ?: "PENDING_REVIEW",
                        orderId = body.orderId,
                        message = body.message
                    )
                } else {
                    anyFailed = true
                    repository.updateSyncFailure(entity.eventId, "HTTP ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                anyFailed = true
                repository.updateSyncFailure(entity.eventId, e.message)
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
    }
}
