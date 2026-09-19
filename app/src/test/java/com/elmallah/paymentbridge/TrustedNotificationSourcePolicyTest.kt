package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.capture.SourceValidationResult
import com.elmallah.paymentbridge.capture.TrustedNotificationSourcePolicy
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedNotificationSourcePolicyTest {

    @Test
    fun samsungDirectionalMarksDoNotBreakBankSenderMatching() {
        val rule = PaymentSourceRule(
            id = "instapay-rule",
            code = "instapay",
            name = "إنستا باي",
            enabled = true,
            paymentChannel = "INSTAPAY",
            packageNames = listOf("com.samsung.android.messaging"),
            senderFilters = listOf("Bank-AlAhly"),
            bodyContains = listOf("تم إضافة تحويل لحظي"),
            amountExtractionRegex = """(?:بمبلغ|مبلغ)\s*([0-9,]+(?:\.[0-9]+)?)\s*(?:جم|جنيه|ج\.م|EGP)?"""
        )

        val result = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.samsung.android.messaging",
            senderTitle = "\u200f\u2068BanK-AlAhly\u2069",
            rules = listOf(rule),
            bodyText = "تم إضافة تحويل لحظي لحسابكم رقم 0130 بمبلغ 100.00 جم"
        )

        assertTrue(result is SourceValidationResult.Accepted)
    }

    @Test
    fun directionalMarksDoNotBreakIncomingBodyMatching() {
        val rule = PaymentSourceRule(
            id = "instapay-rule",
            code = "instapay",
            name = "إنستا باي",
            enabled = true,
            paymentChannel = "INSTAPAY",
            packageNames = listOf("com.samsung.android.messaging"),
            senderFilters = listOf("Bank-AlAhly"),
            bodyContains = listOf("تم إضافة تحويل لحظي")
        )

        val mark = 0x200F.toChar()
        val isolateStart = 0x2068.toChar()
        val isolateEnd = 0x2069.toChar()
        val body = "تم" + mark + " إضافة تحويل" + isolateStart + " لحظي" + isolateEnd +
            " لحسابكم رقم 0130 بمبلغ 50.00 جم"

        val result = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage = "com.samsung.android.messaging",
            senderTitle = "Bank-AlAhly",
            rules = listOf(rule),
            bodyText = body
        )

        assertTrue(result is SourceValidationResult.Accepted)
    }
}
