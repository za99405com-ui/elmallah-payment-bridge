package com.elmallah.paymentbridge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.elmallah.paymentbridge.sync.BridgeSyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val keyManager: DeviceKeyManager,
    private val repository: PaymentRepository
) : ViewModel() {

    val deviceId = keyManager.deviceId

    val apiBaseUrlInput = MutableStateFlow(keyManager.apiBaseUrl)
    val bridgeUploadEnabled: StateFlow<Boolean> = MutableStateFlow(keyManager.bridgeUploadEnabled)
    val vfCashEnabled = MutableStateFlow(keyManager.vfCashEnabled)
    val bankAlAhlyEnabled = MutableStateFlow(keyManager.bankAlAhlyEnabled)
    val rawDiagnosticsEnabled = MutableStateFlow(keyManager.rawDiagnosticsEnabled)
    val serverBusy = MutableStateFlow(keyManager.serverBusy)
    val busySessionId = MutableStateFlow(keyManager.busySessionId)
    val lastHeartbeatTimestamp = MutableStateFlow(keyManager.lastHeartbeatTimestamp)

    val healthCheckStatus = MutableStateFlow<String?>(null)
    val provisioningStatus = MutableStateFlow<String?>(null)
    val purgeResultStatus = MutableStateFlow<String?>(null)

    fun saveBaseUrl(newUrl: String): Boolean {
        return try {
            keyManager.apiBaseUrl = newUrl.trim()
            apiBaseUrlInput.value = keyManager.apiBaseUrl
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setBridgeUploadEnabled(enabled: Boolean) {
        keyManager.bridgeUploadEnabled = enabled
        (bridgeUploadEnabled as MutableStateFlow).value = enabled
        if (enabled) {
            BridgeSyncCoordinator.enqueueImmediate(AppContextHolder.context ?: return)
        }
    }

    fun setVfCashEnabled(enabled: Boolean) {
        keyManager.vfCashEnabled = enabled
        vfCashEnabled.value = enabled
    }

    fun setBankAlAhlyEnabled(enabled: Boolean) {
        keyManager.bankAlAhlyEnabled = enabled
        bankAlAhlyEnabled.value = enabled
    }

    fun provisionSecret(secret: String): Boolean {
        val success = keyManager.setDeviceSecret(secret)
        provisioningStatus.value = if (success) {
            "تم حفظ مفتاح HMAC داخل Android Keystore. يمكن الآن التحقق من الاتصال بالسيرفر."
        } else {
            "مفتاح التهيئة غير صالح. يجب أن يكون 32 حرفاً على الأقل."
        }
        return success
    }

    fun toggleRawDiagnostics(enabled: Boolean) {
        keyManager.rawDiagnosticsEnabled = enabled
        rawDiagnosticsEnabled.value = enabled
    }

    fun clearAllRawSnippets() {
        viewModelScope.launch {
            val count = repository.clearAllRawSnippets()
            purgeResultStatus.value = "تم بنجاح حذف $count نص رسالة خام من قاعدة البيانات المحلية."
        }
    }

    fun refreshServerState() {
        serverBusy.value = keyManager.serverBusy
        busySessionId.value = keyManager.busySessionId
        lastHeartbeatTimestamp.value = keyManager.lastHeartbeatTimestamp
    }

    fun testServerHealth() {
        viewModelScope.launch {
            healthCheckStatus.value = "جارٍ فحص الاتصال بالسيرفر..."
            try {
                val api = ApiClientProvider(keyManager).getApi()
                val response = api.checkHealth()
                if (response.isSuccessful) {
                    healthCheckStatus.value = "نجح الاتصال بالسيرفر! الرد: ${response.body()?.status}"
                } else {
                    healthCheckStatus.value = "فشل الاتصال: كود ${response.code()}"
                }
            } catch (e: Exception) {
                healthCheckStatus.value = "خطأ في الاتصال: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    /**
     * Tiny holder populated by SettingsScreen so WorkManager can be triggered after
     * enabling uploads without storing an Activity reference in the ViewModel.
     */
    object AppContextHolder {
        @Volatile
        var context: android.content.Context? = null
    }
}
