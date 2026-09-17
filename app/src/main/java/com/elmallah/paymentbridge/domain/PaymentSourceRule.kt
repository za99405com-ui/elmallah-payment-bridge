package com.elmallah.paymentbridge.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

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
    @Json(name = "destinationAccount") val destinationAccount: String? = null
)
