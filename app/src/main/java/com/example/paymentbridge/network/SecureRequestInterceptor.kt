package com.example.paymentbridge.network

import com.example.paymentbridge.security.DeviceKeyManager
import com.example.paymentbridge.security.HmacSigner
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.util.UUID

class SecureRequestInterceptor(
    private val keyManager: DeviceKeyManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val timestamp = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString()
        val deviceId = keyManager.deviceId
        val deviceSecret = keyManager.deviceSecret

        val bodyString = if (original.body != null) {
            val buffer = Buffer()
            original.body?.writeTo(buffer)
            buffer.readUtf8()
        } else {
            ""
        }

        val bodyHash = HmacSigner.sha256Hex(bodyString)
        val signature = HmacSigner.sign(
            timestamp = timestamp,
            nonce = nonce,
            requestBody = bodyString,
            secretHexOrString = deviceSecret
        )

        val requestBuilder = original.newBuilder()
            .header("X-Device-Id", deviceId)
            .header("X-Timestamp", timestamp.toString())
            .header("X-Nonce", nonce)
            .header("X-Body-Hash", bodyHash)
            .header("X-Signature", signature)
            .header("Accept", "application/json")
            .header("User-Agent", "AlMallah-Payment-Bridge-Android/1.0")

        return chain.proceed(requestBuilder.build())
    }
}
