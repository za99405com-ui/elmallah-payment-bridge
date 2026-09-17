package com.elmallah.paymentbridge.domain

/**
 * Normalized payment event domain model.
 * Money is strictly stored as integer minor units (piastres / cents) to eliminate floating point issues.
 * 1 EGP = 100 piastres (e.g. 180.00 EGP => 18000, 287.22 EGP => 28722).
 */
data class PaymentBridgeEvent(
    val eventId: String,
    val provider: String,
    val paymentChannel: String,
    val amountMinor: Long,
    val currency: String = "EGP",
    val payerPhone: String? = null,
    val walletPhone: String? = null,
    val transactionReference: String? = null,
    val accountLast4: String? = null,
    val sourceSender: String,
    val sourcePackage: String,
    val notificationPostedAt: Long,
    val capturedAt: Long,
    val parserVersion: String = "2.0",
    val parseConfidence: String = "high", // parseConfidence != payment match confidence
    val rawMessageHash: String,
    val deviceId: String,
    val paymentSourceId: String = provider
) {
    /**
     * Deterministic provider-level transaction fingerprint.
     * Prevents the same financial transaction reference from being processed twice.
     * If transaction reference is absent, falls back to raw message hash.
     */
    val fingerprint: String
        get() = "$paymentSourceId:${transactionReference?.trim()?.ifEmpty { null } ?: rawMessageHash}"

    val amountInMajorUnits: Double
        get() = amountMinor / 100.0
}
