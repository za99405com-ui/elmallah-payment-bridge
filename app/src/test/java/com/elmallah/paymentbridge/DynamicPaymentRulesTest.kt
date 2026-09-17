package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.capture.SourceValidationResult
import com.elmallah.paymentbridge.capture.TrustedNotificationSourcePolicy
import com.elmallah.paymentbridge.domain.PaymentRuleStore
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.CompositePaymentParser
import com.elmallah.paymentbridge.parser.PaymentParseResult
import com.elmallah.paymentbridge.parser.RuleBasedPaymentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicPaymentRulesTest {

    @Test
    fun testRuleBasedParsingWithArabicIndicNumerals() {
        val rule = PaymentSourceRule(
            id = "BANQUE_MISR_TEST",
            name = "بنك مصر",
            paymentChannel = "BANK_TRANSFER",
            packageNames = listOf("com.google.android.apps.messaging"),
            senderFilters = listOf("BanqueMisr"),
            enabled = true
        )

        val parser = RuleBasedPaymentParser(rule)
        val raw = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "BanqueMisr",
            text = "تم إضافة تحويل بمبلغ ١٬٢٥٠٫٠٠ جم لحسابك رقم *4321 من 01223344556 مرجع: BM987654",
            postedAtMillis = System.currentTimeMillis()
        )

        val result = parser.parse(raw, "device-test-1")
        assertTrue("Parsing should succeed", result is PaymentParseResult.Success)
        val event = (result as PaymentParseResult.Success).event

        assertEquals("BANQUE_MISR_TEST", event.paymentSourceId)
        assertEquals(125000L, event.amountMinor)
        assertEquals("01223344556", event.payerPhone)
        assertEquals("4321", event.accountLast4)
        assertEquals("BM987654", event.transactionReference)
    }

    @Test
    fun testDynamicRuleRoutingThroughCompositeParser() {
        val customRule = PaymentSourceRule(
            id = "INSTAPAY_CUSTOM",
            name = "إنستاباي فوري",
            paymentChannel = "INSTAPAY",
            packageNames = listOf("com.google.android.apps.messaging"),
            senderFilters = listOf("InstaPay"),
            amountExtractionRegex = """([0-9]+(?:\.[0-9]{2})?)\s*EGP""",
            enabled = true
        )

        val rulesList = listOf(customRule)
        val composite = CompositePaymentParser(ruleSupplier = { rulesList })

        // 1. Valid InstaPay notification
        val raw = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "InstaPay",
            text = "You received 450.50 EGP from 01155667788 Ref: IP-998877",
            postedAtMillis = System.currentTimeMillis()
        )

        val result = composite.parseLiveMessage(raw, "device-123")
        assertTrue("Live message should be parsed via dynamic rule", result is PaymentParseResult.Success)
        val event = (result as PaymentParseResult.Success).event
        assertEquals("INSTAPAY_CUSTOM", event.paymentSourceId)
        assertEquals(45050L, event.amountMinor)
        assertEquals("01155667788", event.payerPhone)
        assertEquals("IP-998877", event.transactionReference)
    }

    @Test
    fun testDisabledDynamicRuleRejection() {
        val disabledRule = PaymentSourceRule(
            id = "DISABLED_PROVIDER",
            name = "مزود معطل",
            paymentChannel = "WALLET",
            packageNames = listOf("com.google.android.apps.messaging"),
            senderFilters = listOf("DisabledSender"),
            enabled = false
        )

        val validation = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.google.android.apps.messaging",
            senderTitle = "DisabledSender",
            rules = listOf(disabledRule)
        )

        assertTrue("Disabled rule must be rejected", validation is SourceValidationResult.Rejected)
    }

    @Test
    fun testDefaultRulesIncludeVodafoneAndNbe() {
        val defaults = PaymentRuleStore.getDefaultRules()
        assertTrue(defaults.any { it.id == "vodafone_cash" })
        assertTrue(defaults.any { it.id == "nbe_incoming_transfer" })
        assertTrue(defaults.any { it.id == "instapay_egypt" })
    }
}
