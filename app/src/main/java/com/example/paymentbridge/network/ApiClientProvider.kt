package com.example.paymentbridge.network

import android.content.Context
import com.example.paymentbridge.security.DeviceKeyManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClientProvider(private val context: Context) {

    val keyManager = DeviceKeyManager(context)

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val secureInterceptor = SecureRequestInterceptor(keyManager)

    // Dynamic base URL interceptor that redirects calls according to the user's configured base URL
    private val dynamicBaseUrlInterceptor = okhttp3.Interceptor { chain ->
        var request = chain.request()
        val configuredBase = keyManager.apiBaseUrl.trim()
        val targetHttpUrl = configuredBase.toHttpUrlOrNull()

        if (targetHttpUrl != null) {
            val newUrl = request.url.newBuilder()
                .scheme(targetHttpUrl.scheme)
                .host(targetHttpUrl.host)
                .port(targetHttpUrl.port)
                .build()
            request = request.newBuilder().url(newUrl).build()
        }
        chain.proceed(request)
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(dynamicBaseUrlInterceptor)
        .addInterceptor(secureInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    fun getApi(): PaymentBridgeApi {
        val baseUrl = if (keyManager.apiBaseUrl.endsWith("/")) {
            keyManager.apiBaseUrl
        } else {
            "${keyManager.apiBaseUrl}/"
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PaymentBridgeApi::class.java)
    }
}
