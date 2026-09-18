package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.capture.SourceValidationResult
import com.elmallah.paymentbridge.capture.TrustedNotificationSourcePolicy
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.domain.RawNotificationMessage

/**
 * Composite payment parser supporting both legacy provider parsers and
 * generic dynamic rule-based parsers fetched from elmallah-admin3.
 */
class CompositePaymentParser(
    private val ruleSupplier: () -> List<PaymentSourceRule> = { emptyList() },
    private val legacyParsers: List<PaymentMessageParser> = listOf(
        VodafoneCashParser(),
        NbeIncomingTransferParser()
    )
) {

    /**
     * Strict LIVE notification processing pipeline:
     * 1. Validates notification source package and sender against enabled admin3 rules.
     * 2. Routes to the appropriate legacy or dynamic rule-based parser.
     * 3. Rejects any spoofed senders, unauthorized packages, or unmatched formats.
     */
    fun parseLiveMessage(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val rules = ruleSupplier().filter { it.enabled }

        when (val validation = TrustedNotificationSourcePolicy.validateSource(
            message.sourcePackage,
            message.title,
            rules,
            message.fullText
        )) {
            is SourceValidationResult.Rejected -> {
                return PaymentParseResult.Ignored(
                    IgnoreReason.IGNORED_NON_PAYMENT_SENDER,
                    validation.reason
                )
            }
            is SourceValidationResult.Accepted -> {
                val matchedRule = rules.firstOrNull { it.id == validation.provider }

                // Delegate to legacy parser if matched rule specifies legacy type
                if (matchedRule?.parserType == "VODAFONE_CASH_LEGACY" || validation.provider == PaymentProvider.VODAFONE_CASH) {
                    val parser = legacyParsers.firstOrNull { it.providerName == PaymentProvider.VODAFONE_CASH }
                    if (parser != null) return parser.parse(message, deviceId)
                }

                if (matchedRule?.parserType == "NBE_LEGACY" || validation.provider == PaymentProvider.NBE_INCOMING_TRANSFER) {
                    val parser = legacyParsers.firstOrNull { it.providerName == PaymentProvider.NBE_INCOMING_TRANSFER }
                    if (parser != null) return parser.parse(message, deviceId)
                }

                // If matched by an active dynamic rule, parse with RuleBasedPaymentParser
                if (matchedRule != null) {
                    return RuleBasedPaymentParser(matchedRule).parse(message, deviceId)
                }

                // Check other legacy parsers
                val legacy = legacyParsers.firstOrNull { it.providerName == validation.provider }
                if (legacy != null) {
                    return legacy.parse(message, deviceId)
                }

                return PaymentParseResult.Failed(
                    FailureReason.UNKNOWN_MESSAGE_FORMAT,
                    "لم يتم العثور على معالج للمزود المعتمد: ${validation.provider}"
                )
            }
        }
    }

    /**
     * Diagnostic parser test mode used by the manual test screen.
     * Allows merchant testing of pasted SMS snippets without enforcing package requirements.
     */
    fun parseDiagnosticTestMessage(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val rules = ruleSupplier().filter { it.enabled }

        // 1. Try legacy parsers by sender title or canHandle
        val matchingLegacy = legacyParsers.firstOrNull { it.canHandle(message) }
            ?: legacyParsers.firstOrNull {
                val titleClean = message.title.trim()
                if (it.providerName == PaymentProvider.VODAFONE_CASH && titleClean.equals("VF-Cash", ignoreCase = true)) true
                else if (it.providerName == PaymentProvider.NBE_INCOMING_TRANSFER && titleClean.equals("Bank-AlAhly", ignoreCase = true)) true
                else false
            }

        if (matchingLegacy != null) {
            return matchingLegacy.parse(message, deviceId)
        }

        // 2. Try dynamic rules
        for (rule in rules) {
            val ruleParser = RuleBasedPaymentParser(rule)
            if (ruleParser.canHandle(message)) {
                val res = ruleParser.parse(message, deviceId)
                if (res is PaymentParseResult.Success) return res
            }
        }

        // 3. Try fallback text heuristics for merchant test convenience
        val text = message.fullText
        if (text.contains("تم استلام مبلغ") || text.contains("فودافون كاش") || text.contains("محفظتك")) {
            return legacyParsers.first { it.providerName == PaymentProvider.VODAFONE_CASH }.parse(message, deviceId)
        }
        if (text.contains("تم إضافة تحويل") || text.contains("تم اضافه تحويل") || text.contains("Bank-AlAhly")) {
            return legacyParsers.first { it.providerName == PaymentProvider.NBE_INCOMING_TRANSFER }.parse(message, deviceId)
        }

        // 4. Try any enabled dynamic rule that finds an amount in the text
        for (rule in rules) {
            val res = RuleBasedPaymentParser(rule).parse(message, deviceId)
            if (res is PaymentParseResult.Success) {
                return res
            }
        }

        return PaymentParseResult.Failed(
            FailureReason.UNKNOWN_MESSAGE_FORMAT,
            "لم يتم التعرف على مزود الدفع أو المبلغ من نص الرسالة"
        )
    }
}
