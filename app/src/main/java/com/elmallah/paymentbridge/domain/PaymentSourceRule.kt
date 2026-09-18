package com.elmallah.paymentbridge.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class PaymentMessageSample(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "title") val title: String = "",
    @Json(name = "body") val body: String = "",
    @Json(name = "addedAt") val addedAt: Long = System.currentTimeMillis()
)

enum class SourceStatus(val label: String) {
    READY("جاهز"),
    LOCAL_DRAFT("مسودة محلية"),
    MISSING_APP("ناقص اختيار التطبيق"),
    NO_SAMPLES("لا توجد نماذج رسائل"),
    NEEDS_TEST("يحتاج اختبار"),
    DISABLED_BY_ADMIN("متوقف من لوحة التحكم")
}

@JsonClass(generateAdapter = true)
data class PaymentSourceRule(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "paymentChannel") val paymentChannel: String = "WALLET",
    @Json(name = "packageNames") val packageNames: List<String> = emptyList(),
    @Json(name = "senderFilters") val senderFilters: List<String> = emptyList(),
    @Json(name = "titleContains") val titleContains: List<String>? = null,
    @Json(name = "bodyContains") val bodyContains: List<String>? = null,
    @Json(name = "regexPatterns") val regexPatterns: List<String>? = null,
    @Json(name = "amountExtractionRegex") val amountExtractionRegex: String? = null,
    @Json(name = "senderPhoneExtractionRegex") val senderPhoneExtractionRegex: String? = null,
    @Json(name = "accountIdentifierRegex") val accountIdentifierRegex: String? = null,
    @Json(name = "priority") val priority: Int = 100,
    @Json(name = "parserType") val parserType: String = "RULE_BASED",
    @Json(name = "destinationAccount") val destinationAccount: String? = null,

    // Guided setup properties (backward-compatible defaults)
    @Json(name = "appName") val appName: String? = null,
    @Json(name = "sampleMessages") val sampleMessages: List<PaymentMessageSample> = emptyList(),
    @Json(name = "isLocalDraft") val isLocalDraft: Boolean = false,
    @Json(name = "lastTestedSuccess") val lastTestedSuccess: Boolean? = null
) {
    /**
     * Computes the human-friendly state for merchant display.
     */
    val status: SourceStatus
        get() {
            if (!enabled) return SourceStatus.DISABLED_BY_ADMIN
            if (isLocalDraft) return SourceStatus.LOCAL_DRAFT
            if (packageNames.isEmpty()) return SourceStatus.MISSING_APP
            val hasActivePattern = !amountExtractionRegex.isNullOrBlank() ||
                !regexPatterns.isNullOrEmpty() ||
                parserType == "VODAFONE_CASH_LEGACY" ||
                parserType == "NBE_LEGACY"
            if (sampleMessages.isEmpty() && !hasActivePattern) return SourceStatus.NO_SAMPLES
            if (lastTestedSuccess == false) return SourceStatus.NEEDS_TEST
            return SourceStatus.READY
        }

    val friendlyDisplayAppName: String
        get() = appName?.takeIf { it.isNotBlank() }
            ?: packageNames.firstOrNull()?.let { pkg ->
                when {
                    pkg.contains("vodafone", ignoreCase = true) -> "Vodafone Cash"
                    pkg.contains("nbe", ignoreCase = true) -> "NBE Mobile"
                    pkg.contains("misr", ignoreCase = true) -> "BM Online"
                    pkg.contains("cib", ignoreCase = true) -> "CIB Egypt"
                    pkg.contains("messaging", ignoreCase = true) -> "رسائل SMS"
                    else -> pkg
                }
            }
            ?: "لم يتم اختيار التطبيق"
}
