package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.capture.NotificationMessageCandidate
import com.elmallah.paymentbridge.capture.NotificationTextResolver
import com.elmallah.paymentbridge.capture.PaymentMessageDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTextResolverTest {

    @Test
    fun prefersNewestMessagingStyleMessageOverStaleBigText() {
        val outgoing = "تم تنفيذ تحويل لحظي من حسابكم رقم 0130 بمبلغ 100.00 جم إلى عميل رقم مرجعي 955126971885"
        val incoming = "تم إضافة تحويل لحظي لحسابكم رقم 0130 بمبلغ 100.00 جم من زياد معتز بالله السيد قاسم الملاح رقم مرجعي 955126971885 يوم 19-09 الساعة 09:28"

        val resolved = NotificationTextResolver.resolveLatest(
            messagingCandidates = listOf(
                NotificationMessageCandidate(outgoing, timestamp = 1000L, order = 0),
                NotificationMessageCandidate(incoming, timestamp = 2000L, order = 1)
            ),
            directText = incoming,
            textLines = listOf(outgoing, incoming),
            bigText = outgoing
        )

        assertEquals(incoming, resolved)
    }

    @Test
    fun directTextBeatsStaleBigTextWhenMessagingStyleIsMissing() {
        val outgoing = "تم تنفيذ تحويل لحظي من حسابكم رقم 0130 بمبلغ 100.00 جم إلى عميل"
        val incoming = "تم إضافة تحويل لحظي لحسابكم رقم 0130 بمبلغ 100.00 جم من عميل رقم مرجعي 955126971885"

        val resolved = NotificationTextResolver.resolveLatest(
            messagingCandidates = emptyList(),
            directText = incoming,
            textLines = listOf(outgoing, incoming),
            bigText = outgoing
        )

        assertEquals(incoming, resolved)
    }

    @Test
    fun outgoingBankTransferIsRejectedAsPaymentSample() {
        val outgoing = "تم تنفيذ تحويل لحظي من حسابكم رقم 0130 بمبلغ 100.00 جم إلى زياد رقم مرجعي 955126971885"
        val incoming = "تم إضافة تحويل لحظي لحسابكم رقم 0130 بمبلغ 100.00 جم من زياد رقم مرجعي 955126971885"

        assertTrue(PaymentMessageDirection.isClearlyOutgoing(outgoing))
        assertFalse(PaymentMessageDirection.looksLikeIncomingForSource("instapay", outgoing))
        assertFalse(PaymentMessageDirection.isClearlyOutgoing(incoming))
        assertTrue(PaymentMessageDirection.looksLikeIncomingForSource("instapay", incoming))
    }
}
