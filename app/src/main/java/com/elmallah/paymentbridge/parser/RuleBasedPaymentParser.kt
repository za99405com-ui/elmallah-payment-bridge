package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import java.util.UUID

/**
 * Generic rule-based payment parser driven by dynamic Payment Source Rules from elmallah-admin3.
 * Capable of parsing notifications from any Egyptian bank, e-wallet, or InstaPay.
 */
class RuleBasedPaymentParser(
    private val rule: PaymentSourceRule
) : PaymentMessageParser {

    override val providerName: String = rule.id
    override val supportedSenders: List<String> = rule.senderFilters

    override fun canHandle(message: RawNotificationMessage): Boolean {
        val cleanSender = message.title.trim()
        val senderMatches = rule.senderFilters.isEmpty() ||
            rule.senderFilters.any { it.equals(cleanSender, ignoreCase = true) }
        val packageMatches = rule.packageNames.isEmpty() ||
            rule.packageNames.any { it.equals(message.sourcePackage.trim(), ignoreCase = true) }
        return senderMatches && packageMatches
    }

    override fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val normalizedText = MessageNormalizer.normalizeForParsing(message.fullText)
        val rawHash = MessageNormalizer.sha256Hex("${message.sourcePackage}:${message.title}:${message.fullText}")

        // 1. Extract Amount
        val amountMinor = extractAmount(normalizedText)
            ?: return PaymentParseResult.Failed(
                FailureReason.AMOUNT_EXTRACTION_FAILED,
                "فشل استخراج المبلغ المالي من إشعار (${rule.name})"
            )

        if (amountMinor <= 0L) {
            return PaymentParseResult.Failed(
                FailureReason.AMOUNT_INVALID,
                "المبلغ المستخرج غير صالح ($amountMinor قرش)"
            )
        }

        // 2. Extract Payer Phone (if available)
        val payerPhone = extractPayerPhone(normalizedText)

        // 3. Extract Account Identifier / Last 4 (if available)
        val accountLast4 = extractAccountLast4(normalizedText)

        // 4. Extract Transaction Reference / RRN (if available)
        val transactionReference = extractTransactionReference(normalizedText)

        val event = PaymentBridgeEvent(
            eventId = UUID.randomUUID().toString(),
            provider = rule.id,
            paymentChannel = rule.paymentChannel,
            amountMinor = amountMinor,
            currency = "EGP",
            payerPhone = payerPhone,
            walletPhone = rule.destinationAccount,
            transactionReference = transactionReference,
            accountLast4 = accountLast4,
            sourceSender = message.title.trim(),
            sourcePackage = message.sourcePackage.trim(),
            notificationPostedAt = message.postedAtMillis,
            capturedAt = System.currentTimeMillis(),
            parserVersion = "2.0",
            parseConfidence = if (transactionReference != null || payerPhone != null) "high" else "medium",
            rawMessageHash = rawHash,
            deviceId = deviceId,
            paymentSourceId = rule.id
        )

        return PaymentParseResult.Success(event)
    }

    private fun extractAmount(text: String): Long? {
        // A. Custom rule amount extraction regex
        if (!rule.amountExtractionRegex.isNullOrBlank()) {
            try {
                val match = Regex(rule.amountExtractionRegex, RegexOption.IGNORE_CASE).find(text)
                if (match != null && match.groupValues.size > 1) {
                    val candidate = match.groupValues[1]
                    val parsed = MessageNormalizer.parseAmountToMinor(candidate)
                    if (parsed != null && parsed > 0) return parsed
                }
            } catch (_: Exception) {}
        }

        // B. Standard Arabic financial amount patterns
        // e.g. "مبلغ 500.00 جم" or "تم استلام 1,250.50 جنيه" or "بمبلغ 300 EGP"
        val generalAmountRegex = Regex(
            """(?:مبلغ|بمبلغ|قيمة|بقيمة|استلام|تحويل|إضافة|اضافة)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:[\.,][0-9]{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP|LE|L\.E)""",
            RegexOption.IGNORE_CASE
        )
        val match = generalAmountRegex.find(text)
        if (match != null && match.groupValues.size > 1) {
            val candidate = match.groupValues[1]
            val parsed = MessageNormalizer.parseAmountToMinor(candidate)
            if (parsed != null && parsed > 0) return parsed
        }

        // C. Fallback: Standalone currency indicator followed by number
        val currencyFirstRegex = Regex(
            """(?:جم|جنيه|ج\.م|EGP)\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:[\.,][0-9]{1,2})?)""",
            RegexOption.IGNORE_CASE
        )
        val match2 = currencyFirstRegex.find(text)
        if (match2 != null && match2.groupValues.size > 1) {
            val candidate = match2.groupValues[1]
            return MessageNormalizer.parseAmountToMinor(candidate)
        }

        return null
    }

    private fun extractPayerPhone(text: String): String? {
        if (!rule.senderPhoneExtractionRegex.isNullOrBlank()) {
            try {
                val match = Regex(rule.senderPhoneExtractionRegex).find(text)
                if (match != null && match.groupValues.size > 1) {
                    return MessageNormalizer.normalizeEgyptianPhone(match.groupValues[1])
                }
            } catch (_: Exception) {}
        }

        // Egyptian mobile phone pattern: 010, 011, 012, 015 followed by 8 digits
        val phoneRegex = Regex("""(?:من|المرسل|رقم)?\s*(01[0125][0-9]{8})""")
        val match = phoneRegex.find(text)
        if (match != null && match.groupValues.size > 1) {
            return MessageNormalizer.normalizeEgyptianPhone(match.groupValues[1])
        }

        return null
    }

    private fun extractAccountLast4(text: String): String? {
        if (!rule.accountIdentifierRegex.isNullOrBlank()) {
            try {
                val match = Regex(rule.accountIdentifierRegex).find(text)
                if (match != null && match.groupValues.size > 1) {
                    return match.groupValues[1].takeLast(4)
                }
            } catch (_: Exception) {}
        }

        // Common patterns: account ending in *1234 or حساب ****1234 or لحسابك رقم *4321
        val accRegex = Regex("""(?:حساب|لحسابك|account|card|بطاقة)?\s*(?:رقم)?\s*[\*xX]{1,}\s*([0-9]{4})""", RegexOption.IGNORE_CASE)
        val match = accRegex.find(text)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1]
        }
        return null
    }

    private fun extractTransactionReference(text: String): String? {
        // e.g. "رقم العملية: 12345678" or "مرجع: BM987654" or "Ref: 987654321" or "RRN: 11223344"
        val refRegex = Regex(
            """(?:رقم العملية|المرجعي|رقم المرجع|مرجع|عملية رقم|ref(?:erence)?|rrn|txn)[:\s]*([A-Za-z0-9\-_]{6,30})""",
            RegexOption.IGNORE_CASE
        )
        val match = refRegex.find(text)
        return match?.groupValues?.getOrNull(1)
    }
}
