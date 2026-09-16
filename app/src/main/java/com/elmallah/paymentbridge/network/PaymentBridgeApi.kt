package com.elmallah.paymentbridge.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface PaymentBridgeApi {

    @POST("api/payment-bridge/events")
    suspend fun uploadPaymentEvent(
        @Body request: PaymentEventUploadRequest
    ): Response<PaymentEventUploadResponse>

    @POST("api/payment-bridge/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: HeartbeatRequest
    ): Response<HeartbeatResponse>

    @POST("api/payment-bridge/config")
    suspend fun updateProviderConfig(
        @Body request: DeviceProviderConfigRequest
    ): Response<DeviceProviderConfigResponse>

    @GET("api/payment-bridge/health")
    suspend fun checkHealth(): Response<HealthCheckResponse>
}
