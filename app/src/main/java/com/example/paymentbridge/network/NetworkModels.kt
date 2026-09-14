package com.example.paymentbridge.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentEventUploadRequest(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "provider") val provider: String,
    @Json(name = "paymentChannel") val paymentChannel: String,
    @Json(name = "amountMinor") val amountMinor: Long,
    @Json(name = "currency") val currency: String,
    @Json(name = "payerPhone") val payerPhone: String?,
    @Json(name = "walletPhone") val walletPhone: String?,
    @Json(name = "transactionReference") val transactionReference: String,
    @Json(name = "accountLast4") val accountLast4: String?,
    @Json(name = "notificationPostedAt") val notificationPostedAt: Long,
    @Json(name = "capturedAt") val capturedAt: Long,
    @Json(name = "parserVersion") val parserVersion: String,
    @Json(name = "confidence") val confidence: String,
    @Json(name = "rawMessageHash") val rawMessageHash: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "rawSnippet") val rawSnippet: String? = null
)

@JsonClass(generateAdapter = true)
data class PaymentEventUploadResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "eventId") val eventId: String,
    @Json(name = "matchStatus") val matchStatus: String?,
    @Json(name = "matchedOrderId") val matchedOrderId: String?,
    @Json(name = "message") val message: String?,
    @Json(name = "processedAt") val processedAt: Long?
)

@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    @Json(name = "status") val status: String,
    @Json(name = "serverTime") val serverTime: Long? = null,
    @Json(name = "version") val version: String? = null
)
