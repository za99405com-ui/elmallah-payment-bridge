package com.elmallah.paymentbridge.parser

import java.security.MessageDigest

object MessageNormalizer {

    private val ARABIC_INDIC_DIGITS = charArrayOf(
        '٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'
    )
    private val EASTERN_ARABIC_DIGITS = charArrayOf(
        '۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'
    )

    /**
     * Converts Arabic and Eastern Indic numerals to standard ASCII '0'-'9'.
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
     * Strips invisible Unicode directional control characters and zero-width spaces.
     */
    fun removeInvisibleControlChars(input: String): String {
        return input.replace(Regex("[\u200B-\u200F\u202A-\u202E\u2066-\u2069\uFEFF]"), "")
    }

    /**
     * Normalizes punctuation and separators:
     * - Removes Arabic thousands separator (٬ U+066C)
     * - Converts Arabic decimal separator (٫ U+066B) to standard dot '.'
     * - Normalizes fullwidth/Arabic colons to ':'
     */
    fun normalizePunctuation(input: String): String {
        return input.replace('\u00A0', ' ')
            .replace('：', ':')
            .replace('،', ',')
            .replace('٬', ',')
            .replace('٫', '.')
    }

    /**
     * Normalizes whitespace and line breaks cleanly.
     */
    fun normalizeWhitespace(input: String): String {
        return input.lines()
            .map { it.trim().replace(Regex("[ \\t]+"), " ") }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    /**
     * Full normalization pipeline for message parsing.
     */
    fun normalizeForParsing(input: String): String {
        val withoutInvisible = removeInvisibleControlChars(input)
        val asciiDigits = convertDigitsToAscii(withoutInvisible)
        val cleanPunctuation = normalizePunctuation(asciiDigits)
        return normalizeWhitespace(cleanPunctuation)
    }

    /**
     * Normalizes Egyptian mobile phone numbers to standard 11-digit local format (e.g. 01012345678).
     * Handles:
     * - 01012345678 -> 01012345678
     * - 1012345678 -> 01012345678
     * - +201012345678 -> 01012345678
     * - 201012345678 -> 01012345678
     * - 00201012345678 -> 01012345678
     */
    fun normalizeEgyptianPhone(phone: String?): String? {
        if (phone.isNullOrBlank()) return null
        val digitsOnly = convertDigitsToAscii(phone).replace(Regex("[^0-9]"), "")

        val normalized = when {
            // e.g. 00201012345678 (14 digits: 0020 + 10 digits) -> 0 + 10 digits = 11 digits
            digitsOnly.startsWith("00201") && digitsOnly.length == 14 -> "0" + digitsOnly.substring(4)
            // e.g. 201012345678 (12 digits: 20 + 10 digits) -> 0 + 10 digits = 11 digits
            digitsOnly.startsWith("201") && digitsOnly.length == 12 -> "0" + digitsOnly.substring(2)
            // e.g. 01012345678 (11 digits)
            digitsOnly.startsWith("01") && digitsOnly.length == 11 -> digitsOnly
            // e.g. 1012345678 (10 digits without leading 0) -> prepend 0
            (digitsOnly.startsWith("10") || digitsOnly.startsWith("11") ||
             digitsOnly.startsWith("12") || digitsOnly.startsWith("15")) && digitsOnly.length == 10 -> "0$digitsOnly"
            else -> digitsOnly
        }

        return if (normalized.length == 11 && (normalized.startsWith("010") || normalized.startsWith("011") || normalized.startsWith("012") || normalized.startsWith("015"))) {
            normalized
        } else {
            null
        }
    }

    /**
     * Parses money string into integer minor units (piastres / cents).
     * 1 EGP = 100 piastres.
     * Safely handles:
     * - 180 -> 18000
     * - 180.00 -> 18000
     * - 287.22 -> 28722
     * - 1,000.00 -> 100000 (never 1.00!)
     * - ١٨٠٫٠٠ -> 18000
     * - ١٬٠٠٠٫٠٠ -> 100000
     */
    fun parseAmountToMinor(amountStr: String?): Long? {
        if (amountStr.isNullOrBlank()) return null
        return try {
            val ascii = convertDigitsToAscii(amountStr.trim())
            // Remove Arabic thousands separator (٬)
            var clean = ascii.replace("٬", "")
            // Replace Arabic decimal separator (٫) with dot
            clean = clean.replace("٫", ".")

            // Handle standard comma:
            // If comma is followed by 3 digits (e.g. 1,000 or 1,000.00), it's a thousands separator: remove it
            if (clean.matches(Regex(".*\\d+,\\d{3}(\\.\\d+)?.*"))) {
                clean = clean.replace(",", "")
            } else if (clean.contains(",") && !clean.contains(".")) {
                // If there is only a comma and no dot (e.g. 180,50), treat comma as decimal separator
                clean = clean.replace(",", ".")
            } else {
                clean = clean.replace(",", "")
            }

            clean = clean.replace(Regex("[^0-9.]"), "")
            if (clean.isBlank()) return null

            val parts = clean.split(".")
            val major = parts[0].toLong()
            val minor = when {
                parts.size == 1 -> 0L
                parts[1].isEmpty() -> 0L
                parts[1].length == 1 -> (parts[1] + "0").toLong()
                parts[1].length >= 2 -> parts[1].take(2).toLong()
                else -> 0L
            }
            (major * 100) + minor
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes SHA-256 hash of normalized text for deduplication and privacy audits.
     */
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun sha256Hex(input: String): String = sha256(input)
}
