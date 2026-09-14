package com.example.paymentbridge

import com.example.paymentbridge.domain.PaymentChannel
import com.example.paymentbridge.domain.PaymentProvider
import com.example.paymentbridge.domain.RawNotificationMessage
import com.example.paymentbridge.parser.CompositePaymentParser
import com.example.paymentbridge.parser.IgnoreReason
import com.example.paymentbridge.parser.MessageNormalizer
import com.example.paymentbridge.parser.NbeIncomingTransferParser
import com.example.paymentbridge.parser.PaymentParseResult
import com.example.paymentbridge.parser.VodafoneCashParser
import com.example.paymentbridge.security.HmacSigner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentParsersTest {

    private val deviceId = "test-device-pos-001"

    @Test
    fun `test message normalizer handles arabic digits and invisible chars`() {
        val input = "\u200Eتم استلام مبلغ ١٨٠.٠٠ جنيه من ٠١٠١٢٣٤٥٦٧٨"
        val normalized = MessageNormalizer.normalizeForParsing(input)

        assertTrue(normalized.contains("180.00"))
        assertTrue(normalized.contains("01012345678"))
        assertEquals(18000L, MessageNormalizer.parseAmountToMinor("180.00"))
        assertEquals(28722L, MessageNormalizer.parseAmountToMinor("287.22"))
    }

    @Test
    fun `test vodafone cash incoming payment parsing`() {
        val parser = VodafoneCashParser()
        val rawMsg = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "VF-Cash",
            text = """
                تم استلام مبلغ 180.00 جنيه من 01012345678
                المسجل باسم احمد علي
                على رقم محفظتك 01098765432
                بتاريخ 14-09-26 01:50
                رصيدك الحالي: 258.56 جنيه
                رقم العملية: 023686770281
            """.trimIndent()
        )

        val result = parser.parse(rawMsg, deviceId)
        assertTrue(result is PaymentParseResult.Success)

        val event = (result as PaymentParseResult.Success).event
        assertEquals(PaymentProvider.VODAFONE_CASH, event.provider)
        assertEquals(PaymentChannel.VODAFONE_CASH, event.paymentChannel)
        assertEquals(18000L, event.amountMinor)
        assertEquals(180.00, event.amountInMajorUnits, 0.001)
        assertEquals("01012345678", event.payerPhone)
        assertEquals("01098765432", event.walletPhone)
        assertEquals("023686770281", event.transactionReference)
        assertEquals("vodafone_cash:023686770281:18000", event.fingerprint)
    }

    @Test
    fun `test vodafone cash balance inquiry is ignored`() {
        val parser = VodafoneCashParser()
        val rawMsg = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "VF-Cash",
            text = "رصيد حسابك في فودافون كاش الحالي هو 258.56 جنيه. لمعرفة تفاصيل العمليات السابقة اطلب #9*."
        )

        val result = parser.parse(rawMsg, deviceId)
        assertTrue(result is PaymentParseResult.Ignored)
        assertEquals(IgnoreReason.IGNORED_BALANCE_MESSAGE, (result as PaymentParseResult.Ignored).reason)
    }

    @Test
    fun `test vodafone cash outgoing transfer is ignored`() {
        val parser = VodafoneCashParser()
        val rawMsg = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "VF-Cash",
            text = "تم تحويل مبلغ 100.00 جنيه من محفظتك إلى 01011112222 رقم العملية: 99887766."
        )

        val result = parser.parse(rawMsg, deviceId)
        assertTrue(result is PaymentParseResult.Ignored)
        assertEquals(IgnoreReason.IGNORED_OUTGOING_TRANSFER, (result as PaymentParseResult.Ignored).reason)
    }

    @Test
    fun `test nbe incoming transfer parsing with standard spelling`() {
        val parser = NbeIncomingTransferParser()
        val rawMsg = RawNotificationMessage(
            sourcePackage = "com.samsung.android.messaging",
            title = "Bank-AlAhly",
            text = """
                تم اضافه تحويل 287.22 EGP
                لحساب 0013
                مرجع 599INTA2625604XW
            """.trimIndent()
        )

        val result = parser.parse(rawMsg, deviceId)
        assertTrue(result is PaymentParseResult.Success)

        val event = (result as PaymentParseResult.Success).event
        assertEquals(PaymentProvider.NBE_INCOMING_TRANSFER, event.provider)
        assertEquals(PaymentChannel.INSTAPAY_OR_BANK_TRANSFER, event.paymentChannel)
        assertEquals(28722L, event.amountMinor)
        assertEquals(287.22, event.amountInMajorUnits, 0.001)
        assertEquals("599INTA2625604XW", event.transactionReference)
        assertEquals("0013", event.accountLast4)
        // Bank SMS does not include payer phone; must remain null
        assertNull(event.payerPhone)
    }

    @Test
    fun `test nbe incoming transfer with alt spelling and arabic digits`() {
        val parser = NbeIncomingTransferParser()
        val rawMsg = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "Bank-AlAhly",
            text = "تم إضافة تحويل ١٥٠.٥٠ EGP لحساب رقم ٠٠١٣ مرجع 9988INTA776655"
        )

        val result = parser.parse(rawMsg, deviceId)
        assertTrue(result is PaymentParseResult.Success)

        val event = (result as PaymentParseResult.Success).event
        assertEquals(15050L, event.amountMinor)
        assertEquals("0013", event.accountLast4)
        assertEquals("9988INTA776655", event.transactionReference)
    }

    @Test
    fun `test nbe outgoing transfer is ignored`() {
        val parser = NbeIncomingTransferParser()
        val rawMsg = RawNotificationMessage(
            sourcePackage = "com.samsung.android.messaging",
            title = "Bank-AlAhly",
            text = "تم تنفيذ تحويل لحظي من حسابكم رقم 0013 بمبلغ 250.00 جم إلى محفظة فودافون كاش رقم مرجعي 98827361."
        )

        val result = parser.parse(rawMsg, deviceId)
        assertTrue(result is PaymentParseResult.Ignored)
        assertEquals(IgnoreReason.IGNORED_OUTGOING_TRANSFER, (result as PaymentParseResult.Ignored).reason)
    }

    @Test
    fun `test composite parser delegates correctly`() {
        val composite = CompositePaymentParser()
        val vfMsg = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "VF-Cash",
            text = "تم استلام مبلغ 50.00 جنيه من 01000000000 على رقم محفظتك 01011111111 رقم العملية: 12345"
        )
        val nbeMsg = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "Bank-AlAhly",
            text = "تم اضافه تحويل 100 EGP لحساب 0013 مرجع REF123"
        )

        val vfResult = composite.parse(vfMsg, deviceId)
        val nbeResult = composite.parse(nbeMsg, deviceId)

        assertTrue(vfResult is PaymentParseResult.Success)
        assertTrue(nbeResult is PaymentParseResult.Success)
    }

    @Test
    fun `test hmac signing produces deterministic valid signature`() {
        val timestamp = 1726300000000L
        val nonce = "test-nonce-12345"
        val body = """{"amountMinor":18000,"transactionReference":"023686770281"}"""
        val secret = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2"

        val sig1 = HmacSigner.sign(timestamp, nonce, body, secret)
        val sig2 = HmacSigner.sign(timestamp, nonce, body, secret)

        assertEquals(sig1, sig2)
        assertEquals(64, sig1.length) // 256 bits = 64 hex chars
    }
}
