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
        "تم تنفيذ تحويل إلى", "تم تحويل إلى", "خصم", "سحب", "مدفوعات", "شراء",
        "debited", "paid", "withdrawn", "sent to"
    )

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
        val isOutgoing = OUTGOING_KEYWORDS.any { normalized.contains(it, ignoreCase = true) }
        val isIncoming = INCOMING_KEYWORDS.any { normalized.contains(it, ignoreCase = true) }
        val operationType = if (isOutgoing && !isIncoming) "تحويل صادر (يُتجاهل)" else "استلام أموال"

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
        val generalAmountRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة|تحويل|استلام|إيداع|ايداع|credited|received)?\s*(\d+(?:[.,]\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP|LE)?"""

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
            lastTestedSuccess = true,
            parserType = existingRule?.parserType ?: "RULE_BASED",
            enabled = existingRule?.enabled ?: true
        )
    }

    private fun extractAmountMinor(text: String): Long? {
        val patterns = listOf(
            // "180.00 جنيه" or "1,250.50 جم" or "287.22 EGP"
            Regex("""(?:مبلغ|بمبلغ|قيمة|بقيمة|تحويل|استلام|إيداع|ايداع|credited|received)?\s*(\d+(?:[.,]\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP|LE)""", RegexOption.IGNORE_CASE),
            // "جنيه 250" or "EGP 250"
            Regex("""(?:جم|جنيه|ج\.م|EGP|LE)\s*(\d+(?:[.,]\d{1,2})?)""", RegexOption.IGNORE_CASE),
            // Standalone decimal amount
            Regex("""\b(\d{1,6}\.\d{2})\b""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val numStr = match.groupValues[1].replace(",", "").trim()
                val doubleVal = numStr.toDoubleOrNull()
                if (doubleVal != null && doubleVal > 0.0) {
                    return Math.round(doubleVal * 100)
                }
            }
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
