package com.elmallah.paymentbridge

import com.elmallah.paymentbridge.parser.MessageNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageNormalizerTest {

    @Test
    fun testNormalizeArabicDigits() {
        val input = "مبلغ ١٨٠٫٥٠ جنيه بتاريخ ١٤-٠٩-٢٠٢٦"
        val normalized = MessageNormalizer.convertDigitsToAscii(input)
        assertEquals("مبلغ 180٫50 جنيه بتاريخ 14-09-2026", normalized)
    }

    @Test
    fun testEgyptianPhoneNumberNormalization() {
        // 11-digit standard
        assertEquals("01015192040", MessageNormalizer.normalizeEgyptianPhone("01015192040"))
        // Without leading zero
        assertEquals("01015192040", MessageNormalizer.normalizeEgyptianPhone("1015192040"))
        // International with +20
        assertEquals("01015192040", MessageNormalizer.normalizeEgyptianPhone("+201015192040"))
        // International with 0020
        assertEquals("01015192040", MessageNormalizer.normalizeEgyptianPhone("00201015192040"))
        // International with 20
        assertEquals("01015192040", MessageNormalizer.normalizeEgyptianPhone("201015192040"))
        // With spaces and hyphens
        assertEquals("01015192040", MessageNormalizer.normalizeEgyptianPhone("+20 10-1519 2040"))
        // With Eastern Arabic digits
        assertEquals("01015192040", MessageNormalizer.normalizeEgyptianPhone("٠١٠١٥١٩٢٠٤٠"))

        // Invalid numbers
        assertNull(MessageNormalizer.normalizeEgyptianPhone("01312345678")) // Invalid prefix
        assertNull(MessageNormalizer.normalizeEgyptianPhone("12345"))       // Too short
        assertNull(MessageNormalizer.normalizeEgyptianPhone(null))
    }

    @Test
    fun testMoneyMinorUnitNormalization() {
        // Plain integer
        assertEquals(18000L, MessageNormalizer.parseAmountToMinor("180"))
        // Decimal with point
        assertEquals(18000L, MessageNormalizer.parseAmountToMinor("180.00"))
        assertEquals(18050L, MessageNormalizer.parseAmountToMinor("180.50"))
        // Decimal with single digit (e.g. .5 -> 50 piastres)
        assertEquals(18050L, MessageNormalizer.parseAmountToMinor("180.5"))
        // Arabic comma decimal separator (٫)
        assertEquals(28722L, MessageNormalizer.parseAmountToMinor("287٫22"))
        // Thousands separator
        assertEquals(150000L, MessageNormalizer.parseAmountToMinor("1,500.00"))
        // Arabic numerals
        assertEquals(18000L, MessageNormalizer.parseAmountToMinor("١٨٠٫٠٠"))

        // Invalid money
        assertNull(MessageNormalizer.parseAmountToMinor("abc"))
        assertNull(MessageNormalizer.parseAmountToMinor(null))
    }
}
