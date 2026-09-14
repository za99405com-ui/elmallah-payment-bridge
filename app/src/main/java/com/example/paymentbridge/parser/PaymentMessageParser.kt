package com.example.paymentbridge.parser

import com.example.paymentbridge.domain.RawNotificationMessage

interface PaymentMessageParser {
    /**
     * Checks if this parser can handle the given raw notification message based on sender/title/content.
     */
    fun canHandle(message: RawNotificationMessage): Boolean

    /**
     * Parses the raw notification message into a PaymentParseResult.
     */
    fun parse(message: RawNotificationMessage, deviceId: String): PaymentParseResult
}
