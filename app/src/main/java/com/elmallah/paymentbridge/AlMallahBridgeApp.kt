package com.elmallah.paymentbridge

import android.app.Application
import android.util.Log
import com.elmallah.paymentbridge.data.PaymentDatabase
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.domain.PaymentRuleStore
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.elmallah.paymentbridge.sync.BridgeForegroundService
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

    lateinit var ruleStore: PaymentRuleStore
        private set

    override fun onCreate() {
        super.onCreate()

        val keyManager = DeviceKeyManager(this)
        database = PaymentDatabase.getInstance(this)
        repository = PaymentRepository(database.paymentEventDao(), keyManager)
        apiProvider = ApiClientProvider(keyManager)
        ruleStore = PaymentRuleStore(this)

        // Phase 2: retry unsent bridge events whenever network becomes available.
        try {
            BridgeSyncCoordinator.ensurePeriodicRetry(this)
        } catch (e: Exception) {
            Log.d(TAG, "WorkManager periodic sync setup bypassed: ${e.message}")
        }

        // Background service: start persistent foreground monitoring if upload bridge is active
        if (keyManager.bridgeUploadEnabled) {
            try {
                BridgeForegroundService.start(this)
            } catch (e: Exception) {
                Log.d(TAG, "Foreground service start deferred: ${e.message}")
            }
        }

        // Privacy maintenance: purge raw diagnostic snippets older than 7 days.
        applicationScope.launch {
            try {
                repository.purgeOldSnippets(olderThanDays = 7)
            } catch (_: Exception) {}
        }
    }
}
