package com.elmallah.paymentbridge

import android.app.Application
import com.elmallah.paymentbridge.data.PaymentDatabase
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlMallahBridgeApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: PaymentDatabase
        private set

    lateinit var repository: PaymentRepository
        private set

    lateinit var apiProvider: ApiClientProvider
        private set

    override fun onCreate() {
        super.onCreate()

        val keyManager = DeviceKeyManager(this)
        database = PaymentDatabase.getInstance(this)
        repository = PaymentRepository(database.paymentEventDao(), keyManager)
        apiProvider = ApiClientProvider(keyManager)

        // Privacy maintenance: purge raw diagnostic snippets older than 7 days
        applicationScope.launch {
            try {
                repository.purgeOldSnippets(olderThanDays = 7)
            } catch (_: Exception) {
                // Ignore transient database maintenance issues
            }
        }
    }
}
