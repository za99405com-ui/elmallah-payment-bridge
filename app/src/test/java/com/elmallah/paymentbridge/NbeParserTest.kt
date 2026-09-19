package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.domain.PaymentChannel
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.NbeIncomingTransferParser
import com.elmallah.paymentbridge.parser.PaymentParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NbeParserTest {

    private val parser = NbeIncomingTransferParser()

    @Test
    fun testParseRealNbeIncomingTransfer() {
        val body = """
            تم اضافه تحويل 287.22 EGP
            لحساب 0013
            مرجع 599INTA2625604XW
        """.trimIndent()

        val raw = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "Bank-AlAhly",
            text = body,
            postedAtMillis = 1726318000000L
        )

        val result = parser.parse(raw, "device-pos-1")
        assertTrue(result is PaymentParseResult.Success)

        val event = (result as PaymentParseResult.Success).event
        assertEquals(PaymentProvider.NBE_INCOMING_TRANSFER, event.provider)
        assertEquals(PaymentChannel.INSTAPAY_OR_BANK_TRANSFER, event.paymentChannel)
        assertEquals(28722L, event.amountMinor)
        assertEquals(287.22, event.amountInMajorUnits, 0.001)
        assertEquals("0013", event.accountLast4)
        assertNull(event.payerPhone) // Explicitly null as bank SMS doesn't include payer phone
        assertEquals("599INTA2625604XW", event.transactionReference)
        assertEquals("high", event.parseConfidence)
        assertEquals("nbe_incoming_transfer:599INTA2625604XW", event.fingerprint)
    }

    @Test
    fun testParseActualNbeInstantTransferFormat() {
        val body = "تم إضافة تحويل لحظي لحسابكم رقم 0130 بمبلغ 100.00 جم من زياد معتز بالله السيد قاسم الملاح رقم مرجعي 955126971885 يوم 19-09 الساعة 09:28 للمزيد اتصل ب 19623"
        val raw = RawNotificationMessage(
            sourcePackage = "com.samsung.android.messaging",
            title = "Bank-AlAhly",
            text = body,
            postedAtMillis = System.currentTimeMillis()
        )

        val result = parser.parse(raw, "device-pos-actual")
        assertTrue(result is PaymentParseResult.Success)

        val event = (result as PaymentParseResult.Success).event
        assertEquals(10000L, event.amountMinor)
        assertEquals("0130", event.accountLast4)
        assertEquals("955126971885", event.transactionReference)
    }

    @Test
    fun testIgnoreNbeOutgoingTransfer() {
        val body = "تم تنفيذ تحويل لحظي من حسابكم رقم 0130 بمبلغ 180.00 جم إلى محفظة فودافون كاش رقم مرجعي 643086325070"
        val raw = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "Bank-AlAhly",
            text = body,
            postedAtMillis = System.currentTimeMillis()
        )

        val result = parser.parse(raw, "device-pos-1")
        assertTrue(result is PaymentParseResult.Ignored)
    }

    @Test
    fun testIgnoreNbeAtmWithdrawal() {
        val body = "تم سحب مبلغ 500.00 جم من بطاقتكم رقم 1234 من ماكينة ATM"
        val raw = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = "Bank-AlAhly",
            text = body,
            postedAtMillis = System.currentTimeMillis()
        )

        val result = parser.parse(raw, "device-pos-1")
        assertTrue(result is PaymentParseResult.Ignored)
    }
}
