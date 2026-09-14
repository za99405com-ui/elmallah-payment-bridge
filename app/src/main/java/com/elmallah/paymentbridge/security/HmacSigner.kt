package com.elmallah.paymentbridge.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HmacSigner {

    private const val ALGORITHM = "HmacSHA256"

    /**
     * Computes HMAC-SHA256 signature for:
     * "$timestamp.$nonce.$requestBody"
     */
    fun sign(
        timestamp: Long,
        nonce: String,
        requestBody: String,
        secretHexOrPlain: String
    ): String {
        val payload = "$timestamp.$nonce.$requestBody"
        val keyBytes = secretHexOrPlain.toByteArray(Charsets.UTF_8)
        val keySpec = SecretKeySpec(keyBytes, ALGORITHM)

        val mac = Mac.getInstance(ALGORITHM)
        mac.init(keySpec)
        val hash = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
