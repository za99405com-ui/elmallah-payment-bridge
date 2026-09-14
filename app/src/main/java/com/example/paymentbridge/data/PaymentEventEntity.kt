package com.example.paymentbridge.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.paymentbridge.domain.PaymentBridgeEvent

@Entity(
    tableName = "payment_events",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["transactionFingerprint"], unique = true),
        Index(value = ["transactionReference"]),
        Index(value = ["receivedAt"]),
        Index(value = ["syncStatus"])
    ]
)
data class PaymentEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val transactionFingerprint: String,
    val provider: String,
    val paymentChannel: String,
    val amountMinor: Long,
    val currency: String = "EGP",
    val payerPhone: String? = null,
    val walletPhone: String? = null,
    val transactionReference: String,
    val accountLast4: String? = null,
    val sourceSender: String,
    val sourcePackage: String,
    val notificationPostedAt: Long,
    val capturedAt: Long,
    val receivedAt: Long = System.currentTimeMillis(),
    val rawMessageHash: String,
    val rawMessageSnippet: String? = null,
    val syncStatus: String,
    val serverMatchStatus: String? = null,
    val matchedOrderId: String? = null,
    val parseStatus: String = "SUCCESS",
    val attemptCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastErrorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val amountInMajorUnits: Double
        get() = amountMinor / 100.0

    fun toDomain(): PaymentBridgeEvent {
        return PaymentBridgeEvent(
            eventId = eventId,
            provider = provider,
            paymentChannel = paymentChannel,
            amountMinor = amountMinor,
            currency = currency,
            payerPhone = payerPhone,
            walletPhone = walletPhone,
            transactionReference = transactionReference,
            accountLast4 = accountLast4,
            sourceSender = sourceSender,
            sourcePackage = sourcePackage,
            notificationPostedAt = notificationPostedAt,
            capturedAt = capturedAt,
            parserVersion = "1.0",
            confidence = "high",
            rawMessageHash = rawMessageHash,
            deviceId = ""
        )
    }

    companion object {
        fun fromDomain(
            event: PaymentBridgeEvent,
            syncStatus: String,
            rawMessageSnippet: String? = null
        ): PaymentEventEntity {
            return PaymentEventEntity(
                eventId = event.eventId,
                transactionFingerprint = event.fingerprint,
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
                receivedAt = System.currentTimeMillis(),
                rawMessageHash = event.rawMessageHash,
                rawMessageSnippet = rawMessageSnippet,
                syncStatus = syncStatus
            )
        }
    }
}
