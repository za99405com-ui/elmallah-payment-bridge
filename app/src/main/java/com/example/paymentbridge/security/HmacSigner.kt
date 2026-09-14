package com.example.paymentbridge.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacSigner {

    private const val HMAC_ALGORITHM = "HmacSHA256"

    /**
     * Signs a request payload using HMAC-SHA256.
     * signaturePayload = "$timestamp.$nonce.$requestBody"
     */
    fun sign(
        timestamp: Long,
        nonce: String,
        requestBody: String,
        secretHexOrString: String
    ): String {
        val payload = "$timestamp.$nonce.$requestBody"
        val keyBytes = try {
            if (secretHexOrString.length % 2 == 0 && secretHexOrString.matches(Regex("^[0-9a-fA-F]+$"))) {
                hexToBytes(secretHexOrString)
            } else {
                secretHexOrString.toByteArray(Charsets.UTF_8)
            }
        } catch (_: Exception) {
            secretHexOrString.toByteArray(Charsets.UTF_8)
        }

        val secretKey = SecretKeySpec(keyBytes, HMAC_ALGORITHM)
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(secretKey)
        val hmacBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return bytesToHex(hmacBytes)
    }

    fun sha256Hex(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
        return bytesToHex(hash)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
