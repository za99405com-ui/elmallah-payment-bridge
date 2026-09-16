package com.elmallah.paymentbridge

import android.app.Application
import android.util.Log
import com.elmallah.paymentbridge.data.PaymentDatabase
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.elmallah.paymentbridge.sync.BridgeSyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlMallahBridgeApp : Application() {

    companion object {
        private const val TAG = "AlMallahBridgeApp"
    }

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

        // Phase 2: retry unsent bridge events whenever network becomes available.
        // WorkManager is not initialized in some JVM/Robolectric migration tests,
        // so scheduling must never prevent database/application initialization.
        try {
            BridgeSyncCoordinator.ensurePeriodicRetry(this)
        } catch (e: IllegalStateException) {
            Log.d(TAG, "WorkManager unavailable in current runtime; periodic sync not scheduled.")
        }

        // Privacy maintenance: purge raw diagnostic snippets older than 7 days.
        applicationScope.launch {
            try {
                repository.purgeOldSnippets(olderThanDays = 7)
            } catch (_: Exception) {
                // Ignore transient database maintenance issues.
            }
        }
    }
}
