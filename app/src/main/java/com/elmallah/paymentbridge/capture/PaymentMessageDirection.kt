package com.elmallah.paymentbridge.capture

import com.elmallah.paymentbridge.parser.MessageNormalizer

/**
 * Shared payment-direction guard used by setup UX and live notification handling.
 */
object PaymentMessageDirection {

    fun isClearlyOutgoing(rawText: String): Boolean {
        val text = MessageNormalizer.normalizeForParsing(rawText)
        if (text.isBlank()) return false

        val hardOutgoing = listOf(
            "تم تنفيذ تحويل",
            "تم الخصم",
            "خصم",
            "تم سحب",
            "سحب",
            "شراء",
            "مشتريات",
            "debited",
            "withdrawn",
            "sent to",
            "paid to"
        )
        if (hardOutgoing.any { text.contains(it, ignoreCase = true) }) return true

        val fromMerchantAccount =
            text.contains("من حسابكم", ignoreCase = true) ||
                text.contains("من حسابك", ignoreCase = true)

        val toOtherParty =
            text.contains(" إلى ", ignoreCase = true) ||
                text.contains(" الى ", ignoreCase = true)

        return fromMerchantAccount && toOtherParty
    }

    fun looksLikeIncomingForSource(sourceCode: String?, rawText: String): Boolean {
        if (rawText.isBlank() || isClearlyOutgoing(rawText)) return false
        val text = MessageNormalizer.normalizeForParsing(rawText)

        return when (sourceCode) {
            "instapay" -> listOf(
                "تم إضافة تحويل",
                "تم اضافه تحويل",
                "لقد استلمت",
                "تم استلام تحويل",
                "received"
            ).any { text.contains(it, ignoreCase = true) }

            "vf_cash" -> listOf(
                "تم استلام مبلغ",
                "تم استلام",
                "استلمت"
            ).any { text.contains(it, ignoreCase = true) }

            else -> true
        }
    }
}
