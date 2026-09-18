package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.capture.SourceValidationResult
import com.elmallah.paymentbridge.capture.TrustedNotificationSourcePolicy
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.PaymentRuleStore
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.CompositePaymentParser
import com.elmallah.paymentbridge.parser.PaymentParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityPolicyTest {

    private val parser = CompositePaymentParser()

    @Test
    fun testRejectArbitraryPackage() {
        // WhatsApp notification sending fake payment text
        val result = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.whatsapp",
            senderTitle = "VF-Cash"
        )
        assertTrue(result is SourceValidationResult.Rejected)
        assertTrue((result as SourceValidationResult.Rejected).reason.contains("غير موثوقة"))

        // Live processing through CompositePaymentParser must reject and ignore
        val raw = RawNotificationMessage(
            sourcePackage = "com.whatsapp",
            title = "VF-Cash",
            text = "تم استلام مبلغ 500 جنيه من 01012345678 رقم العملية: 123456789012",
            postedAtMillis = System.currentTimeMillis()
        )
        val parseResult = parser.parseLiveMessage(raw, "device-123")
        assertTrue(parseResult is PaymentParseResult.Ignored)
    }

    @Test
    fun testRejectInvalidOrSpoofedSender() {
        // Samsung Messages from an unknown or spoofed sender
        val result = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.samsung.android.messaging",
            senderTitle = "Fake-Cash-Scam"
        )
        assertTrue(result is SourceValidationResult.Rejected)

        // Google Messages with an arbitrary personal contact
        val result2 = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.google.android.apps.messaging",
            senderTitle = "Mohamed Hassan"
        )
        assertTrue(result2 is SourceValidationResult.Rejected)
    }

    @Test
    fun testAcceptOnlyTrustedMessagingAppAndApprovedSender() {
        val authorizedRules = PaymentRuleStore.getDefaultRules()

        // Samsung Messages + VF-Cash only when explicitly authorized
        val vfSamsung = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.samsung.android.messaging",
            senderTitle = "VF-Cash",
            rules = authorizedRules
        )
        assertTrue(vfSamsung is SourceValidationResult.Accepted)
        assertEquals(PaymentProvider.VODAFONE_CASH, (vfSamsung as SourceValidationResult.Accepted).provider)

        // Google Messages + Bank-AlAhly
        val nbeGoogle = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.google.android.apps.messaging",
            senderTitle = "Bank-AlAhly",
            rules = authorizedRules
        )
        assertTrue(nbeGoogle is SourceValidationResult.Accepted)
        assertEquals(PaymentProvider.NBE_INCOMING_TRANSFER, (nbeGoogle as SourceValidationResult.Accepted).provider)
    }
}
