package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.domain.PaymentChannel
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.PaymentParseResult
import com.elmallah.paymentbridge.parser.VodafoneCashParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VodafoneCashParserTest {

    private val parser = VodafoneCashParser()

    @Test
    fun testParseRealIncomingTransfer() {
        val body = """
            تم استلام مبلغ 180.00 جنيه من 01015192040
            المسجل باسم احمد علي
            على رقم محفظتك 01098765432
            بتاريخ 14-09-26 01:50
            رصيدك الحالي: 258.56 جنيه
            رقم العملية: 023686770281
        """.trimIndent()

        val raw = RawNotificationMessage(
            sourcePackage = "com.samsung.android.messaging",
            title = "VF-Cash",
            text = body,
            postedAtMillis = 1726317000000L
        )

        val result = parser.parse(raw, "device-pos-1")
        assertTrue(result is PaymentParseResult.Success)

        val event = (result as PaymentParseResult.Success).event
        assertEquals(PaymentProvider.VODAFONE_CASH, event.provider)
        assertEquals(PaymentChannel.VODAFONE_CASH, event.paymentChannel)
        assertEquals(18000L, event.amountMinor)
        assertEquals(180.00, event.amountInMajorUnits, 0.001)
        assertEquals("01015192040", event.payerPhone)
        assertEquals("01098765432", event.walletPhone)
        assertEquals("023686770281", event.transactionReference)
        assertEquals("high", event.parseConfidence)
        assertNotNull(event.rawMessageHash)
        assertEquals("vodafone_cash:023686770281", event.fingerprint)
    }

    @Test
    fun testIgnoreBalanceInquiry() {
        val body = "رصيد حسابك في فودافون كاش الحالي هو 258.56 جنيه. لمعرفة تفاصيل العمليات السابقة اطلب #9*."
        val raw = RawNotificationMessage(
            sourcePackage = "com.samsung.android.messaging",
            title = "VF-Cash",
            text = body,
            postedAtMillis = System.currentTimeMillis()
        )

        val result = parser.parse(raw, "device-pos-1")
        assertTrue(result is PaymentParseResult.Ignored)
    }

    @Test
    fun testIgnoreOutgoingTransfer() {
        val body = "تم تحويل مبلغ 100 جنيه إلى رقم 01023456789 بنجاح. مصاريف الخدمة 1 جنيه. رقم العملية 987654321012"
        val raw = RawNotificationMessage(
            sourcePackage = "com.samsung.android.messaging",
            title = "VF-Cash",
            text = body,
            postedAtMillis = System.currentTimeMillis()
        )

        val result = parser.parse(raw, "device-pos-1")
        assertTrue(result is PaymentParseResult.Ignored)
    }
}
