package com.elmallah.paymentbridge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val keyManager: DeviceKeyManager,
    private val repository: PaymentRepository
) : ViewModel() {

    val deviceId = keyManager.deviceId

    val apiBaseUrlInput = MutableStateFlow(keyManager.apiBaseUrl)
    val bridgeUploadEnabled = MutableStateFlow(keyManager.bridgeUploadEnabled)
    val rawDiagnosticsEnabled = MutableStateFlow(keyManager.rawDiagnosticsEnabled)

    val healthCheckStatus = MutableStateFlow<String?>(null)
    val purgeResultStatus = MutableStateFlow<String?>(null)

    fun saveBaseUrl(newUrl: String): Boolean {
        return try {
            keyManager.apiBaseUrl = newUrl.trim()
            apiBaseUrlInput.value = keyManager.apiBaseUrl
            true
        } catch (e: Exception) {
            false
        }
    }

    fun toggleBridgeUpload(enabled: Boolean) {
        keyManager.bridgeUploadEnabled = enabled
        bridgeUploadEnabled.value = enabled
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
