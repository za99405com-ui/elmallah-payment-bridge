package com.elmallah.paymentbridge.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.domain.PaymentRuleStore
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.network.ApiClientProvider
import com.elmallah.paymentbridge.network.DeviceProviderConfigRequest
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.elmallah.paymentbridge.sync.BridgeForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    val keyManager: DeviceKeyManager,
    private val repository: PaymentRepository,
    private val ruleStore: PaymentRuleStore? = null,
    private val appContext: Context? = null
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
    val appTheme = MutableStateFlow(keyManager.appTheme)

    val isProvisioned = MutableStateFlow(keyManager.isProvisioned)
    val isBgRunning = BridgeForegroundService.isRunning

    val lastServerResponse = MutableStateFlow(keyManager.lastServerResponse)
    val lastServerStatusCode = MutableStateFlow(keyManager.lastServerStatusCode)

    val rules: StateFlow<List<PaymentSourceRule>> = ruleStore?.rulesFlow
        ?: MutableStateFlow<List<PaymentSourceRule>>(emptyList())

    val hasSyncedWithServer: Boolean
        get() = ruleStore?.hasSyncedWithServer ?: false

    val lastSyncTimestamp: Long
        get() = ruleStore?.lastSyncTimestamp ?: 0L

    val hasUnsavedChanges: Boolean
        get() = ruleStore?.hasUnsavedChanges() ?: false

    val healthCheckStatus = MutableStateFlow<String?>(null)
    val provisioningStatus = MutableStateFlow<String?>(null)
    val providerConfigStatus = MutableStateFlow<String?>(null)
    val purgeResultStatus = MutableStateFlow<String?>(null)
    val rulesSyncStatus = MutableStateFlow<String?>(null)

    fun saveRule(rule: PaymentSourceRule) {
        ruleStore?.saveLocalRuleDraft(rule)
        keyManager.activeRulesCount = ruleStore?.getActiveRules()?.count { it.enabled } ?: 0
    }

    fun deleteRule(ruleId: String) {
        ruleStore?.deleteRule(ruleId)
        keyManager.activeRulesCount = ruleStore?.getActiveRules()?.count { it.enabled } ?: 0
    }

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
        if (appContext != null) {
            if (enabled) {
                BridgeForegroundService.start(appContext)
            } else {
                BridgeForegroundService.stop(appContext)
            }
        }
    }

    fun setAppTheme(theme: String) {
        keyManager.appTheme = theme
        appTheme.value = theme
    }

    fun toggleBackgroundService(enabled: Boolean) {
        if (appContext != null) {
            if (enabled) {
                BridgeForegroundService.start(appContext)
            } else {
                BridgeForegroundService.stop(appContext)
            }
        }
    }

    fun toggleRule(ruleId: String, enabled: Boolean) {
        val rule = ruleStore?.getRuleById(ruleId)
        if (rule != null) {
            ruleStore.saveLocalRuleDraft(rule.copy(enabled = enabled))
            keyManager.activeRulesCount = ruleStore.getActiveRules().count { it.enabled }
        }
    }

    fun fetchRulesFromServer() {
        viewModelScope.launch {
            rulesSyncStatus.value = "جارٍ جلب قواعد مصادر الدفع من admin3..."
            try {
                val api = ApiClientProvider(keyManager).getApi()
                val response = api.fetchPaymentRules()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    ruleStore?.updateRules(body.rules)
                    keyManager.activeRulesCount = ruleStore?.getActiveRules()?.count { it.enabled } ?: 0
                    rulesSyncStatus.value = "تم بنجاح تحديث ${body.rules.size} قاعدة دفع من admin3."
                } else {
                    rulesSyncStatus.value = "تعذر جلب القواعد: HTTP ${response.code()}"
                }
            } catch (e: Exception) {
                rulesSyncStatus.value = "خطأ أثناء جلب القواعد: ${e.message}"
            }
        }
    }

    fun setVfCashEnabled(enabled: Boolean) {
        updateProviderConfig(vfCash = enabled, bankAlAhly = null)
    }

    fun setBankAlAhlyEnabled(enabled: Boolean) {
        updateProviderConfig(vfCash = null, bankAlAhly = enabled)
    }

    private fun updateProviderConfig(vfCash: Boolean?, bankAlAhly: Boolean?) {
        viewModelScope.launch {
            providerConfigStatus.value = "جارٍ حفظ إعداد وسائل الدفع في admin3..."
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
            isProvisioned.value = true
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
        lastServerResponse.value = keyManager.lastServerResponse
        lastServerStatusCode.value = keyManager.lastServerStatusCode
    }

    fun testServerHealth() {
        viewModelScope.launch {
            healthCheckStatus.value = "جارٍ فحص الاتصال بالسيرفر..."
            try {
                val api = ApiClientProvider(keyManager).getApi()
                val response = api.checkHealth()
                if (response.isSuccessful) {
                    keyManager.lastServerStatusCode = response.code()
                    keyManager.lastServerResponse = "HTTP ${response.code()} OK: ${response.body()?.status}"
                    healthCheckStatus.value = "نجح الاتصال بالسيرفر! الرد: ${response.body()?.status ?: "Healthy"}"
                } else {
                    keyManager.lastServerStatusCode = response.code()
                    healthCheckStatus.value = "فشل الاتصال: كود ${response.code()}"
                }
            } catch (e: Exception) {
                keyManager.lastServerStatusCode = -1
                healthCheckStatus.value = "خطأ في الاتصال: ${e.localizedMessage ?: e.message}"
            }
            refreshServerState()
        }
    }
}
