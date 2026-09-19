package com.elmallah.paymentbridge.capture

import com.elmallah.paymentbridge.domain.PaymentSourceRule

sealed class SourceValidationResult {
    data class Accepted(
        val provider: String,
        val paymentChannel: String = "WALLET"
    ) : SourceValidationResult()

    data class Rejected(val reason: String) : SourceValidationResult()
}

/**
 * Strict live notification source validation layer.
 *
 * Validates that incoming notifications originate from trusted messaging apps
 * and from senders approved by active rules from elmallah-admin3.
 */
object TrustedNotificationSourcePolicy {

    val TRUSTED_MESSAGING_PACKAGES: Set<String> = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging"
    )

    const val SENDER_VODAFONE_CASH = "VF-Cash"
    const val SENDER_NBE = "Bank-AlAhly"

    private fun normalizeSenderTitle(value: String): String {
        return value
            .replace(Regex("""\p{Cf}+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun normalizeBodyText(value: String): String {
        return value
            .replace(Regex("""[\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF]"""), "")
            .replace('\u00A0', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun validateSource(
        sourcePackage: String,
        senderTitle: String,
        rules: List<PaymentSourceRule> = emptyList(),
        bodyText: String = ""
    ): SourceValidationResult {
        val trimmedPackage = sourcePackage.trim()
        val normalizedTitle = normalizeSenderTitle(senderTitle)
        val normalizedBody = normalizeBodyText(bodyText)

        // 1. Check if package is trusted
        val isAllowedPackage = TRUSTED_MESSAGING_PACKAGES.contains(trimmedPackage) ||
            rules.any { it.packageNames.any { p -> p.equals(trimmedPackage, ignoreCase = true) } }

        if (!isAllowedPackage) {
            return SourceValidationResult.Rejected(
                "حزمة المصدر غير موثوقة: '$trimmedPackage' (مسموح فقط بتطبيقات الرسائل المعتمدة)"
            )
        }

        // 2. Check against active enabled rules
        val matchedRule = rules.filter { it.enabled }.firstOrNull { rule ->
            val pkgMatch = rule.packageNames.isEmpty() ||
                rule.packageNames.any { it.equals(trimmedPackage, ignoreCase = true) }
            val senderMatch = rule.senderFilters.isEmpty() ||
                rule.senderFilters.any { normalizeSenderTitle(it).equals(normalizedTitle, ignoreCase = true) }
            val bodyMatch = normalizedBody.isBlank() ||
                rule.bodyContains.isNullOrEmpty() ||
                rule.bodyContains.any { token ->
                    normalizedBody.contains(normalizeBodyText(token), ignoreCase = true)
                }
            pkgMatch && senderMatch && bodyMatch
        }

        if (matchedRule != null) {
            return SourceValidationResult.Accepted(
                provider = matchedRule.id,
                paymentChannel = matchedRule.paymentChannel
            )
        }

        // No sender-only fallback is allowed in LIVE mode. An empty admin3 rule
        // list is authoritative and must disable live payment detection.
        return SourceValidationResult.Rejected(
            "عنوان المرسل غير معتمد: '$normalizedTitle' (لا توجد قاعدة مطابقة معتمدة من admin3)"
        )
    }
}
