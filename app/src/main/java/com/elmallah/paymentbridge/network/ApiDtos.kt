package com.elmallah.paymentbridge.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentEventUploadRequest(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "provider") val provider: String,
    @Json(name = "paymentChannel") val paymentChannel: String,
    @Json(name = "amountMinor") val amountMinor: Long,
    @Json(name = "currency") val currency: String = "EGP",
    @Json(name = "payerPhone") val payerPhone: String? = null,
    @Json(name = "walletPhone") val walletPhone: String? = null,
    @Json(name = "transactionReference") val transactionReference: String,
    @Json(name = "accountLast4") val accountLast4: String? = null,
    @Json(name = "sourceSender") val sourceSender: String,
    @Json(name = "sourcePackage") val sourcePackage: String,
    @Json(name = "notificationPostedAt") val notificationPostedAt: Long,
    @Json(name = "capturedAt") val capturedAt: Long,
    @Json(name = "parserVersion") val parserVersion: String,
    @Json(name = "parseConfidence") val parseConfidence: String, // parseConfidence != order match confidence
    @Json(name = "rawMessageHash") val rawMessageHash: String,
    @Json(name = "deviceId") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class PaymentEventUploadResponse(
    @Json(name = "status") val status: String,
    @Json(name = "eventId") val eventId: String,
    @Json(name = "matchStatus") val matchStatus: String? = null,
    @Json(name = "orderId") val orderId: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "serverTimestamp") val serverTimestamp: Long? = null
)

@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    @Json(name = "status") val status: String,
    @Json(name = "version") val version: String? = null,
    @Json(name = "serverTime") val serverTime: Long? = null
)
