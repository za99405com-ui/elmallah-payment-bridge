package com.elmallah.paymentbridge.network

import com.elmallah.paymentbridge.parser.MessageNormalizer
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.elmallah.paymentbridge.security.HmacSigner
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.util.UUID

/**
 * Attaches HMAC-SHA256 authentication headers to every outgoing request:
 * - X-Device-Id: merchant device identifier
 * - X-Timestamp: current unix epoch timestamp (ms)
 * - X-Nonce: unique random nonce for replay prevention
 * - X-Body-Hash: SHA-256 hash of payload
 * - X-Signature: HMAC-SHA256(timestamp.nonce.body)
 */
class SecureRequestInterceptor(
    private val keyManager: DeviceKeyManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val timestamp = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString()
        val deviceId = keyManager.deviceId
        val secret = keyManager.getDeviceSecret()

        val bodyString = if (originalRequest.body != null) {
            val buffer = Buffer()
            originalRequest.body?.writeTo(buffer)
            buffer.readUtf8()
        } else {
            ""
        }

        val bodyHash = MessageNormalizer.sha256(bodyString)
        val signature = HmacSigner.sign(timestamp, nonce, bodyString, secret)

        val signedRequest = originalRequest.newBuilder()
            .header("X-Device-Id", deviceId)
            .header("X-Timestamp", timestamp.toString())
            .header("X-Nonce", nonce)
            .header("X-Body-Hash", bodyHash)
            .header("X-Signature", signature)
            .header("Accept", "application/json")
            .build()

        return chain.proceed(signedRequest)
    }
}
