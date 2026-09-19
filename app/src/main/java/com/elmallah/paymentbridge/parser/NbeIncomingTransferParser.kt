package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.PaymentChannel
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import java.util.UUID

class NbeIncomingTransferParser : PaymentMessageParser {

    override val providerName: String = PaymentProvider.NBE_INCOMING_TRANSFER

    override val supportedSenders: List<String> = listOf("Bank-AlAhly", "bank-alahly", "NBE", "AlAhly")

    // Handles both teh marbuta (ة) and heh (ه) and both hamza variants (إ / ا)
    private val incomingPattern = Regex(
        "تم\\s+[إا]ضاف[ةه]\\s+تحويل(?:\\s+لحظي)?[\\s\\S]{0,100}?(?:بمبلغ|مبلغ)\\s*([0-9.,\\u066B\\u066C]+)\\s*(?:EGP|جم|جنيه)?",
        RegexOption.IGNORE_CASE
    )

    private val accountPattern = Regex(
        "لحساب(?:كم|ك)?(?:\\s+رقم)?\\s*([0-9]+)",
        RegexOption.IGNORE_CASE
    )

    private val referencePattern = Regex(
        "(?:مرجع|رقم\\s+مرجعي|المرجع)\\s*[:\\s]*([0-9A-Za-z]+)",
        RegexOption.IGNORE_CASE
    )

    override fun canHandle(message: RawNotificationMessage): Boolean {
        val titleClean = message.title.trim()
        val senderMatches = supportedSenders.any { it.equals(titleClean, ignoreCase = true) }
        val text = MessageNormalizer.normalizeForParsing(message.fullText)
        return senderMatches || text.contains("Bank-AlAhly") || text.contains("البنك الأهلي")
    }

    override fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val normalized = MessageNormalizer.normalizeForParsing(message.fullText)

        // 1. Check for outgoing instant transfer, ATM withdrawal, or card purchase
        if (normalized.contains("تم تنفيذ تحويل") ||
            normalized.contains("من حسابكم") ||
            normalized.contains("خصم من حسابك") ||
            normalized.contains("خصم") ||
            normalized.contains("تم سحب") ||
            normalized.contains("سحب") ||
            normalized.contains("مشتريات") ||
            normalized.contains("شراء")
        ) {
            return PaymentParseResult.Ignored(
                IgnoreReason.IGNORED_OUTGOING_TRANSFER,
                "تم تجاهل الرسالة لأنها إشعار تنفيذ تحويل صادر أو سحب أو خصم وليست إضافة واردة"
            )
        }

        // 2. Check for balance inquiries
        if (!normalized.contains("تحويل") && (normalized.contains("رصيد حسابك") || normalized.contains("رصيدك المتاح"))) {
            return PaymentParseResult.Ignored(
                IgnoreReason.IGNORED_BALANCE_MESSAGE,
                "تم تجاهل الرسالة لأنها إشعار رصيد"
            )
        }

        // 3. Must strictly contain "تم إضافة تحويل" or "تم اضافه تحويل"
        val incomingMatch = incomingPattern.find(normalized)
            ?: return PaymentParseResult.Failed(
                FailureReason.UNKNOWN_MESSAGE_FORMAT,
                "الرسالة لا تحتوي على صيغة إضافة تحويل معتمدة من البنك الأهلي ('تم إضافة تحويل ... EGP')"
            )

        val rawAmount = incomingMatch.groupValues[1]
        val amountMinor = MessageNormalizer.parseAmountToMinor(rawAmount)
            ?: return PaymentParseResult.Failed(
                FailureReason.MISSING_AMOUNT,
                "فشل في استخراج المبلغ المالي من إشعار التحويل: '$rawAmount'"
            )

        // 4. Extract transaction reference
        val refMatch = referencePattern.find(normalized)
        val transactionReference = refMatch?.groupValues?.get(1)?.trim()
            ?: return PaymentParseResult.Failed(
                FailureReason.MISSING_REFERENCE,
                "لم يتم العثور على الرقم المرجعي للتحويل في إشعار البنك الأهلي"
            )

        // 5. Extract account last 4 digits
        val accMatch = accountPattern.find(normalized)
        val accountLast4 = accMatch?.groupValues?.get(1)?.let {
            if (it.length >= 4) it.takeLast(4) else it
        }

        // Bank SMS never contains payer phone; strictly null
        val payerPhone: String? = null

        val event = PaymentBridgeEvent(
            eventId = UUID.randomUUID().toString(),
            provider = PaymentProvider.NBE_INCOMING_TRANSFER,
            paymentChannel = PaymentChannel.INSTAPAY_OR_BANK_TRANSFER,
            amountMinor = amountMinor,
            currency = "EGP",
            payerPhone = payerPhone,
            walletPhone = null,
            transactionReference = transactionReference,
            accountLast4 = accountLast4,
            sourceSender = message.title.trim().ifEmpty { "Bank-AlAhly" },
            sourcePackage = message.sourcePackage,
            notificationPostedAt = message.postedAtMillis,
            capturedAt = System.currentTimeMillis(),
            parserVersion = "1.0",
            parseConfidence = "high", // parseConfidence != order match confidence!
            rawMessageHash = MessageNormalizer.sha256(normalized),
            deviceId = deviceId
        )

        return PaymentParseResult.Success(
            event = event,
            detailsExplanation = "تم بنجاح قراءة إشعار إضافة تحويل البنك الأهلي بمبلغ ${event.amountInMajorUnits} ج.م ومرجع $transactionReference لحساب $accountLast4"
        )
    }
}
