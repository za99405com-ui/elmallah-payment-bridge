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
    @Json(name = "vfCashEnabled") val vfCashEnabled: Boolean = false,
    @Json(name = "bankAlAhlyEnabled") val bankAlAhlyEnabled: Boolean = false,
    @Json(name = "serverTime") val serverTime: Long,
    @Json(name = "activeRulesCount") val activeRulesCount: Int? = null,
    @Json(name = "rules") val rules: List<PaymentSourceRuleDto>? = null
)

@JsonClass(generateAdapter = true)
data class PaymentSourceRuleDto(
    @Json(name = "id") val id: String,
    @Json(name = "code") val code: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "enabled") val enabled: Boolean = false,
    @Json(name = "channel") val channel: String = "other",
    @Json(name = "packageNames") val packageNames: List<String> = emptyList(),
    @Json(name = "sourceSender") val sourceSender: String? = null,
    @Json(name = "titleContains") val titleContains: String? = null,
    @Json(name = "bodyContains") val bodyContains: String? = null,
    @Json(name = "amountRegex") val amountRegex: String? = null,
    @Json(name = "payerPhoneRegex") val payerPhoneRegex: String? = null,
    @Json(name = "accountIdentifierRegex") val accountIdentifierRegex: String? = null,
    @Json(name = "priority") val priority: Int = 100,
    @Json(name = "parserType") val parserType: String = "regex"
) {
    fun toDomain(): PaymentSourceRule = PaymentSourceRule(
        id = id,
        name = name.ifBlank { code ?: id },
        enabled = enabled,
        paymentChannel = channel,
        packageNames = packageNames.filter { it.isNotBlank() },
        senderFilters = splitValues(sourceSender),
        titleContains = splitValues(titleContains).ifEmpty { null },
        bodyContains = splitValues(bodyContains).ifEmpty { null },
        amountExtractionRegex = amountRegex?.takeIf { it.isNotBlank() },
        senderPhoneExtractionRegex = payerPhoneRegex?.takeIf { it.isNotBlank() },
        accountIdentifierRegex = accountIdentifierRegex?.takeIf { it.isNotBlank() },
        priority = priority,
        parserType = parserType,
        isLocalDraft = false
    )

    private fun splitValues(value: String?): List<String> =
        value
            ?.split(Regex("""[,;|\n]"""))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.distinct()
            ?: emptyList()
}

@JsonClass(generateAdapter = true)
data class PaymentRulesResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "rulesVersion") val rulesVersion: String? = null,
    @Json(name = "rules") val rules: List<PaymentSourceRuleDto> = emptyList()
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
