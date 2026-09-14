package com.elmallah.paymentbridge.network

import com.elmallah.paymentbridge.BuildConfig
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClientProvider(
    val keyManager: DeviceKeyManager
) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Strict security: Never use Level.BODY in production or debug.
            // Release: NONE. Debug: BASIC (request/response line only, no headers or body containing sensitive data).
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(SecureRequestInterceptor(keyManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    fun getApi(): PaymentBridgeApi {
        var baseUrl = keyManager.apiBaseUrl.trim()
        if (!baseUrl.endsWith("/")) {
            baseUrl = "$baseUrl/"
        }

        // HTTPS validation
        if (!baseUrl.startsWith("https://", ignoreCase = true)) {
            if (!BuildConfig.DEBUG) {
                throw SecurityException("Insecure HTTP endpoint rejected in release build: $baseUrl")
            }
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PaymentBridgeApi::class.java)
    }
}
