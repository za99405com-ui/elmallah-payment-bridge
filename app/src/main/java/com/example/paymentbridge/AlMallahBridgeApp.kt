package com.example.paymentbridge

import android.app.Application
import com.example.paymentbridge.data.PaymentDatabase
import com.example.paymentbridge.data.PaymentRepository
import com.example.paymentbridge.network.ApiClientProvider

class AlMallahBridgeApp : Application() {

    lateinit var database: PaymentDatabase
        private set

    lateinit var repository: PaymentRepository
        private set

    lateinit var apiProvider: ApiClientProvider
        private set

    override fun onCreate() {
        super.onCreate()
        database = PaymentDatabase.getInstance(this)
        apiProvider = ApiClientProvider(this)
        repository = PaymentRepository(this, database.paymentEventDao(), apiProvider)
    }
}
