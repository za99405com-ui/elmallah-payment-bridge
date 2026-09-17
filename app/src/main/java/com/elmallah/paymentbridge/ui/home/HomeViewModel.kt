package com.elmallah.paymentbridge.ui.home

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.capture.PaymentNotificationListener
import com.elmallah.paymentbridge.data.PaymentEventEntity
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.domain.PaymentRuleStore
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.elmallah.paymentbridge.sync.BridgeForegroundService
import com.elmallah.paymentbridge.sync.BridgeSyncCoordinator
import com.elmallah.paymentbridge.sync.NetworkConnectivityMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SetupChecklistState(
    val notificationAccessGranted: Boolean = false,
    val backgroundServiceRunning: Boolean = false,
    val serverUrlConfigured: Boolean = false,
    val deviceProvisioned: Boolean = false,
    val serverConnectionVerified: Boolean = false
) {
    val allCompleted: Boolean
        get() = notificationAccessGranted &&
            backgroundServiceRunning &&
            serverUrlConfigured &&
            deviceProvisioned &&
            serverConnectionVerified

    val completedCount: Int
        get() = listOf(
            notificationAccessGranted,
            backgroundServiceRunning,
            serverUrlConfigured,
            deviceProvisioned,
            serverConnectionVerified
        ).count { it }
}

data class DeviceStatusState(
    val isOnline: Boolean = false,
    val isBusy: Boolean = false,
    val busySessionId: String? = null,
    val isListenerConnected: Boolean = false,
    val isBackgroundServiceRunning: Boolean = false,
    val isServerConnected: Boolean = false,
    val deviceId: String = "",
    val lastHeartbeatTimestamp: Long = 0L,
    val lastSyncTimestamp: Long = 0L,
    val serverBaseUrl: String = ""
)

data class MonitoringState(
    val isMonitoringActive: Boolean = false,
    val activeRulesCount: Int = 0,
    val lastDetectedNotification: String? = null,
    val lastParsedAmountMinor: Long? = null,
    val lastParsedTime: Long? = null,
    val pendingEventsCount: Int = 0,
    val failedUploadsCount: Int = 0
)

class HomeViewModel(
    private val appContext: Context,
    private val repository: PaymentRepository,
    val keyManager: DeviceKeyManager,
    private val apiProvider: ApiClientProvider,
    private val ruleStore: PaymentRuleStore
) : ViewModel() {

    private val networkMonitor = NetworkConnectivityMonitor(appContext)

    val isInternetConnected: StateFlow<Boolean> = networkMonitor.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.isConnected())

    val isBackgroundServiceRunning: StateFlow<Boolean> = BridgeForegroundService.isRunning

    val pendingUploadsCount: StateFlow<Int> = repository.getPendingUploadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedUploadsCount: StateFlow<Int> = repository.getFailedUploadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val lastEvent: StateFlow<PaymentEventEntity?> = repository.getLastEvent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isServerVerified = MutableStateFlow(false)
    val isServerVerified: StateFlow<Boolean> = _isServerVerified.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _setupDismissed = MutableStateFlow(false)
    val setupDismissed: StateFlow<Boolean> = _setupDismissed.asStateFlow()

    val isNotificationAccessGranted: Boolean
        get() = checkNotificationAccess(appContext)

    val checklistState: SetupChecklistState
        get() = SetupChecklistState(
            notificationAccessGranted = isNotificationAccessGranted,
            backgroundServiceRunning = isBackgroundServiceRunning.value,
            serverUrlConfigured = keyManager.apiBaseUrl.isNotBlank() && !keyManager.apiBaseUrl.contains("example.com"),
            deviceProvisioned = keyManager.isProvisioned,
            serverConnectionVerified = _isServerVerified.value || (keyManager.lastHeartbeatTimestamp > 0 && keyManager.lastServerStatusCode in 200..299)
        )

    val deviceStatus: DeviceStatusState
        get() = DeviceStatusState(
            isOnline = isInternetConnected.value,
            isBusy = keyManager.serverBusy,
            busySessionId = keyManager.busySessionId,
            isListenerConnected = PaymentNotificationListener.isConnected,
            isBackgroundServiceRunning = isBackgroundServiceRunning.value,
            isServerConnected = keyManager.lastServerStatusCode in 200..299,
            deviceId = keyManager.deviceId,
            lastHeartbeatTimestamp = keyManager.lastHeartbeatTimestamp,
            lastSyncTimestamp = keyManager.lastSyncTimestamp,
            serverBaseUrl = keyManager.apiBaseUrl
        )

    val monitoringState: MonitoringState
        get() = MonitoringState(
            isMonitoringActive = isNotificationAccessGranted && (isBackgroundServiceRunning.value || PaymentNotificationListener.isConnected),
            activeRulesCount = ruleStore.getActiveRules().size,
            lastDetectedNotification = keyManager.lastDetectedNotification,
            lastParsedAmountMinor = keyManager.lastParsedAmountMinor.takeIf { it > 0L },
            lastParsedTime = keyManager.lastParsedTime.takeIf { it > 0L },
            pendingEventsCount = pendingUploadsCount.value,
            failedUploadsCount = failedUploadsCount.value
        )

    init {
        checkServerHealthSilent()
    }

    fun triggerImmediateSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "جاري مزامنة العمليات المعلقة مع admin3..."
            try {
                BridgeSyncCoordinator.enqueueImmediate(appContext)
                _syncMessage.value = "تم إرسال طلب المزامنة إلى مشغّل النظام."
            } catch (e: Exception) {
                _syncMessage.value = "فشل بدء المزامنة: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun verifyServerConnection() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "جاري التحقق من الاتصال بسيرفر admin3..."
            try {
                val response = apiProvider.getApi().checkHealth()
                if (response.isSuccessful && response.body() != null) {
                    _isServerVerified.value = true
                    keyManager.lastServerStatusCode = response.code()
                    keyManager.lastServerResponse = "HTTP 200: Healthy (v=${response.body()?.version ?: "1.0"})"
                    _syncMessage.value = "تم التحقق بنجاح: السيرفر متصل ويعمل."
                } else {
                    _isServerVerified.value = false
                    keyManager.lastServerStatusCode = response.code()
                    _syncMessage.value = "فشل الاتصال: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                _isServerVerified.value = false
                keyManager.lastServerStatusCode = -1
                _syncMessage.value = "تعذر الوصول للسيرفر: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun checkServerHealthSilent() {
        viewModelScope.launch {
            try {
                val response = apiProvider.getApi().checkHealth()
                if (response.isSuccessful) {
                    _isServerVerified.value = true
                    keyManager.lastServerStatusCode = response.code()
                }
            } catch (_: Exception) {}
        }
    }

    fun toggleBackgroundService(enable: Boolean) {
        if (enable) {
            BridgeForegroundService.start(appContext)
        } else {
            BridgeForegroundService.stop(appContext)
        }
    }

    fun dismissSetupCard() {
        _setupDismissed.value = true
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    companion object {
        fun checkNotificationAccess(context: Context): Boolean {
            val componentName = ComponentName(context, PaymentNotificationListener::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat != null && flat.contains(componentName.flattenToString())
        }
    }
}
