package com.example.paymentbridge.parser

import java.security.MessageDigest

object MessageNormalizer {

    private val ARABIC_INDIC_DIGITS = charArrayOf(
        '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'
    )
    private val EASTERN_ARABIC_DIGITS = charArrayOf(
        '۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'
    )

    /**
     * Replaces Arabic/Eastern Indic digits with ASCII digits '0'-'9'.
     */
    fun convertDigitsToAscii(input: String): String {
        val chars = input.toCharArray()
        for (i in chars.indices) {
            val c = chars[i]
            val arabicIndex = ARABIC_INDIC_DIGITS.indexOf(c)
            if (arabicIndex >= 0) {
                chars[i] = (arabicIndex + '0'.code).toChar()
                continue
            }
            val easternIndex = EASTERN_ARABIC_DIGITS.indexOf(c)
            if (easternIndex >= 0) {
                chars[i] = (easternIndex + '0'.code).toChar()
            }
        }
        return String(chars)
    }

    /**
     * Strips zero-width and directional unicode control characters.
     */
    fun removeInvisibleControlChars(input: String): String {
        return input.replace(Regex("[\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF]"), "")
    }

    /**
     * Normalizes punctuation and separators:
     * - Arabic decimal separator (٫) or comma between digits -> .
     * - Arabic / fullwidth colons -> :
     * - Non-breaking spaces -> standard spaces
     */
    fun normalizePunctuation(input: String): String {
        var res = input.replace('\u00A0', ' ')
            .replace('：', ':')
            .replace('،', ',')
        // Replace comma or Arabic decimal comma between digits with dot
        res = res.replace(Regex("(?<=\\d)[,٫](?=\\d)"), ".")
        return res
    }

    /**
     * Normalizes general text while keeping alphanumeric references intact.
     * Replaces multiple whitespace / line break sequences.
     */
    fun normalizeWhitespace(input: String): String {
        return input.lines()
            .map { it.trim().replace(Regex("[ \\t]+"), " ") }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    /**
     * Full pre-parser normalization pipeline.
     */
    fun normalizeForParsing(input: String): String {
        val withoutInvisible = removeInvisibleControlChars(input)
        val asciiDigits = convertDigitsToAscii(withoutInvisible)
        val cleanPunctuation = normalizePunctuation(asciiDigits)
        return normalizeWhitespace(cleanPunctuation)
    }

    /**
     * Normalizes Egyptian mobile phone numbers to standard 11-digit local format:
     * e.g. "+201012345678", "00201012345678", "201012345678", "01012345678" -> "01012345678".
     */
    fun normalizeEgyptianPhone(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val digitsOnly = convertDigitsToAscii(phone).replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.startsWith("00201") && digitsOnly.length == 14 -> digitsOnly.substring(4)
            digitsOnly.startsWith("201") && digitsOnly.length == 13 -> digitsOnly.substring(2)
            digitsOnly.startsWith("01") && digitsOnly.length == 11 -> digitsOnly
            digitsOnly.startsWith("1") && digitsOnly.length == 10 -> "0$digitsOnly"
            else -> digitsOnly
        }
    }

    /**
     * Parses money string into integer minor units (piastres / cents).
     * 180.00 -> 18000
     * 287.22 -> 28722
     * 250 -> 25000
     */
    fun parseAmountToMinor(amountStr: String): Long? {
        return try {
            val clean = amountStr.trim().replace(",", "")
            val parts = clean.split(".")
            val major = parts[0].toLong()
            val minor = when {
                parts.size == 1 -> 0L
                parts[1].length == 1 -> (parts[1] + "0").take(2).toLong()
                parts[1].length >= 2 -> parts[1].take(2).toLong()
                else -> 0L
            }
            (major * 100) + minor
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes SHA-256 hash of normalized text for privacy and duplicate checking.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
