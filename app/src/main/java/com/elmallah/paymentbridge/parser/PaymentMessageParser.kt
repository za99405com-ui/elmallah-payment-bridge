package com.elmallah.paymentbridge.parser

import com.elmallah.paymentbridge.domain.RawNotificationMessage

interface PaymentMessageParser {
    val providerName: String
    val supportedSenders: List<String>

    fun canHandle(message: RawNotificationMessage): Boolean
    fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult
}
