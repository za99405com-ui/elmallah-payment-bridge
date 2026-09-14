package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.capture.SourceValidationResult
import com.elmallah.paymentbridge.capture.TrustedNotificationSourcePolicy
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.RawNotificationMessage

class CompositePaymentParser(
    private val parsers: List<PaymentMessageParser> = listOf(
        VodafoneCashParser(),
        NbeIncomingTransferParser()
    )
) {

    /**
     * Strict LIVE notification processing pipeline:
     * 1. Strictly validates notification source package against trusted SMS messaging apps.
     * 2. Strictly validates sender title against verified financial sender IDs (VF-Cash, Bank-AlAhly).
     * 3. Routes directly to the authoritative parser for that verified provider.
     * Rejects any spoofed senders, arbitrary applications, or unknown message formats without fallback.
     */
    fun parseLiveMessage(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        when (val validation = TrustedNotificationSourcePolicy.validateSource(message.sourcePackage, message.title)) {
            is SourceValidationResult.Rejected -> {
                return PaymentParseResult.Ignored(
                    IgnoreReason.IGNORED_NON_PAYMENT_SENDER,
                    validation.reason
                )
            }
            is SourceValidationResult.Accepted -> {
                val parser = parsers.firstOrNull { it.providerName == validation.provider }
                    ?: return PaymentParseResult.Failed(
                        FailureReason.UNKNOWN_MESSAGE_FORMAT,
                        "لم يتم العثور على معالج للمزود المعتمد: ${validation.provider}"
                    )
                return parser.parse(message, deviceId)
            }
        }
    }

    /**
     * Diagnostic parser test mode used exclusively by the manual "اختبار قراءة الرسائل" screen.
     * Allows merchant testing of pasted SMS snippets without enforcing package-level permissions.
     */
    fun parseDiagnosticTestMessage(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val matchingParser = parsers.firstOrNull { it.canHandle(message) }
            ?: parsers.firstOrNull {
                val titleClean = message.title.trim()
                if (it.providerName == PaymentProvider.VODAFONE_CASH && titleClean.equals("VF-Cash", ignoreCase = true)) true
                else if (it.providerName == PaymentProvider.NBE_INCOMING_TRANSFER && titleClean.equals("Bank-AlAhly", ignoreCase = true)) true
                else false
            }

        if (matchingParser != null) {
            return matchingParser.parse(message, deviceId)
        }

        // Try parsers based on message content indicators in test mode
        val text = message.fullText
        if (text.contains("تم استلام مبلغ") || text.contains("فودافون كاش") || text.contains("محفظتك")) {
            return parsers.first { it.providerName == PaymentProvider.VODAFONE_CASH }.parse(message, deviceId)
        }
        if (text.contains("تم إضافة تحويل") || text.contains("تم اضافه تحويل") || text.contains("Bank-AlAhly")) {
            return parsers.first { it.providerName == PaymentProvider.NBE_INCOMING_TRANSFER }.parse(message, deviceId)
        }

        return PaymentParseResult.Failed(
            FailureReason.UNKNOWN_MESSAGE_FORMAT,
            "لم يتم التعرف على مزود الدفع من نص الرسالة (يدعم فقط فودافون كاش أو البنك الأهلي)"
        )
    }
}
