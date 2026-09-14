package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.PaymentChannel
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import java.util.UUID

class VodafoneCashParser : PaymentMessageParser {

    override val providerName: String = PaymentProvider.VODAFONE_CASH

    override val supportedSenders: List<String> = listOf("VF-Cash", "vf-cash")

    private val incomingPattern = Regex(
        "تم\\s+استلام\\s+مبلغ\\s+([0-9.,\\u066B\\u066C]+)\\s*(?:جنيه|جم)?\\s*من\\s*([0-9+]+)",
        RegexOption.IGNORE_CASE
    )

    private val operationPattern = Regex(
        "(?:رقم\\s+(?:العملية|المعاملة|العمليه)|عملية\\s+رقم)\\s*[:\\s]*([0-9A-Za-z]+)",
        RegexOption.IGNORE_CASE
    )

    private val walletPhonePattern = Regex(
        "(?:على|علي)\\s+رقم\\s+محفظتك\\s*[:\\s]*([0-9+]+)",
        RegexOption.IGNORE_CASE
    )

    override fun canHandle(message: RawNotificationMessage): Boolean {
        val titleClean = message.title.trim()
        val senderMatches = supportedSenders.any { it.equals(titleClean, ignoreCase = true) }
        val text = MessageNormalizer.normalizeForParsing(message.fullText)
        return senderMatches || text.contains("فودافون كاش") || text.contains("VF-Cash")
    }

    override fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val normalized = MessageNormalizer.normalizeForParsing(message.fullText)

        // 1. Check for outgoing transfer
        if (normalized.contains("تم تحويل مبلغ") ||
            normalized.contains("من محفظتك إلى") ||
            normalized.contains("من محفظتك الي") ||
            normalized.contains("تم خصم") ||
            normalized.contains("تم سحب") ||
            normalized.contains("سداد فاتورة")
        ) {
            return PaymentParseResult.Ignored(
                IgnoreReason.IGNORED_OUTGOING_TRANSFER,
                "تم تجاهل الرسالة لأنها عملية تحويل صادرة أو خصم وليست استلام أموال"
            )
        }

        // 2. Check for mobile recharge
        if (normalized.contains("تم شحن رصيد") || normalized.contains("شحن رصيد")) {
            return PaymentParseResult.Ignored(
                IgnoreReason.IGNORED_RECHARGE,
                "تم تجاهل الرسالة لأنها عملية شحن رصيد"
            )
        }

        // 3. Check for balance inquiry message without incoming transfer receipt
        if (!normalized.contains("تم استلام مبلغ") &&
            (normalized.contains("رصيد حسابك") || normalized.contains("رصيدك الحالي هو") || normalized.contains("رصيدك في فودافون كاش"))
        ) {
            return PaymentParseResult.Ignored(
                IgnoreReason.IGNORED_BALANCE_MESSAGE,
                "تم تجاهل الرسالة لأنها إشعار استعلام عن رصيد فقط"
            )
        }

        // 4. Must strictly contain "تم استلام مبلغ"
        val incomingMatch = incomingPattern.find(normalized)
            ?: return PaymentParseResult.Failed(
                FailureReason.UNKNOWN_MESSAGE_FORMAT,
                "الرسالة لا تحتوي على صيغة استلام أموال معتمدة من فودافون كاش ('تم استلام مبلغ ... جنيه من ...')"
            )

        val rawAmount = incomingMatch.groupValues[1]
        val rawPayerPhone = incomingMatch.groupValues[2]

        val amountMinor = MessageNormalizer.parseAmountToMinor(rawAmount)
            ?: return PaymentParseResult.Failed(
                FailureReason.MISSING_AMOUNT,
                "فشل في استخراج المبلغ المالي من الرسالة: '$rawAmount'"
            )

        val payerPhone = MessageNormalizer.normalizeEgyptianPhone(rawPayerPhone)

        // 5. Extract transaction reference
        val opMatch = operationPattern.find(normalized)
        val transactionReference = opMatch?.groupValues?.get(1)?.trim()
            ?: return PaymentParseResult.Failed(
                FailureReason.MISSING_REFERENCE,
                "لم يتم العثور على رقم العملية المرجعي في إشعار فودافون كاش"
            )

        // 6. Extract merchant wallet phone if present
        val walletMatch = walletPhonePattern.find(normalized)
        val walletPhone = walletMatch?.groupValues?.get(1)?.let {
            MessageNormalizer.normalizeEgyptianPhone(it)
        }

        val event = PaymentBridgeEvent(
            eventId = UUID.randomUUID().toString(),
            provider = PaymentProvider.VODAFONE_CASH,
            paymentChannel = PaymentChannel.VODAFONE_CASH,
            amountMinor = amountMinor,
            currency = "EGP",
            payerPhone = payerPhone,
            walletPhone = walletPhone,
            transactionReference = transactionReference,
            accountLast4 = null,
            sourceSender = message.title.trim().ifEmpty { "VF-Cash" },
            sourcePackage = message.sourcePackage,
            notificationPostedAt = message.postedAtMillis,
            capturedAt = System.currentTimeMillis(),
            parserVersion = "1.0",
            parseConfidence = if (payerPhone != null) "high" else "medium",
            rawMessageHash = MessageNormalizer.sha256(normalized),
            deviceId = deviceId
        )

        return PaymentParseResult.Success(
            event = event,
            detailsExplanation = "تم بنجاح قراءة إشعار استلام فودافون كاش بمبلغ ${event.amountInMajorUnits} ج.م ورقم عملية $transactionReference"
        )
    }
}
