package com.example.paymentbridge.domain

/**
 * Normalized payment event model.
 * Money is strictly stored as integer minor units (piastres / cents) to prevent floating point inaccuracy.
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
    val transactionReference: String,
    val accountLast4: String? = null,
    val sourceSender: String,
    val sourcePackage: String,
    val notificationPostedAt: Long,
    val capturedAt: Long,
    val parserVersion: String = "1.0",
    val confidence: String = "high",
    val rawMessageHash: String,
    val deviceId: String
) {
    /**
     * Deterministic local transaction fingerprint.
     * Prevents double processing and ensures transaction uniqueness.
     */
    val fingerprint: String
        get() = "$provider:${transactionReference.trim()}:$amountMinor"

    val amountInMajorUnits: Double
        get() = amountMinor / 100.0
}
