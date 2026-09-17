package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.domain.PaymentBridgeEvent

enum class IgnoreReason {
    IGNORED_OUTGOING_TRANSFER,
    IGNORED_BALANCE_MESSAGE,
    IGNORED_RECHARGE,
    IGNORED_NON_PAYMENT_SENDER,
    IGNORED_PROMOTIONAL,
    IGNORED_OTHER
}

enum class FailureReason {
    UNKNOWN_MESSAGE_FORMAT,
    MISSING_AMOUNT,
    AMOUNT_EXTRACTION_FAILED,
    AMOUNT_INVALID,
    MISSING_REFERENCE,
    INVALID_PAYER_PHONE,
    MALFORMED_DATA
}

sealed class PaymentParseResult {
    data class Success(
        val event: PaymentBridgeEvent,
        val detailsExplanation: String = "تم استخراج بيانات الدفع بنجاح"
    ) : PaymentParseResult()

    data class Ignored(
        val reason: IgnoreReason,
        val message: String
    ) : PaymentParseResult()

    data class Failed(
        val reason: FailureReason,
        val message: String
    ) : PaymentParseResult()
}
