package com.example.paymentbridge.parser

import com.example.paymentbridge.domain.RawNotificationMessage

class CompositePaymentParser(
    private val parsers: List<PaymentMessageParser> = listOf(
        VodafoneCashParser(),
        NbeIncomingTransferParser()
    )
) {

    fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult {
        // First try parsers where canHandle is true
        for (parser in parsers) {
            if (parser.canHandle(message)) {
                return parser.parse(message, deviceId)
            }
        }

        // Fallback: If title was generic (e.g. from an SMS app notification without sender name in title),
        // try all parsers to see if any can parse the body successfully
        for (parser in parsers) {
            val result = parser.parse(message, deviceId)
            if (result is PaymentParseResult.Success) {
                return result
            }
        }

        return PaymentParseResult.Ignored(
            reason = IgnoreReason.IGNORED_NON_PAYMENT_SENDER,
            message = "الرسالة ليست من مرسل مدفوعات معتمد (VF-Cash أو Bank-AlAhly)"
        )
    }
}
