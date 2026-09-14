package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.PaymentChannel
import com.elmallah.paymentbridge.domain.PaymentProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DuplicateDetectionTest {

    @Test
    fun testDeterministicFingerprintMatchesAcrossIdenticalTransaction() {
        val event1 = PaymentBridgeEvent(
            eventId = "evt-1",
            provider = PaymentProvider.VODAFONE_CASH,
            paymentChannel = PaymentChannel.VODAFONE_CASH,
            amountMinor = 18000L,
            transactionReference = "023686770281",
            payerPhone = "01015192040",
            sourceSender = "VF-Cash",
            sourcePackage = "com.samsung.android.messaging",
            notificationPostedAt = 1000L,
            capturedAt = 1050L,
            parserVersion = "1.0",
            parseConfidence = "high",
            rawMessageHash = "hash1",
            deviceId = "dev-1"
        )

        val event2 = PaymentBridgeEvent(
            eventId = "evt-2",
            provider = PaymentProvider.VODAFONE_CASH,
            paymentChannel = PaymentChannel.VODAFONE_CASH,
            amountMinor = 18000L,
            transactionReference = "023686770281",
            payerPhone = "01015192040",
            sourceSender = "VF-Cash",
            sourcePackage = "com.samsung.android.messaging",
            notificationPostedAt = 2000L,
            capturedAt = 2050L,
            parserVersion = "1.0",
            parseConfidence = "high",
            rawMessageHash = "hash2",
            deviceId = "dev-1"
        )

        // Both events identify the same underlying financial transaction
        assertEquals(event1.fingerprint, event2.fingerprint)
        assertEquals("vodafone_cash:023686770281", event1.fingerprint)
    }

    @Test
    fun testDifferentReferenceHasDifferentFingerprint() {
        val event1 = PaymentBridgeEvent(
            eventId = "evt-1",
            provider = PaymentProvider.VODAFONE_CASH,
            paymentChannel = PaymentChannel.VODAFONE_CASH,
            amountMinor = 18000L,
            transactionReference = "023686770281",
            payerPhone = "01015192040",
            sourceSender = "VF-Cash",
            sourcePackage = "com.samsung.android.messaging",
            notificationPostedAt = 1000L,
            capturedAt = 1050L,
            parserVersion = "1.0",
            parseConfidence = "high",
            rawMessageHash = "hash1",
            deviceId = "dev-1"
        )

        val event2 = PaymentBridgeEvent(
            eventId = "evt-2",
            provider = PaymentProvider.VODAFONE_CASH,
            paymentChannel = PaymentChannel.VODAFONE_CASH,
            amountMinor = 18000L,
            transactionReference = "023686770282",
            payerPhone = "01015192040",
            sourceSender = "VF-Cash",
            sourcePackage = "com.samsung.android.messaging",
            notificationPostedAt = 1000L,
            capturedAt = 1050L,
            parserVersion = "1.0",
            parseConfidence = "high",
            rawMessageHash = "hash1",
            deviceId = "dev-1"
        )

        assertNotEquals(event1.fingerprint, event2.fingerprint)
    }
}
