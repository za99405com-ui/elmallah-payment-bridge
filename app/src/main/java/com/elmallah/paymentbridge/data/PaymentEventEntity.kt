package com.elmallah.paymentbridge.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.SyncStatus

@Entity(
    tableName = "payment_events",
    indices = [
        Index(value = ["eventId"], unique = true),
        // Strengthened uniqueness: same transaction reference from provider cannot be inserted twice
        Index(value = ["provider", "transactionReference"], unique = true),
        Index(value = ["capturedAt"]),
        Index(value = ["syncStatus"])
    ]
)
data class PaymentEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val fingerprint: String,
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
    val parserVersion: String = "1.0",
    @ColumnInfo(name = "parseConfidence", defaultValue = "high")
    val parseConfidence: String = "high", // parseConfidence != payment match confidence!
    val rawMessageHash: String,
    val rawSnippet: String? = null,
    val deviceId: String,
    val syncStatus: String = SyncStatus.CAPTURE_ONLY.name,
    val syncAttempts: Int = 0,
    val lastSyncAttemptAt: Long? = null,
    val lastSyncError: String? = null,
    val serverMatchStatus: String? = null,
    val serverOrderId: String? = null,
    val serverMessage: String? = null,
    val acknowledgedAt: Long? = null
) {
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
            parserVersion = parserVersion,
            parseConfidence = parseConfidence,
            rawMessageHash = rawMessageHash,
            deviceId = deviceId
        )
    }

    companion object {
        fun fromDomain(
            event: PaymentBridgeEvent,
            rawSnippet: String? = null,
            initialSyncStatus: SyncStatus = SyncStatus.CAPTURE_ONLY
        ): PaymentEventEntity {
            return PaymentEventEntity(
                eventId = event.eventId,
                fingerprint = event.fingerprint,
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
                rawSnippet = rawSnippet,
                deviceId = event.deviceId,
                syncStatus = initialSyncStatus.name
            )
        }
    }
}
