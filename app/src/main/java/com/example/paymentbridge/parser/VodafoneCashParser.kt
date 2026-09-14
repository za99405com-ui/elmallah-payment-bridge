package com.example.paymentbridge.parser

import com.example.paymentbridge.domain.PaymentBridgeEvent
import com.example.paymentbridge.domain.PaymentChannel
import com.example.paymentbridge.domain.PaymentProvider
import com.example.paymentbridge.domain.RawNotificationMessage
import java.util.UUID

class VodafoneCashParser : PaymentMessageParser {

    override fun canHandle(message: RawNotificationMessage): Boolean {
        val title = message.title.trim()
        val text = message.fullText
        val senderMatch = title.contains("VF-Cash", ignoreCase = true) ||
                title.contains("Vodafone", ignoreCase = true) ||
                title.contains("فودافون", ignoreCase = true) ||
                message.sourcePackage.contains("vodafone", ignoreCase = true)

        val contentMatch = text.contains("فودافون كاش") ||
                text.contains("VF-Cash", ignoreCase = true) ||
                (text.contains("تم استلام مبلغ") || text.contains("تم إستلام مبلغ")) &&
                (text.contains("جنيه") || text.contains("محفظتك") || text.contains("رقم العملية"))

        return senderMatch || contentMatch
    }

    override fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val rawText = message.fullText
        val normalized = MessageNormalizer.normalizeForParsing(rawText)

        // 1. Filter out outgoing / deductions
        if (normalized.contains("تم خصم") ||
            normalized.contains("تم سحب") ||
            normalized.contains("تم تحويل") && normalized.contains("من محفظتك") ||
            normalized.contains("سداد فاتورة") ||
            normalized.contains("شراء كارت")
        ) {
            return PaymentParseResult.Ignored(
                reason = IgnoreReason.IGNORED_OUTGOING_TRANSFER,
                message = "عملية صادرة أو خصم من المحفظة وليست مدفوعات واردة"
            )
        }

        // 2. Filter out mobile recharge
        if (normalized.contains("تم شحن رصيد") ||
            normalized.contains("كروت شحن") ||
            normalized.contains("شحن رصيد موبايلك")
        ) {
            return PaymentParseResult.Ignored(
                reason = IgnoreReason.IGNORED_RECHARGE,
                message = "رسالة شحن رصيد وليست إيداعاً من عميل"
            )
        }

        // 3. Filter out balance inquiries
        val isBalanceOnly = (normalized.contains("رصيد حسابك") ||
                normalized.contains("رصيدك في فودافون كاش") ||
                normalized.contains("رصيدك الحالي هو")) &&
                !normalized.contains("تم استلام مبلغ") &&
                !normalized.contains("تم إستلام مبلغ")
        if (isBalanceOnly) {
            return PaymentParseResult.Ignored(
                reason = IgnoreReason.IGNORED_BALANCE_MESSAGE,
                message = "رسالة استعلام عن الرصيد وليست إيداعاً"
            )
        }

        // 4. Must contain the core incoming payment phrase
        val hasIncomingTrigger = normalized.contains("تم استلام مبلغ") || normalized.contains("تم إستلام مبلغ")
        if (!hasIncomingTrigger) {
            return PaymentParseResult.Ignored(
                reason = IgnoreReason.IGNORED_PROMOTIONAL,
                message = "رسالة فودافون كاش لا تتضمن إشعار استلام أموال واردة"
            )
        }

        // 5. Extract amount
        val amountRegex = Regex("تم\\s*(?:استلام|إستلام)\\s*مبلغ\\s*([\\d\\.]+)")
        val amountMatch = amountRegex.find(normalized)
        if (amountMatch == null) {
            return PaymentParseResult.Failed(
                reason = FailureReason.MISSING_AMOUNT,
                message = "تعذر استخراج المبلغ المالي من الرسالة"
            )
        }
        val amountStr = amountMatch.groupValues[1]
        val amountMinor = MessageNormalizer.parseAmountToMinor(amountStr)
            ?: return PaymentParseResult.Failed(
                reason = FailureReason.MISSING_AMOUNT,
                message = "صيغة المبلغ غير صالحة: $amountStr"
            )

        // 6. Extract payer phone (following 'من')
        val payerPhoneRegex = Regex("من\\s*(\\+?\\d{10,14})")
        val payerPhoneRaw = payerPhoneRegex.find(normalized)?.groupValues?.get(1)
        val normalizedPayerPhone = MessageNormalizer.normalizeEgyptianPhone(payerPhoneRaw)

        // 7. Extract wallet phone (following 'على رقم محفظتك')
        val walletPhoneRegex = Regex("على\\s*رقم\\s*محفظت(?:ك|كم)?\\s*[:\\s]*(\\+?\\d{10,14})")
        val walletPhoneRaw = walletPhoneRegex.find(normalized)?.groupValues?.get(1)
        val normalizedWalletPhone = MessageNormalizer.normalizeEgyptianPhone(walletPhoneRaw)

        // 8. Extract transaction reference
        val referenceRegex = Regex("رقم\\s*(?:العملية|العمليه)\\s*[:\\s]*([A-Za-z0-9]+)")
        val referenceMatch = referenceRegex.find(normalized)
        if (referenceMatch == null) {
            return PaymentParseResult.Failed(
                reason = FailureReason.MISSING_REFERENCE,
                message = "تعذر استخراج رقم العملية / المرجع من الرسالة"
            )
        }
        val transactionReference = referenceMatch.groupValues[1].trim()

        val rawHash = MessageNormalizer.sha256(normalized)
        val eventId = UUID.randomUUID().toString()

        val event = PaymentBridgeEvent(
            eventId = eventId,
            provider = PaymentProvider.VODAFONE_CASH,
            paymentChannel = PaymentChannel.VODAFONE_CASH,
            amountMinor = amountMinor,
            currency = "EGP",
            payerPhone = normalizedPayerPhone,
            walletPhone = normalizedWalletPhone,
            transactionReference = transactionReference,
            accountLast4 = null,
            sourceSender = message.title.ifBlank { "VF-Cash" },
            sourcePackage = message.sourcePackage,
            notificationPostedAt = message.postedAtMillis,
            capturedAt = System.currentTimeMillis(),
            parserVersion = "1.0",
            confidence = if (normalizedPayerPhone != null && transactionReference.isNotEmpty()) "high" else "medium",
            rawMessageHash = rawHash,
            deviceId = deviceId
        )

        return PaymentParseResult.Success(
            event = event,
            detailsExplanation = "تم بنجاح استخراج عملية فودافون كاش واردة بقيمة ${event.amountInMajorUnits} ج.م من الهاتف ${normalizedPayerPhone ?: "غير محدد"}"
        )
    }
}
