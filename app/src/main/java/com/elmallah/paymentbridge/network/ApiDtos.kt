package com.elmallah.paymentbridge.network

import com.elmallah.paymentbridge.domain.PaymentSourceRule
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
    @Json(name = "transactionReference") val transactionReference: String? = null,
    @Json(name = "accountLast4") val accountLast4: String? = null,
    @Json(name = "sourceSender") val sourceSender: String,
    @Json(name = "sourcePackage") val sourcePackage: String,
    @Json(name = "notificationPostedAt") val notificationPostedAt: Long,
    @Json(name = "capturedAt") val capturedAt: Long,
    @Json(name = "parserVersion") val parserVersion: String,
    @Json(name = "parseConfidence") val parseConfidence: String,
    @Json(name = "rawMessageHash") val rawMessageHash: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "paymentSourceId") val paymentSourceId: String = provider
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
data class HeartbeatRequest(
    @Json(name = "internetConnected") val internetConnected: Boolean,
    @Json(name = "appRunning") val appRunning: Boolean = true,
    @Json(name = "notificationListenerEnabled") val notificationListenerEnabled: Boolean,
    @Json(name = "appVersion") val appVersion: String
)

@JsonClass(generateAdapter = true)
data class HeartbeatResponse(
    @Json(name = "status") val status: String,
    @Json(name = "online") val online: Boolean,
    @Json(name = "busy") val busy: Boolean,
    @Json(name = "busySessionId") val busySessionId: String? = null,
    @Json(name = "vfCashEnabled") val vfCashEnabled: Boolean = true,
    @Json(name = "bankAlAhlyEnabled") val bankAlAhlyEnabled: Boolean = false,
    @Json(name = "serverTime") val serverTime: Long,
    @Json(name = "activeRulesCount") val activeRulesCount: Int? = null,
    @Json(name = "rules") val rules: List<PaymentSourceRule>? = null
)

@JsonClass(generateAdapter = true)
data class PaymentRulesResponse(
    @Json(name = "status") val status: String,
    @Json(name = "rules") val rules: List<PaymentSourceRule>
)

@JsonClass(generateAdapter = true)
data class DeviceProviderConfigRequest(
    @Json(name = "vfCashEnabled") val vfCashEnabled: Boolean? = null,
    @Json(name = "bankAlAhlyEnabled") val bankAlAhlyEnabled: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DeviceProviderConfigResponse(
    @Json(name = "status") val status: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "vfCashEnabled") val vfCashEnabled: Boolean,
    @Json(name = "bankAlAhlyEnabled") val bankAlAhlyEnabled: Boolean,
    @Json(name = "busy") val busy: Boolean,
    @Json(name = "busySessionId") val busySessionId: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    @Json(name = "status") val status: String,
    @Json(name = "version") val version: String? = null,
    @Json(name = "serverTime") val serverTime: Long? = null
)
