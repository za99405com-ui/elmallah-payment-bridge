package com.elmallah.paymentbridge.capture

import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.PaymentRuleStore
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

    fun validateSource(
        sourcePackage: String,
        senderTitle: String,
        rules: List<PaymentSourceRule> = PaymentRuleStore.getDefaultRules()
    ): SourceValidationResult {
        val trimmedPackage = sourcePackage.trim()
        val normalizedTitle = senderTitle.trim()

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
                rule.senderFilters.any { it.equals(normalizedTitle, ignoreCase = true) }
            pkgMatch && senderMatch
        }

        if (matchedRule != null) {
            return SourceValidationResult.Accepted(
                provider = matchedRule.id,
                paymentChannel = matchedRule.paymentChannel
            )
        }

        // 3. Fallback compatibility for standard legacy senders
        if (normalizedTitle.equals(SENDER_VODAFONE_CASH, ignoreCase = true)) {
            return SourceValidationResult.Accepted(PaymentProvider.VODAFONE_CASH)
        }
        if (normalizedTitle.equals(SENDER_NBE, ignoreCase = true)) {
            return SourceValidationResult.Accepted(PaymentProvider.NBE_INCOMING_TRANSFER)
        }

        return SourceValidationResult.Rejected(
            "عنوان المرسل غير معتمد: '$normalizedTitle' (لا توجد قاعدة مطابقة معتمدة من admin3)"
        )
    }
}
