package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.domain.PaymentMessageSample
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import java.text.DecimalFormat
import java.util.Locale

data class AutoDetectionResult(
    val success: Boolean,
    val detectedAmountMinor: Long? = null,
    val formattedAmount: String? = null,
    val payerPhone: String? = null,
    val accountIdentifier: String? = null,
    val transactionReference: String? = null,
    val operationType: String = "استلام أموال",
    val statusMessage: String = ""
)

/**
 * Derives safe, general payment parsing patterns from user-provided notification samples.
 * Crucially, it identifies variable dynamic tokens (e.g. varying amounts and phone numbers)
 * rather than hardcoding literal numbers from a specific test message.
 */
object AutoRuleGenerator {

    private val amountFormat = DecimalFormat("#,##0.00")

    // Common Egyptian financial incoming indicators
    private val INCOMING_KEYWORDS = listOf(
        "استلام", "تم استلام", "تحويل", "تم تحويل", "إضافة", "تم إضافة", "اضافة", "تم اضافه",
        "إيداع", "ايداع", "وارد", "received", "credited", "deposit", "transfer"
    )

    private val OUTGOING_KEYWORDS = listOf(
        "خصم", "تم الخصم", "سحب", "تم السحب", "شراء", "مدفوعات",
        "debited", "withdrawn", "sent to", "paid to"
    )

    private fun isClearlyOutgoing(text: String): Boolean {
        val merchantRecipient = listOf(
            "إلى حسابك", "الى حسابك", "لحسابك", "إلى محفظتك", "الى محفظتك"
        ).any { text.contains(it, ignoreCase = true) }

        if (OUTGOING_KEYWORDS.any { text.contains(it, ignoreCase = true) }) return true
        if (text.contains("من حسابكم", ignoreCase = true) || text.contains("من حسابك", ignoreCase = true)) {
            if (text.contains(" إلى ", ignoreCase = true) || text.contains(" الى ", ignoreCase = true)) return true
        }

        val transferToOtherParty = Regex(
            """(?:تم\s*)?(?:تنفيذ\s*)?تحويل.*(?:\sإلى\s|\sالى\s)""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(text)

        return transferToOtherParty && !merchantRecipient
    }

    /**
     * Analyzes a single message sample to extract amount, phone, ref, and direction.
     */
    fun analyzeSample(title: String, rawBody: String): AutoDetectionResult {
        if (rawBody.isBlank()) {
            return AutoDetectionResult(
                success = false,
                statusMessage = "نص الرسالة فارغ"
            )
        }

        val normalized = MessageNormalizer.normalizeForParsing(rawBody)

        // 1. Check operation direction
        val isOutgoing = isClearlyOutgoing(normalized)
        val operationType = if (isOutgoing) "تحويل صادر (يُتجاهل)" else "استلام أموال"

        if (isOutgoing) {
            return AutoDetectionResult(
                success = false,
                operationType = operationType,
                statusMessage = "الرسالة تبدو عملية صادرة وليست استلام أموال"
            )
        }

        // 2. Extract Amount
        val amountMinor = extractAmountMinor(normalized)
        if (amountMinor == null || amountMinor <= 0L) {
            return AutoDetectionResult(
                success = false,
                operationType = operationType,
                statusMessage = "لم نستطع تحديد المبلغ من نص الإشعار"
            )
        }

        // 3. Extract Payer Phone
        val phone = extractPayerPhone(normalized)

        // 4. Extract Account Last 4
        val accountLast4 = extractAccountLast4(normalized)

        // 5. Extract Transaction Reference
        val ref = extractTransactionReference(normalized)

        val formatted = "${amountFormat.format(amountMinor / 100.0)} جنيه"

        return AutoDetectionResult(
            success = true,
            detectedAmountMinor = amountMinor,
            formattedAmount = formatted,
            payerPhone = phone,
            accountIdentifier = accountLast4,
            transactionReference = ref,
            operationType = operationType,
            statusMessage = "تم التعرف على الرسالة بنجاح"
        )
    }

    /**
     * Analyzes multiple samples and derives a PaymentSourceRule with generalized regex.
     */
    fun buildRuleFromSamples(
        existingRule: PaymentSourceRule?,
        ruleId: String,
        sourceName: String,
        selectedPackage: String,
        appName: String?,
        samples: List<PaymentMessageSample>
    ): PaymentSourceRule {
        // Collect distinct sender titles
        val senders = samples.map { it.title.trim() }.filter { it.isNotBlank() }.distinct()

        // Analyze all samples to determine common vocabulary
        val normalizedBodies: List<String> = samples.map { MessageNormalizer.normalizeForParsing(it.body) }

        // Find recurring incoming keywords present across samples
        val detectedKeywords = mutableListOf<String>()
        for (kw in INCOMING_KEYWORDS) {
            if (normalizedBodies.any { it.contains(kw, ignoreCase = true) }) {
                detectedKeywords.add(kw)
            }
        }
        val safeBodyKeywords = if (detectedKeywords.isNotEmpty()) {
            detectedKeywords.take(4)
        } else {
            listOf("استلام", "تحويل", "إضافة")
        }

        // General non-hardcoded amount extraction regex
        val generalAmountRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة|تحويل|استلام|إيداع|ايداع|credited|received)?\s*([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:[\.,][0-9]{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP|LE)?"""

        // General non-hardcoded phone extraction regex
        val generalPhoneRegex = """(?:من|from)?\s*(01[0125][0-9]{8})\b"""

        // General account identifier regex
        val generalAccountRegex = """(?:حساب|لحسابك|account|card|بطاقة)?\s*(?:رقم)?\s*[\*xX]{1,}\s*([0-9]{4})\b"""

        val packages = if (selectedPackage.isNotBlank()) listOf(selectedPackage) else emptyList()

        return (existingRule ?: PaymentSourceRule(
            id = ruleId,
            name = sourceName
        )).copy(
            id = ruleId,
            name = sourceName,
            appName = appName?.ifBlank { null } ?: existingRule?.appName,
            packageNames = if (packages.isNotEmpty()) packages else existingRule?.packageNames ?: emptyList(),
            senderFilters = if (senders.isNotEmpty()) senders else existingRule?.senderFilters ?: emptyList(),
            bodyContains = safeBodyKeywords,
            amountExtractionRegex = existingRule?.amountExtractionRegex ?: generalAmountRegex,
            senderPhoneExtractionRegex = existingRule?.senderPhoneExtractionRegex ?: generalPhoneRegex,
            accountIdentifierRegex = existingRule?.accountIdentifierRegex ?: generalAccountRegex,
            sampleMessages = samples,
            isLocalDraft = true,
            lastTestedSuccess = samples.isNotEmpty() && samples.all { analyzeSample(it.title, it.body).success },
            parserType = existingRule?.parserType ?: "RULE_BASED",
            enabled = existingRule?.enabled ?: true
        )
    }

    private fun extractAmountMinor(text: String): Long? {
        val numberPattern = """([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:[\.,][0-9]{1,2})?)"""
        val patterns = listOf(
            Regex(
                """(?:مبلغ|بمبلغ|قيمة|بقيمة|تحويل|استلام|إيداع|ايداع|credited|received)?\s*$numberPattern\s*(?:جم|جنيه|ج\.م|EGP|LE)""",
                RegexOption.IGNORE_CASE
            ),
            Regex(
                """(?:جم|جنيه|ج\.م|EGP|LE)\s*$numberPattern""",
                RegexOption.IGNORE_CASE
            ),
            Regex("""\b([0-9]{1,6}\.[0-9]{2})\b""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            val candidate = match?.groupValues?.getOrNull(1) ?: continue
            val parsed = MessageNormalizer.parseAmountToMinor(candidate)
            if (parsed != null && parsed > 0L) return parsed
        }
        return null
    }

    private fun extractPayerPhone(text: String): String? {
        val phoneRegex = Regex("""\b(01[0125][0-9]{8})\b""")
        val match = phoneRegex.find(text)
        return match?.groupValues?.getOrNull(1)
    }

    private fun extractAccountLast4(text: String): String? {
        val accRegex = Regex("""(?:حساب|لحسابك|account|card|بطاقة)?\s*(?:رقم)?\s*[\*xX]{1,}\s*([0-9]{4})\b""", RegexOption.IGNORE_CASE)
        val match = accRegex.find(text)
        return match?.groupValues?.getOrNull(1)
    }

    private fun extractTransactionReference(text: String): String? {
        val refRegex = Regex(
            """(?:رقم العملية|المرجعي|رقم المرجع|مرجع|عملية رقم|ref(?:erence)?|rrn|txn)[:\s]*([A-Za-z0-9\-_]{6,30})""",
            RegexOption.IGNORE_CASE
        )
        val match = refRegex.find(text)
        return match?.groupValues?.getOrNull(1)
    }
}
