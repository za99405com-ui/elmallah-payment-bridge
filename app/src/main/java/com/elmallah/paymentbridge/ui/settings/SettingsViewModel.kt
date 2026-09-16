package com.elmallah.paymentbridge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.network.DeviceProviderConfigRequest
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val keyManager: DeviceKeyManager,
    private val repository: PaymentRepository
) : ViewModel() {

    val deviceId = keyManager.deviceId

    val apiBaseUrlInput = MutableStateFlow(keyManager.apiBaseUrl)
    val bridgeUploadEnabled = MutableStateFlow(keyManager.bridgeUploadEnabled)
    val vfCashEnabled = MutableStateFlow(keyManager.vfCashEnabled)
    val bankAlAhlyEnabled = MutableStateFlow(keyManager.bankAlAhlyEnabled)
    val rawDiagnosticsEnabled = MutableStateFlow(keyManager.rawDiagnosticsEnabled)
    val serverBusy = MutableStateFlow(keyManager.serverBusy)
    val busySessionId = MutableStateFlow(keyManager.busySessionId)
    val lastHeartbeatTimestamp = MutableStateFlow(keyManager.lastHeartbeatTimestamp)

    val healthCheckStatus = MutableStateFlow<String?>(null)
    val provisioningStatus = MutableStateFlow<String?>(null)
    val providerConfigStatus = MutableStateFlow<String?>(null)
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
        bridgeUploadEnabled.value = enabled
    }

    fun setVfCashEnabled(enabled: Boolean) {
        updateProviderConfig(vfCash = enabled, bankAlAhly = null)
    }

    fun setBankAlAhlyEnabled(enabled: Boolean) {
        updateProviderConfig(vfCash = null, bankAlAhly = enabled)
    }

    private fun updateProviderConfig(vfCash: Boolean?, bankAlAhly: Boolean?) {
        viewModelScope.launch {
            providerConfigStatus.value = "جارٍ حفظ إعداد الجهاز في admin3..."
            try {
                val response = ApiClientProvider(keyManager).getApi().updateProviderConfig(
                    DeviceProviderConfigRequest(
                        vfCashEnabled = vfCash,
                        bankAlAhlyEnabled = bankAlAhly
                    )
                )
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    keyManager.vfCashEnabled = body.vfCashEnabled
                    keyManager.bankAlAhlyEnabled = body.bankAlAhlyEnabled
                    keyManager.serverBusy = body.busy
                    keyManager.busySessionId = body.busySessionId
                    vfCashEnabled.value = body.vfCashEnabled
                    bankAlAhlyEnabled.value = body.bankAlAhlyEnabled
                    serverBusy.value = body.busy
                    busySessionId.value = body.busySessionId
                    providerConfigStatus.value = "تم حفظ إعداد وسائل الدفع في admin3."
                } else {
                    providerConfigStatus.value = "تعذر حفظ إعداد وسائل الدفع: HTTP ${response.code()}"
                    refreshServerState()
                }
            } catch (e: Exception) {
                providerConfigStatus.value = "تعذر حفظ إعداد وسائل الدفع: ${e.localizedMessage ?: e.javaClass.simpleName}"
                refreshServerState()
            }
        }
    }

    fun provisionSecret(secret: String): Boolean {
        val success = keyManager.setDeviceSecret(secret)
        provisioningStatus.value = if (success) {
            keyManager.bridgeUploadEnabled = true
            bridgeUploadEnabled.value = true
            "تم حفظ مفتاح HMAC داخل Android Keystore وتفعيل الربط مع السيرفر."
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
        vfCashEnabled.value = keyManager.vfCashEnabled
        bankAlAhlyEnabled.value = keyManager.bankAlAhlyEnabled
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
}
