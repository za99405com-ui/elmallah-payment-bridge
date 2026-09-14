package com.example.paymentbridge.parser

import com.example.paymentbridge.domain.PaymentBridgeEvent
import com.example.paymentbridge.domain.PaymentChannel
import com.example.paymentbridge.domain.PaymentProvider
import com.example.paymentbridge.domain.RawNotificationMessage
import java.util.UUID

class NbeIncomingTransferParser : PaymentMessageParser {

    override fun canHandle(message: RawNotificationMessage): Boolean {
        val title = message.title.trim()
        val text = message.fullText
        val senderMatch = title.contains("Bank-AlAhly", ignoreCase = true) ||
                title.contains("AlAhly", ignoreCase = true) ||
                title.contains("NBE", ignoreCase = true) ||
                title.contains("البنك الأهلي") ||
                title.contains("البنك الاهلي")

        val contentMatch = text.contains("Bank-AlAhly", ignoreCase = true) ||
                text.contains("البنك الأهلي") ||
                (text.contains("تحويل") && (text.contains("إضافة") || text.contains("اضافه") || text.contains("اضافة") || text.contains("إضافه")))

        return senderMatch || contentMatch
    }

    override fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        val rawText = message.fullText
        val normalized = MessageNormalizer.normalizeForParsing(rawText)

        // 1. CRITICAL: Check for outgoing bank transfer (money leaving account)
        if (normalized.contains("تم تنفيذ تحويل لحظي من حسابكم") ||
            normalized.contains("تم تنفيذ تحويل من حسابكم") ||
            normalized.contains("خصم من حسابكم")
        ) {
            return PaymentParseResult.Ignored(
                reason = IgnoreReason.IGNORED_OUTGOING_TRANSFER,
                message = "تم تجاهل تحويل صادر من حساب البنك الأهلي (أموال خارجة وليست واردة)"
            )
        }

        // 2. Incoming transfer check (supports both spelling variants: إضافة and اضافه)
        val incomingTransferRegex = Regex("تم\\s*(?:إضافه|إضافة|اضافه|اضافة)\\s*تحويل\\s*([\\d\\.]+)\\s*(EGP|جم|جنيه|ج\\.?م)?")
        val transferMatch = incomingTransferRegex.find(normalized)

        if (transferMatch == null) {
            return PaymentParseResult.Ignored(
                reason = IgnoreReason.IGNORED_PROMOTIONAL,
                message = "الرسالة البنكية لا تحتوي على عبارة (تم إضافة تحويل) الواردة"
            )
        }

        // 3. Extract amount
        val amountStr = transferMatch.groupValues[1]
        val amountMinor = MessageNormalizer.parseAmountToMinor(amountStr)
            ?: return PaymentParseResult.Failed(
                reason = FailureReason.MISSING_AMOUNT,
                message = "صيغة المبلغ البنكي غير صالحة: $amountStr"
            )

        // 4. Extract account last 4 digits (e.g. "لحساب 0013")
        val accountRegex = Regex("لحساب\\s*(?:رقم\\s*)?(\\d{3,6})")
        val accountLast4 = accountRegex.find(normalized)?.groupValues?.get(1)

        // 5. Extract transaction reference (e.g. "مرجع 599INTA2625604XW")
        val referenceRegex = Regex("(?:مرجع|رقم\\s*مرجعي)\\s*[:\\s]*([A-Za-z0-9]+)")
        val referenceMatch = referenceRegex.find(normalized)
        if (referenceMatch == null) {
            return PaymentParseResult.Failed(
                reason = FailureReason.MISSING_REFERENCE,
                message = "تعذر استخراج رقم المرجع البنكي من الرسالة"
            )
        }
        val transactionReference = referenceMatch.groupValues[1].trim()

        val rawHash = MessageNormalizer.sha256(normalized)
        val eventId = UUID.randomUUID().toString()

        // IMPORTANT: payerPhone MUST remain null for NBE/InstaPay as bank SMS does NOT contain payer phone.
        val event = PaymentBridgeEvent(
            eventId = eventId,
            provider = PaymentProvider.NBE_INCOMING_TRANSFER,
            paymentChannel = PaymentChannel.INSTAPAY_OR_BANK_TRANSFER,
            amountMinor = amountMinor,
            currency = "EGP",
            payerPhone = null,
            walletPhone = null,
            transactionReference = transactionReference,
            accountLast4 = accountLast4,
            sourceSender = message.title.ifBlank { "Bank-AlAhly" },
            sourcePackage = message.sourcePackage,
            notificationPostedAt = message.postedAtMillis,
            capturedAt = System.currentTimeMillis(),
            parserVersion = "1.0",
            confidence = if (transactionReference.isNotEmpty()) "high" else "medium",
            rawMessageHash = rawHash,
            deviceId = deviceId
        )

        return PaymentParseResult.Success(
            event = event,
            detailsExplanation = "تم استخراج تحويل إنستاباي / البنك الأهلي بقيمة ${event.amountInMajorUnits} ج.م بمرجع $transactionReference لحساب ${accountLast4 ?: "غير محدد"}"
        )
    }
}
