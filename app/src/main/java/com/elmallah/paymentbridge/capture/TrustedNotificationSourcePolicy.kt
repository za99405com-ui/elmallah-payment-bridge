package com.elmallah.paymentbridge.capture

import com.elmallah.paymentbridge.domain.PaymentProvider

sealed class SourceValidationResult {
    data class Accepted(val provider: String) : SourceValidationResult()
    data class Rejected(val reason: String) : SourceValidationResult()
}

/**
 * Strict live notification source validation layer.
 * Enforces that notifications only originate from approved system messaging apps
 * and from exact verified financial senders (VF-Cash, Bank-AlAhly).
 */
object TrustedNotificationSourcePolicy {

    val TRUSTED_MESSAGING_PACKAGES: Set<String> = setOf(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging"
    )

    const val SENDER_VODAFONE_CASH = "VF-Cash"
    const val SENDER_NBE = "Bank-AlAhly"

    fun validateSource(sourcePackage: String, senderTitle: String): SourceValidationResult {
        val trimmedPackage = sourcePackage.trim()
        if (!TRUSTED_MESSAGING_PACKAGES.contains(trimmedPackage)) {
            return SourceValidationResult.Rejected(
                "حزمة المصدر غير موثوقة: '$trimmedPackage' (مسموح فقط بتطبيقات الرسائل المعتمدة)"
            )
        }

        val normalizedTitle = senderTitle.trim()

        if (normalizedTitle.equals(SENDER_VODAFONE_CASH, ignoreCase = true)) {
            return SourceValidationResult.Accepted(PaymentProvider.VODAFONE_CASH)
        }

        if (normalizedTitle.equals(SENDER_NBE, ignoreCase = true)) {
            return SourceValidationResult.Accepted(PaymentProvider.NBE_INCOMING_TRANSFER)
        }

        return SourceValidationResult.Rejected(
            "عنوان المرسل غير معتمد: '$normalizedTitle' (يُقبل فقط $SENDER_VODAFONE_CASH أو $SENDER_NBE)"
        )
    }
}
