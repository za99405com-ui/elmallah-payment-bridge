package com.example.paymentbridge.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PaymentBridgeApi {

    @POST("api/payment-bridge/events")
    suspend fun uploadPaymentEvent(
        @Body request: PaymentEventUploadRequest
    ): Response<PaymentEventUploadResponse>

    @GET("api/payment-bridge/health")
    suspend fun checkHealth(): Response<HealthCheckResponse>
}
