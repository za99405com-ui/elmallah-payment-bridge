package com.elmallah.paymentbridge.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.elmallah.paymentbridge.BuildConfig
import com.elmallah.paymentbridge.domain.PaymentRuleStore
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.network.HeartbeatRequest
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BridgeHeartbeatLoop(
    private val context: Context,
    private val scope: CoroutineScope,
    private val keyManager: DeviceKeyManager,
    private val apiProvider: ApiClientProvider,
    private val ruleStore: PaymentRuleStore? = null
) {
    companion object {
        private const val TAG = "BridgeHeartbeat"
        private const val HEARTBEAT_INTERVAL_MS = 20_000L
    }

    private var job: Job? = null

    fun start(notificationListenerEnabled: () -> Boolean) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                sendOnce(notificationListenerEnabled())
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    suspend fun sendOnce(listenerEnabled: Boolean): Boolean {
        if (!keyManager.bridgeUploadEnabled) return false

        val request = HeartbeatRequest(
            internetConnected = isInternetConnected(),
            appRunning = true,
            notificationListenerEnabled = listenerEnabled,
            appVersion = BuildConfig.VERSION_NAME
        )

        return try {
            val api = apiProvider.getApi()
            val response = api.sendHeartbeat(request)
            val body = response.body()
            keyManager.lastServerStatusCode = response.code()

            if (response.isSuccessful && body != null) {
                keyManager.lastHeartbeatTimestamp = System.currentTimeMillis()
                keyManager.serverBusy = body.busy
                keyManager.busySessionId = body.busySessionId

                // admin3 is authoritative for provider config & dynamic rules
                keyManager.vfCashEnabled = body.vfCashEnabled
                keyManager.bankAlAhlyEnabled = body.bankAlAhlyEnabled
                keyManager.lastServerResponse = "HTTP ${response.code()} OK - Online=${body.online}"

                if (body.rules != null) {
                    // Compatibility path if the heartbeat ever embeds rules.
                    val authoritativeRules = body.rules.map { it.toDomain() }
                    ruleStore?.updateRules(authoritativeRules, body.rulesVersion)
                    keyManager.activeRulesCount = authoritativeRules.count { it.enabled }
                } else {
                    if (body.activeRulesCount != null) {
                        keyManager.activeRulesCount = body.activeRulesCount
                    }

                    // Current admin3 heartbeat sends a rulesVersion, not the full rules.
                    // Fetch rules only on first sync or when that version changes.
                    val store = ruleStore
                    val shouldRefreshRules = store != null && (
                        !store.hasSyncedWithServer ||
                            (!body.rulesVersion.isNullOrBlank() && body.rulesVersion != store.lastRulesVersion)
                        )

                    if (shouldRefreshRules && store != null) {
                        try {
                            val rulesResponse = api.fetchPaymentRules()
                            val rulesBody = rulesResponse.body()
                            if (rulesResponse.isSuccessful && rulesBody != null) {
                                val authoritativeRules = rulesBody.rules.map { it.toDomain() }
                                store.updateRules(
                                    authoritativeRules,
                                    rulesBody.rulesVersion ?: body.rulesVersion
                                )
                                keyManager.activeRulesCount = authoritativeRules.count { it.enabled }
                                Log.i(TAG, "Payment rules refreshed automatically. Count=${authoritativeRules.size}")
                            } else {
                                Log.w(TAG, "Rules refresh rejected: HTTP ${rulesResponse.code()}")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Automatic rules refresh failed: ${e.javaClass.simpleName}")
                        }
                    }
                }
                true
            } else {
                keyManager.lastServerResponse = "HTTP ${response.code()}: ${response.message()}"
                Log.w(TAG, "Heartbeat rejected: HTTP ${response.code()}")
                false
            }
        } catch (e: Exception) {
            keyManager.lastServerStatusCode = -1
            keyManager.lastServerResponse = "Error: ${e.message ?: e.javaClass.simpleName}"
            Log.w(TAG, "Heartbeat failed: ${e.javaClass.simpleName}")
            false
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun isInternetConnected(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
